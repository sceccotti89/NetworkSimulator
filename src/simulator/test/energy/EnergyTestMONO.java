
package simulator.test.energy;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import simulator.core.Agent;
import simulator.core.Simulator;
import simulator.events.Event;
import simulator.events.EventGenerator;
import simulator.events.EventHandler;
import simulator.events.Packet;
import simulator.events.impl.ResponseEvent;
import simulator.graphics.AnimationNetwork;
import simulator.graphics.plotter.Plotter;
import simulator.graphics.plotter.Plotter.Axis;
import simulator.graphics.plotter.Plotter.Line;
import simulator.graphics.plotter.Plotter.Theme;
import simulator.network.NetworkAgent;
import simulator.network.NetworkLayer;
import simulator.test.energy.CPU.Core;
import simulator.test.energy.CPUModel.CONSmodel;
import simulator.test.energy.CPUModel.LOAD_SENSITIVEmodel;
import simulator.test.energy.CPUModel.MY_model;
import simulator.test.energy.CPUModel.Mode;
import simulator.test.energy.CPUModel.PEGASUSmodel;
import simulator.test.energy.CPUModel.PERFmodel;
import simulator.test.energy.CPUModel.PESOSmodel;
import simulator.test.energy.CPUModel.QueryInfo;
import simulator.test.energy.CPUModel.TERRIERmodel;
import simulator.test.energy.CPUModel.Type;
import simulator.test.energy.EnergyCPU.CONScore;
import simulator.test.energy.EnergyCPU.LOAD_SENSITIVEcore;
import simulator.test.energy.EnergyCPU.MY_MODELcore;
import simulator.test.energy.EnergyCPU.PEGASUScore;
import simulator.test.energy.EnergyCPU.PERFcore;
import simulator.test.energy.EnergyCPU.PESOScore;
import simulator.test.energy.EnergyCPU.TERRIERcore;
import simulator.topology.NetworkTopology;
import simulator.utils.Pair;
import simulator.utils.Sampler;
import simulator.utils.Sampler.Sampling;
import simulator.utils.SizeUnit;
import simulator.utils.Time;
import simulator.utils.Utils;

public class EnergyTestMONO
{
    private static final int CPU_CORES = 4;
    
    private static boolean CENTRALIZED_QUEUE = false;
    private static boolean JOB_STEALING = false;
    
    private static CPU cpu;
    
    private static Map<String,String> testResults;
    
    
    private static class ClientGenerator extends EventGenerator
    {
        private static final String QUERY_TRACE = "Models/msn.day2.arrivals.txt";
        //private static final String QUERY_TRACE = "Models/test_arrivals.txt";
        
        private BufferedReader queryReader;
        private boolean closed = false;
        
        private long lastDeparture = 0;
        
        
        
        public ClientGenerator() throws IOException
        {
            super( Time.INFINITE, Time.ZERO, EventGenerator.BEFORE_CREATION );
            
            // Open the associated file.
            queryReader = new BufferedReader( new FileReader( QUERY_TRACE ) );
        }
        
        @Override
        public Time computeDepartureTime( Event e )
        {
            if (!closed) {
                try {
                    String queryLine = null;
                    if ((queryLine = queryReader.readLine()) != null) {
                        long time = Long.parseLong( queryLine );
                        long timeDistance = time - lastDeparture;
                        //getAgent().getSampler( "QPS" ).addSampledValue( time * 1000, time * 1000, 1 );
                        lastDeparture = time;
                        return new Time( timeDistance, TimeUnit.MILLISECONDS );
                    } else {
                        // No more lines to read.
                        queryReader.close();
                        closed = true;
                    }
                } catch ( IOException e1 ) {
                    e1.printStackTrace();
                }
            }
            
            return null;
        }
    }
    
    private static class ClientAgent extends Agent
    {
        private ClientModel model;
        
        private static final int NUM_QUERIES = 10000;
        // Random generator seed.
        private static final int SEED = 50000;
        private final Random RANDOM = new Random( SEED );
        
        
        
        public ClientAgent( long id, EventGenerator evGenerator ) throws IOException
        {
            super( NetworkAgent.FULL_DUPLEX, NetworkLayer.APPLICATION, id );
            addEventGenerator( evGenerator );
            
            //addSampler( "QPS", new Sampler( new Time( 5, TimeUnit.MINUTES ), "Results/QPS.txt", Sampling.CUMULATIVE ) );
            model = new ClientModel( "Models/Monolithic/PESOS/MaxScore/time_energy.txt" );
            model.loadModel();
        }
        
        private Packet makePacket()
        {
            Packet packet = new Packet( 20, SizeUnit.BYTE );
            while (true) {
                long queryID = RANDOM.nextInt( NUM_QUERIES ) + 1;
                if (model.isQueryAvailable( queryID )) {
                    packet.addContent( Global.QUERY_ID, queryID );
                    break;
                }
            }
            
            return packet;
        }
        
        @Override
        public void receivedMessage( Event e ) {
            // Empty body.
        }
        
        @Override
        public void notifyEvent( Event e )
        {
            // Send the packet to the server.
            Agent dest = getConnectedAgent( 1 );
            sendMessage( dest, makePacket(), true );
        }
    }
    
    
    
    
    private static class SwitchAgent extends Agent
    {
        private PrintWriter writer;
        private List<QueryLatency> queries;
        
        private Map<Long,Long> tieSelection;
        
        private long queryDistrId = -1;
        
        public SwitchAgent( long id, String model ) throws IOException
        {
            super( NetworkAgent.FULL_DUPLEX, NetworkLayer.APPLICATION, id );
            
            tieSelection = new HashMap<>();
            
            writer = new PrintWriter( "Log/" + model + "_Tail_Latency.log", "UTF-8" );
            queries = new ArrayList<>( 1 << 10 );
        }
        
        @Override
        public void connect( Agent destination )
        {
            tieSelection.put( destination.getId(), 0L );
            super.connect( destination );
        }
        
        @Override
        public void receivedMessage( Event e )
        {
            long sourceId = e.getSource().getId();
            if (sourceId == 0) {
                // From client.
                queryDistrId = (queryDistrId + 1) % Long.MAX_VALUE;
                e.getPacket().addContent( Global.QUERY_DISTR_ID, queryDistrId );
                queries.add( new QueryLatency( queryDistrId, e.getTime() ) );
                
                Agent dest = selectDestination( e.getTime() );
                sendMessage( dest, e.getPacket(), true );
            } else {
                // From server.
                long queryDistrId = e.getPacket().getContent( Global.QUERY_DISTR_ID );
                for (int index = 0; index < queries.size(); index++) {
                    QueryLatency query = queries.get( index );
                    if (query.id == queryDistrId) {
                        Time endTime = e.getTime();
                        Time completionTime = e.getTime().subTime( query.startTime );
                        
                        // Save on file and remove from list.
                        writer.println( endTime + " " + completionTime );
                        queries.remove( index );
                        
                        // Send the response to the user.
                        sendMessage( getConnectedAgent( 0 ), e.getPacket(), true );
                        break;
                    }
                }
            }
        }
        
        private Agent selectDestination( Time time )
        {
            // Select the next destination, based on the current utilization factor
            // and, if tie, on the least used.
            Agent nextDestination = null;
            double utilization = Double.MAX_VALUE;
            long tiedSelection = Long.MAX_VALUE;
            boolean tieSituation = false;
            for (Agent agent : getConnectedAgents()) {
                double coreUtilization = agent.getNodeUtilization( time );
                if (coreUtilization < utilization) {
                    nextDestination = agent;
                    utilization = coreUtilization;
                    tiedSelection = tieSelection.get( agent.getId() );
                    tieSituation = false;
                } else if (coreUtilization == utilization) {
                    if (tieSelection.get( agent.getId() ) < tiedSelection) {
                        nextDestination = agent;
                        utilization = coreUtilization;
                        tiedSelection = tieSelection.get( agent.getId() );
                    }
                    tieSituation = true;
                }
            }
            
            if (tieSituation) {
                long ties = tieSelection.get( nextDestination.getId() );
                tieSelection.put( nextDestination.getId(), ties+1 );
            }
            
            return nextDestination;
        }
        
        private void computeIdleEnergy( Time time )
        {
            for (Agent dest : getConnectedAgents()) {
                CPU cpu = dest.getDevice( EnergyCPU.class );
                cpu.computeIdleEnergy( time.clone() );
                /*if (cpu != null) {
                    cpu.computeIdleEnergy( time.clone() );
                } else {
                    // Destination is a switch node.
                    SwitchAgent agent = (SwitchAgent) dest;
                    agent.computeIdleEnergy( time );
                }*/
            }
        }
        
        /*@Override
        public double getNodeUtilization( Time time )
        {
            double utilization = 0;
            for (Agent agent : getEventGenerator( 0 ).getDestinations()) {
                utilization += agent.getNodeUtilization( time );
            }
            return utilization;
        }*/
        
        @Override
        public void shutdown() throws IOException
        {
            computeIdleEnergy( getTime() );
            writer.close();
            queries.clear();
            super.shutdown();
        }
        
        private static class QueryLatency
        {
            private long id;
            private Time startTime;
            
            public QueryLatency( long id, Time startTime )
            {
                this.id = id;
                this.startTime = startTime;
            }
        }
        
        @Override
        public void notifyEvent( Event e ) {}
    }
    
    public static class ServerConsGenerator extends EventGenerator
    {
        public static final Time PERIOD = new Time( CONSmodel.PERIOD, TimeUnit.MILLISECONDS );
        
        public ServerConsGenerator( Time duration ) {
            super( duration, PERIOD, EventGenerator.AFTER_CREATION );
        }
        
        @Override
        protected Event createEvent()
        {
            Event e = super.createEvent();
            Packet packet = new Packet( 1, SizeUnit.BIT );
            packet.addContent( Global.CONS_CONTROL, "" );
            e.setPacket( packet );
            return e;
        }
    }
    
    
    
    
    
    
    private static class MulticoreAgent extends Agent implements EventHandler
    {
        public MulticoreAgent( long id ) {
            super( NetworkAgent.FULL_DUPLEX, NetworkLayer.APPLICATION, id );
            addEventHandler( this );
        }
        
        @Override
        public void notifyEvent( Event e )
        {
            Packet p = e.getPacket();
            if (p.hasContent( Global.CONS_CONTROL )) {
                CPU cpu = getDevice( EnergyCPU.class );
                cpu.evalCONSparameters( e.getTime() );
            }
        }
        
        @Override
        public void receivedMessage( Event e )
        {
            Packet p = e.getPacket();
            CPU cpu = getDevice( EnergyCPU.class );
            
            CPUModel model = (CPUModel) cpu.getModel();
            QueryInfo query = model.getQuery( p.getContent( Global.QUERY_ID ) );
            if (p.hasContent( Global.QUERY_DISTR_ID )) {
                query.setDistributedId( p.getContent( Global.QUERY_DISTR_ID ) );
            }
            query.setEvent( e );
            query.setArrivalTime( e.getTime() );
            long coreId = cpu.selectCore( e.getTime(), query );
            cpu.addQuery( coreId, query );
            
            Time time = cpu.timeToCompute( null );
            if (time != null) {
                // Prepare the message to the client.
                sendMessage( cpu.getCore( coreId ).getTime(), getConnectedAgent( 0 ), p, false );
            }
        }
        
        @Override
        public Time handle( Event e, EventType type )
        {
            CPU cpu = getDevice( EnergyCPU.class );
            if (e instanceof ResponseEvent) {
                if (type == EventType.GENERATED) {
                    QueryInfo query = cpu.getLastQuery();
                    query.setEvent( e );
                } else { // EventType.SENT event.
                    // Set the time of the cpu as (at least) the time of the sending event.
                    cpu.setTime( e.getTime() );
                    for (Core core : cpu.getCores()) {
                        if (core.checkQueryCompletion( e.getTime() ) && !core.getQueue().isEmpty()) {
                            Packet msg = new Packet( 20, SizeUnit.BYTE );
                            msg.addContent( Global.QUERY_DISTR_ID, core.getLastQueryInQueue().getDistributedId() );
                            // Prepare the message for the current query.
                            sendMessage( core.getTime(), getConnectedAgent( 0 ), msg, false );
                        }
                    }
                }
            }
            
            return _node.getTcalc();
        }
        
        @Override
        public double getNodeUtilization( Time time )
        {
            CPU cpu = getDevice( EnergyCPU.class );
            return cpu.getUtilization( time );
        }
        
        @Override
        public void shutdown() throws IOException
        {
            CPU cpu = getDevice( EnergyCPU.class );
            cpu.computeIdleEnergy( getEventScheduler().getTimeDuration() );
            super.shutdown();
        }
    }
    
    
    
    
    
    
    
    public static void main( String[] args ) throws Exception
    {
        Utils.VERBOSE = false;
        //CENTRALIZED_QUEUE = true;
        //JOB_STEALING = true;
        
        //System.setProperty( "showGUI", "false" );
        if (System.getProperty( "showGUI" ) != null) {
            Global.showGUI = System.getProperty( "showGUI" ).equalsIgnoreCase( "true" );
        }
        
        testResults = new LinkedHashMap<>();
        
        //plotAllTailLatencies();
        
        //testPESOS( Type.PESOS, Mode.TIME_CONSERVATIVE, 500, 570013, false, new TieLeastLoaded() );
        //testPESOS( Type.PESOS, Mode.TIME_CONSERVATIVE, 500, 570013, false, new LowestPredictedFrequency() );
        //testPESOS( Type.PESOS, Mode.TIME_CONSERVATIVE, 500, 570013, false, new EarliestCompletionTime() );
        
        //testPESOS( Type.PESOS, Mode.TIME_CONSERVATIVE, 500, 570013, true, new TieLeastLoaded() );
        //testPESOS( Type.PESOS, Mode.TIME_CONSERVATIVE, 500, 570013, true, new LowestPredictedFrequency() );
        //testPESOS( Type.PESOS, Mode.TIME_CONSERVATIVE, 500, 570013, true, new EarliestCompletionTime() );
        
        //testPESOS( Type.PESOS, Mode.TIME_CONSERVATIVE, 1000, 420947, false, new TieLeastLoaded() );
        //testPESOS( Type.PESOS, Mode.TIME_CONSERVATIVE, 1000, 420947, false, new LowestPredictedFrequency() );
        //testPESOS( Type.PESOS, Mode.TIME_CONSERVATIVE, 1000, 420947, false, new EarliestCompletionTime() );
        
        //testPESOS( Type.PESOS, Mode.TIME_CONSERVATIVE, 1000, 420947, true, new TieLeastLoaded() );
        //testPESOS( Type.PESOS, Mode.TIME_CONSERVATIVE, 1000, 420947, true, new LowestPredictedFrequency() );
        //testPESOS( Type.PESOS, Mode.TIME_CONSERVATIVE, 1000, 420947, true, new EarliestCompletionTime() );
        
        //testPESOS( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 500, 491949, false, new TieLeastLoaded() );
        //testPESOS( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 500, 491949, false, new LowestPredictedFrequency() );
        //testPESOS( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 500, 491949, false, new EarliestCompletionTime() );
        
        //testPESOS( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 500, 491949, true, new TieLeastLoaded() );
        //testPESOS( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 500, 491949, true, new LowestPredictedFrequency() );
        //testPESOS( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 500, 491949, true, new EarliestCompletionTime() );
        
        //testPESOS( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 1000, 381325, false, new TieLeastLoaded() );
        //testPESOS( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 1000, 381325, false, new LowestPredictedFrequency() );
        //testPESOS( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 1000, 381325, false, new EarliestCompletionTime() );
        
        //testPESOS( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 1000, 381325, true, new TieLeastLoaded() );
        //testPESOS( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 1000, 381325, true, new LowestPredictedFrequency() );
        //testPESOS( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 1000, 381325, true, new EarliestCompletionTime() );
        
        for (Entry<String,String> entry : testResults.entrySet()) {
            Utils.LOGGER.info( entry.getKey() + ", " + entry.getValue() );
        }
        
        testMultiCore( Type.PESOS, Mode.TIME_CONSERVATIVE,  500 );
        //testMultiCore( Type.PESOS, Mode.TIME_CONSERVATIVE, 1000 );
        //testMultiCore( Type.PESOS, Mode.ENERGY_CONSERVATIVE,  500 );
        //testMultiCore( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 1000 );
        
        //plotAllTailLatencies();
        
        //testMultiCore( Type.PERF, null, 0 );
        //testMultiCore( Type.CONS, null, 0 );
        
        //testMultiCore( Type.PEGASUS, null,  500 );
        //testMultiCore( Type.PEGASUS, null, 1000 );
        
        //testMultiCore( Type.LOAD_SENSITIVE, Mode.TIME_CONSERVATIVE,  500 );
        //testMultiCore( Type.LOAD_SENSITIVE, Mode.TIME_CONSERVATIVE, 1000 );
        //testMultiCore( Type.LOAD_SENSITIVE, Mode.ENERGY_CONSERVATIVE,  500 );
        //testMultiCore( Type.LOAD_SENSITIVE, Mode.ENERGY_CONSERVATIVE, 1000 );
        
        //testMultiCore( Type.TERRIER, Mode.TIME_CONSERVATIVE,  500 );    // 794655
        //testMultiCore( Type.TERRIER, Mode.TIME_CONSERVATIVE, 1000 );    // 731356
        //testMultiCore( Type.TERRIER, Mode.ENERGY_CONSERVATIVE,  500 );  // 805553
        //testMultiCore( Type.TERRIER, Mode.ENERGY_CONSERVATIVE, 1000 );  // 708361
        
        //testSingleCore( Type.PESOS, Mode.TIME_CONSERVATIVE,  500 );
        //testSingleCore( Type.PESOS, Mode.TIME_CONSERVATIVE, 1000 );
        //testSingleCore( Type.PESOS, Mode.ENERGY_CONSERVATIVE,  500 );
        //testSingleCore( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 1000 );
        
        //testSingleCore( Type.PERF, null, 0 );
        //testSingleCore( Type.CONS, null, 0 );
        
        //testSingleCore( Type.PEGASUS, null,  500 );
        //testSingleCore( Type.PEGASUS, null, 1000 );
        
        //System.out.println( "MAX: " + EarliestCompletionTime.maxTime ); //  2.853.484
        //System.out.println( "MAX: " + LowestFrequency.maxTime );        //359.403.054
    }
    
    public static void testPESOS( Type type, Mode mode, long timeBudget, double target, boolean centralized_queue, Scheduler<Iterable<Core>, Long, QueryInfo> scheduler ) throws Exception
    {
        cpu = new EnergyCPU( "Models/cpu_spec.json", getCoreClass( type ) );
        cpu.setScheduler( scheduler );
        cpu.setCentralizedQueue( centralized_queue );
        cpu.enableJobStealing( JOB_STEALING );
        double energy = testMultiCore( type, mode, timeBudget );
        String[] subNames = scheduler.getClass().getName().split( "\\." );
        String name = subNames[subNames.length-1];
        String gain;
        if (energy <= target) {
            gain = "-" + ((1d - energy / target) * 100d) + "%";
        } else {
            gain = "+" + ((1d - energy / target) * -100d) + "%";
        }
        gain = gain.substring( 0, Math.min( gain.length(), gain.indexOf( '.' ) + 2 ) );
        String model = getModel( type, mode, timeBudget ).getModelType( false ) + "_" + centralized_queue + "_" + name;
        testResults.put( model, energy + " => " + gain );
    }
    
    private static CPUModel getModel( Type type, Mode mode, long timeBudget )
    {
        CPUModel model = null;
        final String directory = "Models/Shards/Node_5/";
        //final String directory = "Models/Monolithic/PESOS/MaxScore/";
        switch ( type ) {
            case PESOS          : model = new PESOSmodel( timeBudget, mode, directory ); break;
            case LOAD_SENSITIVE : model = new LOAD_SENSITIVEmodel( timeBudget, mode, directory ); break;
            case MY_MODEL       : model = new MY_model( timeBudget, mode, directory ); break;
            case PERF           : model = new PERFmodel( directory ); break;
            case CONS           : model = new CONSmodel( directory ); break;
            case PEGASUS        : model = new PEGASUSmodel( timeBudget, directory ); break;
            case TERRIER        : model = new TERRIERmodel( timeBudget, mode, directory ); break;
            default             : break;
        }
        return model;
    }
    
    private static CPUModel loadModel( Type type, Mode mode, long timeBudget ) throws Exception
    {
        CPUModel model = getModel( type, mode, timeBudget );
        model.loadModel();
        return model;
    }
    
    public static void testAnimationNetwork( CPUModel model, ClientModel clientModel ) throws Exception
    {
        NetworkTopology net = new NetworkTopology( "Topology/Topology_animation_test.json" );
        net.setTrackingEvent( "./Results/packets.txt" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        EventGenerator generator = new ClientGenerator();
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        final String modelType = model.getModelType( true );
        
        Agent switchAgent = new SwitchAgent( 1, modelType );
        net.addAgent( switchAgent );
        
        client.connect( switchAgent );
        
        List<EnergyCPU> cpus = new ArrayList<>( CPU_CORES );
        Plotter plotter = new Plotter( model.getModelType( false ), 800, 600 );
        final Time samplingTime = new Time( 5, TimeUnit.MINUTES );
        for (int i = 0; i < CPU_CORES; i++) {
            EnergyCPU cpu = new EnergyCPU( "Intel i7-4770K", 1, 1, "Models/cpu_frequencies.txt" );
            cpu.setModel( model );
            cpus.add( cpu );
            
            Agent node = new MulticoreAgent( 2 + i );
            node.addDevice( cpu );
            net.addAgent( node );
            
            node.addSampler( Global.ENERGY_SAMPLING, new Sampler( samplingTime, "Log/" + modelType + "_Energy.log", Sampling.CUMULATIVE ) );
            node.addSampler( Global.IDLE_ENERGY_SAMPLING, new Sampler( samplingTime, null, Sampling.CUMULATIVE ) );
            node.addSampler( Global.TAIL_LATENCY_SAMPLING, new Sampler( null, "Log/" + modelType + "_Tail_Latency.log", null ) );
            
            switchAgent.connect( node );
            
            plotter.addPlot( node.getSampler( Global.ENERGY_SAMPLING ).getValues(), "Node " + i + " " + model.getModelType( true ) );
        }
        
        plotter.setAxisName( "Time (h)", "Energy (J)" );
        plotter.setTicks( Axis.Y, 10 );
        plotter.setTicks( Axis.X, 24, 2 );
        plotter.setRange( Axis.Y, 0, 1800 );
        plotter.setScaleX( 60d * 60d * 1000d * 1000d );
        plotter.setVisible( true );
        
        sim.start( new Time( 24, TimeUnit.HOURS ), false );
        sim.close();
        
        double totalEnergy = 0;
        double totalIdleEnergy = 0;
        int totalQueries = 0;
        for (int i = 0; i < CPU_CORES; i++) {
            EnergyCPU cpu = cpus.get( i );
            double energy = cpu.getAgent().getSampler( Global.ENERGY_SAMPLING ).getTotalResult();
            totalEnergy += energy;
            totalIdleEnergy += cpu.getAgent().getSampler( Global.IDLE_ENERGY_SAMPLING ).getTotalResult();
            System.out.println( "CPU " + i + ", Energy consumed: " + energy + "J" );
            totalQueries += cpu.getExecutedQueries();
        }
        System.out.println( model.getModelType( false ) + " - Total energy:      " + totalEnergy + "J" );
        System.out.println( model.getModelType( false ) + " - Total idle energy: " + totalIdleEnergy + "J" );
        System.out.println( "QUERIES: " + totalQueries );
        
        // Show the animation.
        AnimationNetwork an = new AnimationNetwork( 800, 600, modelType );
        an.loadSimulation( "Topology/Topology_animation_test.json", "./Results/packets.txt" );
        an.setTargetFrameRate( 90 );
        an.setForceExit( false );
        an.start();
    }
    
    public static double testMultiCore( Type type, Mode mode, long timeBudget ) throws Exception
    {
        /*
                   1000Mb,0ms
        client <---------------> server
          0ms                    dynamic
        */
        
        final Time duration = new Time( 24, TimeUnit.HOURS );
        
        if (cpu == null) {
            cpu = new EnergyCPU( "Models/cpu_spec.json", getCoreClass( type ) );
            cpu.setCentralizedQueue( CENTRALIZED_QUEUE );
            cpu.enableJobStealing( JOB_STEALING );
        }
        
        CPUModel model = loadModel( type, mode, timeBudget );
        cpu.setModel( model );
        
        String modelType = model.getModelType( true );
        final Time samplingTime = new Time( 5, TimeUnit.MINUTES );
        
        NetworkTopology net = new NetworkTopology( "Topology/Topology_mono_multiCore.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        EventGenerator generator = new ClientGenerator();
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        Agent server = new MulticoreAgent( 1 );
        if (model.getType() == Type.CONS) {
            EventGenerator evtGen = new ServerConsGenerator( duration );
            server.addEventGenerator( evtGen );
        }
        
        server.setParallelTransmission( false );
        server.addDevice( cpu );
        net.addAgent( server );
        
        server.addSampler( Global.ENERGY_SAMPLING, new Sampler( samplingTime, "Log/" + modelType + "_Energy.log", Sampling.CUMULATIVE ) );
        server.addSampler( Global.IDLE_ENERGY_SAMPLING, new Sampler( samplingTime, null, Sampling.CUMULATIVE ) );
        server.addSampler( Global.TAIL_LATENCY_SAMPLING, new Sampler( null, "Log/" + modelType + "_Tail_Latency.log", null ) );
        
        final Time meanSamplingTime = new Time( 15, TimeUnit.MINUTES );
        server.addSampler( Global.QUEUE_SIZE, new Sampler( meanSamplingTime, "Results/QueueSize_" + model.getModelType( false ) + ".log", Sampling.AVERAGE ) );
        if (model.getType() == Type.PERF) {
            server.addSampler( Global.MEAN_COMPLETION_TIME, new Sampler( meanSamplingTime, "Log/MeanCompletionTime.log", Sampling.AVERAGE ) );
            server.addSampler( Global.QUERY_PER_TIME_SLOT, new Sampler( meanSamplingTime, "Log/QueryPerTimeSlot.log", Sampling.CUMULATIVE ) );
        }
        
        //client.getEventGenerator( 0 ).connect( server );
        client.connect( server );
        
        if (Global.showGUI) {
            Plotter plotter = new Plotter( "MONOLITHIC MULTI-CORE - " + model.getModelType( false ), 800, 600 );
            plotter.addPlot( server.getSampler( Global.ENERGY_SAMPLING ).getValues(), model.getModelType( false ) );
            plotter.setAxisName( "Time (h)", "Energy (J)" );
            plotter.setTicks( Axis.Y, 10 );
            plotter.setTicks( Axis.X, 23, 2 );
            plotter.setRange( Axis.Y, 0, 4350 );
            plotter.setScaleX( 60d * 60d * 1000d * 1000d );
            plotter.setVisible( true );
        }
        
        sim.start( duration, false );
        sim.close();
        
        double totalEnergy = server.getSampler( Global.ENERGY_SAMPLING ).getTotalResult();
        System.out.println( model.getModelType( false ) + " - Total energy:      " + totalEnergy + "J" );
        System.out.println( model.getModelType( false ) + " - Total Idle energy: " + server.getSampler( Global.IDLE_ENERGY_SAMPLING ).getTotalResult() + "J" );
        
        System.out.println( "QUERIES: " + cpu.getExecutedQueries() );
        
        if (Global.showGUI) {
            plotTailLatency( type, mode, timeBudget );
        }
        
        cpu = null;
        
        return totalEnergy;
    }
    
    public static void testSingleCore( Type type, Mode mode, long timeBudget ) throws Exception
    {
        /*
        Links have 0ms of latency and "infinite" bandwith (1Gb/s) since they act as "cpu cores".
        
                                   / core1    / core2
                                  / dynamic  / dynamic
               1000Mb,0ms        /          /
        client ---------- switch  ---------
          0ms               0ms  \          \
                                  \          \
                                   \ core3    \ core4
                                    dynamic    dynamic
        */
        
        final Time duration = new Time( 24, TimeUnit.HOURS );
        
        NetworkTopology net = new NetworkTopology( "Topology/Topology_mono_singleCore.json" );
        //net.setTrackingEvent( "./Results/packets2.txt" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        EventGenerator generator = new ClientGenerator();
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        CPUModel model = loadModel( type, mode, timeBudget );
        String modelType = model.getModelType( true );
        
        Agent switchAgent = new SwitchAgent( 1, modelType );
        net.addAgent( switchAgent );
        client.connect( switchAgent );
        
        List<CPU> cpus = new ArrayList<>( CPU_CORES );
        Plotter plotter = new Plotter( "MONOLITHIC SINGLE_CORE - " + model.getModelType( false ), 800, 600 );
        final Time samplingTime = new Time( 5, TimeUnit.MINUTES );
        for (int i = 0; i < CPU_CORES; i++) {
            CPU cpu = new EnergyCPU( "Models/cpu_spec.json", getCoreClass( model.getType() ) );
            cpu.setModel( model.clone() );
            cpus.add( cpu );
            
            Agent node = new MulticoreAgent( 2 + i );
            node.addDevice( cpu );
            net.addAgent( node );
            
            node.addSampler( Global.ENERGY_SAMPLING,
                             new Sampler( samplingTime, "Log/" + modelType + "_Energy.log", Sampling.CUMULATIVE ) );
            node.addSampler( Global.IDLE_ENERGY_SAMPLING,
                             new Sampler( samplingTime, null, Sampling.CUMULATIVE ) );
            
            if (model.getType() == Type.CONS) {
                EventGenerator evtGen = new ServerConsGenerator( duration );
                node.addEventGenerator( evtGen );
            }
            
            if (Global.showGUI) {
                plotter.addPlot( node.getSampler( Global.ENERGY_SAMPLING ).getValues(), "Core " + (i+1) );
            }
        }
        
        plotter.setAxisName( "Time (h)", "Energy (J)" );
        plotter.setTicks( Axis.Y, 10 );
        plotter.setTicks( Axis.X, 23, 2 );
        plotter.setRange( Axis.Y, 0, 4300 );
        plotter.setScaleX( TimeUnit.HOURS.toMicros( 1 ) );
        plotter.setVisible( true );
        
        sim.start( duration, false );
        sim.close();
        
        double totalEnergy = 0;
        double totalIdleEnergy = 0;
        int totalQueries = 0;
        for (int i = 0; i < CPU_CORES; i++) {
            CPU cpu = cpus.get( i );
            double energy = cpu.getAgent().getSampler( Global.ENERGY_SAMPLING ).getTotalResult();
            totalEnergy += energy;
            totalIdleEnergy += cpu.getAgent().getSampler( Global.IDLE_ENERGY_SAMPLING ).getTotalResult();
            System.out.println( "CPU " + (i+1) + ", Energy consumed: " + energy + "J" );
            totalQueries += cpu.getExecutedQueries();
        }
        System.out.println( model.getModelType( false ) + " - Total energy:      " + totalEnergy + "J" );
        System.out.println( model.getModelType( false ) + " - Total idle energy: " + totalIdleEnergy + "J" );
        System.out.println( "QUERIES: " + totalQueries );
        
        if (Global.showGUI) {
            plotTailLatency( type, mode, timeBudget );
        }
    }
    
    private static Class<? extends Core> getCoreClass( Type type )
    {
        switch ( type ) {
            case PESOS          : return PESOScore.class;
            case PERF           : return PERFcore.class;
            case CONS           : return CONScore.class;
            case LOAD_SENSITIVE : return LOAD_SENSITIVEcore.class;
            case MY_MODEL       : return MY_MODELcore.class;
            case PEGASUS        : return PEGASUScore.class;
            case TERRIER        : return TERRIERcore.class;
            default             : return null;
        }
    }
    
    public static void plotTailLatency( Type type, Mode mode, long timeBudget ) throws IOException
    {
        final int percentile = 95;
        final double interval = TimeUnit.MINUTES.toMicros( 5 );
        final long time_budget = (timeBudget == 0) ? 1000000 : (timeBudget * 1000);
        final long plotTimeBudget = time_budget / 1000;
        
        Plotter plotter = new Plotter( "MONOLITHIC Tail Latency " + percentile + "-th Percentile", 800, 600 );
        plotter.setAxisName( "Time (h)", percentile + "th-tile response time (ms)" );
        plotter.setScaleY( 1000d );
        
        plotter.setRange( Axis.X, 0, TimeUnit.HOURS.toMicros( 24 ) );
        plotter.setTicks( Axis.X, 23, 2 );
        plotter.setScaleX( TimeUnit.HOURS.toMicros( 1 ) );
        
        List<Pair<Double, Double>> tl_500ms  = new ArrayList<>( 2 );
        List<Pair<Double, Double>> tl_1000ms = new ArrayList<>( 2 );
        for(int i = 0; i <= 1; i++) {
            tl_500ms.add( new Pair<>( (double) (TimeUnit.HOURS.toMicros( i * 24 )), 500000d ) );
            tl_1000ms.add( new Pair<>( (double) (TimeUnit.HOURS.toMicros( i * 24 )), 1000000d ) );
        }
        plotter.addPlot( tl_500ms, Color.YELLOW, Line.DASHED, "Tail latency (" + 500 + "ms)" );
        plotter.addPlot( tl_1000ms, Color.LIGHT_GRAY, Line.DASHED, "Tail latency (" + 1000 + "ms)" );
        
        final String tau = new String( ("\u03C4").getBytes(), Charset.defaultCharset() );
        final String folder = "Results/Latency/Monolithic/";
        List<Pair<Double,Double>> percentiles = null;
        switch ( type ) {
            case PESOS :
                percentiles = Utils.getPercentiles( percentile, interval,
                                                    "Log/PESOS_" + mode + "_" + plotTimeBudget + "ms_Tail_Latency.log",
                                                    folder + "PESOS_" + mode + "_" + plotTimeBudget + "ms_Tail_Latency_" + percentile + "th_Percentile.txt" );
                plotter.addPlot( percentiles, "PESOS (" + mode + ", " + tau + "=" + plotTimeBudget + "ms)" );
                break;
            case PERF :
                percentiles = Utils.getPercentiles( percentile, interval,
                                                    "Log/PERF_Tail_Latency.log",
                                                    folder + "PERF_Tail_Latency_" + percentile + "th_Percentile.txt" );
                plotter.addPlot( percentiles, "PERF" );
                break;
            case CONS :
                percentiles = Utils.getPercentiles( percentile, interval,
                                                    "Log/CONS_Tail_Latency.log",
                                                    folder + "CONS_Tail_Latency_" + percentile + "th_Percentile.txt" );
                plotter.addPlot( percentiles, "CONS" );
                break;
            case LOAD_SENSITIVE :
                percentiles = Utils.getPercentiles( percentile, interval,
                                                    "Log/LOAD_SENSITIVE_" + mode + "_" + plotTimeBudget + "ms_Tail_Latency.log",
                                                    folder + "LOAD_SENSITIVE_" + mode + "_" + plotTimeBudget + "ms_Tail_Latency_" + percentile + "th_Percentile.txt" );
                plotter.addPlot( percentiles, "LOAD_SENSITIVE (" + mode + ", t=" + plotTimeBudget + "ms)" );
                break;
            case MY_MODEL :
                percentiles = Utils.getPercentiles( percentile, interval,
                                                    "Log/MY_Model_" + mode + "_" + plotTimeBudget + "ms_Tail_Latency.log",
                                                    folder + "MY_Model_" + mode + "_" + plotTimeBudget + "ms_Tail_Latency_" + percentile + "th_Percentile.txt" );                
                plotter.addPlot( percentiles, "MY_Model (" + mode + ", " + tau + "=" + plotTimeBudget + "ms)" );
                break;
            case TERRIER :
                percentiles = Utils.getPercentiles( percentile, interval,
                                                    "Log/TERRIER_" + mode + "_" + plotTimeBudget + "ms_Tail_Latency.log",
                                                    folder + "TERRIER_" + mode + "_" + plotTimeBudget + "ms_Tail_Latency_" + percentile + "th_Percentile.txt" );
                plotter.addPlot( percentiles, "TERRIER (" + mode + ", " + tau + "=" + plotTimeBudget + "ms)" );
                break;
            default : break;
        }
        
        double maxValue = Double.NEGATIVE_INFINITY;
        for (Pair<Double,Double> value : percentiles) {
            if (value.getSecond() > maxValue) {
                maxValue = value.getSecond();
            }
        }
        double yRange = maxValue + 50000d;
        plotter.setRange( Axis.Y, 0, yRange );
        plotter.setTicks( Axis.Y, (int) (yRange / 100000) );
        
        plotter.setVisible( true );
    }
    
    public static void plotAllTailLatencies() throws IOException
    {
        Plotter plotter = new Plotter( "MONOLITHIC Tail Latency 95-th Percentile", 800, 600 );
        plotter.setAxisName( "Time (h)", "95th-tile Response Time (ms)" );
        plotter.setScaleY( 1000d );
        
        plotter.setTheme( Theme.WHITE );
        
        plotter.setRange( Axis.X, 0, TimeUnit.HOURS.toMicros( 24 ) );
        plotter.setTicks( Axis.X, 23, 2 );
        plotter.setScaleX( TimeUnit.HOURS.toMicros( 1 ) );
        
        final String tau = new String( ("\u03C4").getBytes(), Charset.defaultCharset() );
        final String folder = "Results/Latency/Monolithic/";
        //plotter.addPlot( folder + "PERF_Tail_Latency_95th_Percentile.txt", Line.UNIFORM, "perf" );
        //plotter.addPlot( folder + "CONS_Tail_Latency_95th_Percentile.txt", Line.UNIFORM, "cons" );
        //plotter.addPlot( folder + "PESOS_TC_500ms_Tail_Latency_95th_Percentile.txt", Line.UNIFORM, "PESOS TC (" + tau + " = 500 ms)" );
        //plotter.addPlot( folder + "PESOS_EC_500ms_Tail_Latency_95th_Percentile.txt", Line.UNIFORM, "PESOS EC (" + tau + " = 500 ms)" );
        plotter.addPlot( folder + "PESOS_TC_1000ms_Tail_Latency_95th_Percentile.txt", Line.UNIFORM, "PESOS TC (" + tau + " = 1000 ms)" );
        plotter.addPlot( folder + "PESOS_EC_1000ms_Tail_Latency_95th_Percentile.txt", Line.UNIFORM, "PESOS EC (" + tau + " = 1000 ms)" );
        
        //plotter.addPlot( folder + "LOAD_SENSITIVE_TC_500ms_Tail_Latency_95th_Percentile.txt", Line.UNIFORM, "LOAD SENSITIVE TC (" + tau + " = 500 ms)" );
        //plotter.addPlot( folder + "LOAD_SENSITIVE_EC_500ms_Tail_Latency_95th_Percentile.txt", Line.UNIFORM, "LOAD SENSITIVE EC (" + tau + " = 500 ms)" );
        plotter.addPlot( folder + "LOAD_SENSITIVE_TC_1000ms_Tail_Latency_95th_Percentile.txt", Line.UNIFORM, "LOAD SENSITIVE (TC, " + tau + " = 1000 ms)" );
        plotter.addPlot( folder + "LOAD_SENSITIVE_EC_1000ms_Tail_Latency_95th_Percentile.txt", Line.UNIFORM, "LOAD SENSITIVE (EC, " + tau + " = 1000 ms)" );
        
        List<Pair<Double, Double>> tl_500ms  = new ArrayList<>( 2 );
        List<Pair<Double, Double>> tl_1000ms = new ArrayList<>( 2 );
        for(int i = 0; i <= 1; i++) {
            tl_500ms.add( new Pair<>( (double) (TimeUnit.HOURS.toMicros( i * 24 )), 500000d ) );
            tl_1000ms.add( new Pair<>( (double) (TimeUnit.HOURS.toMicros( i * 24 )), 1000000d ) );
        }
        plotter.addPlot( tl_500ms, Color.BLACK, Line.DASHED, "Tail latency (" + 500 + "ms)" );
        plotter.addPlot( tl_1000ms, Color.BLACK, Line.DASHED, "Tail latency (" + 1000 + "ms)" );
        
        double yRange = 1400000d;
        plotter.setRange( Axis.Y, 0, yRange );
        plotter.setTicks( Axis.Y, 13, 2 );
        
        plotter.setVisible( true );
    }
}
