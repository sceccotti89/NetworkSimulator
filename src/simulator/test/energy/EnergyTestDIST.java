
package simulator.test.energy;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import simulator.core.Agent;
import simulator.core.Simulator;
import simulator.events.Event;
import simulator.events.EventHandler;
import simulator.events.Packet;
import simulator.events.generator.CBRGenerator;
import simulator.events.generator.EventGenerator;
import simulator.events.impl.RequestEvent;
import simulator.events.impl.ResponseEvent;
import simulator.graphics.plotter.Plotter;
import simulator.graphics.plotter.Plotter.Axis;
import simulator.graphics.plotter.Plotter.Line;
import simulator.network.NetworkAgent;
import simulator.network.NetworkLayer;
import simulator.test.energy.CPU.Core;
import simulator.test.energy.CPUModel.CONSmodel;
import simulator.test.energy.CPUModel.Mode;
import simulator.test.energy.CPUModel.PEGASUSmodel;
import simulator.test.energy.CPUModel.PERFmodel;
import simulator.test.energy.CPUModel.PESOSmodel;
import simulator.test.energy.CPUModel.QueryInfo;
import simulator.test.energy.CPUModel.Type;
import simulator.test.energy.EnergyCPU.CONScore;
import simulator.test.energy.EnergyCPU.LOAD_SENSITIVEcore;
import simulator.test.energy.EnergyCPU.MY_MODELcore;
import simulator.test.energy.EnergyCPU.PEGASUScore;
import simulator.test.energy.EnergyCPU.PERFcore;
import simulator.test.energy.EnergyCPU.PESOScore;
import simulator.topology.NetworkTopology;
import simulator.utils.Pair;
import simulator.utils.Sampler;
import simulator.utils.Sampler.Sampling;
import simulator.utils.SizeUnit;
import simulator.utils.Time;
import simulator.utils.Utils;
import simulator.utils.resources.ResourceLoader;

public class EnergyTestDIST
{
    private static final int NODES = 5;
    
    private static final Packet PACKET = new Packet( 20, SizeUnit.BYTE );
    
    private static boolean PESOS_CONTROLLER   = false;
    private static boolean PEGASUS_CONTROLLER = false;
    
    private static PESOScontroller controller;
    
    
    
    
    
    private static class ClientGenerator extends EventGenerator
    {
        private static final String QUERY_TRACE = "Models/msn.day2.arrivals.txt";
        //private static final String QUERY_TRACE = "Models/test_arrivals.txt";
        private static final int NUM_QUERIES = 10000;
        // Random generator seed.
        private static final int SEED = 50000;
        private static final Random RANDOM = new Random( SEED );
        
        private BufferedReader queryReader;
        private boolean closed = false;
        
        private long lastDeparture = 0;
        private ClientModel model;
        
        
        
        public ClientGenerator( Packet reqPacket, Packet resPacket ) throws IOException
        {
            super( Time.INFINITE, Time.ZERO, reqPacket, resPacket );
            startAt( Time.ZERO );
            
            // Open the associated file.
            queryReader = new BufferedReader( new FileReader( QUERY_TRACE ) );
            model = new ClientModel( "Models/Monolithic/PESOS/MaxScore/time_energy.txt" );
            model.loadModel();
        }
        
        @Override
        public Packet makePacket( Event e, long destination )
        {
            Packet packet = getRequestPacket();
            while (true) {
                long queryID = RANDOM.nextInt( NUM_QUERIES ) + 1;
                if (model.isQueryAvailable( queryID )) {
                    packet.addContent( Global.QUERY_ID, queryID );
                    //System.out.println( "New Query: " + packet.getContent( Global.QUERY_ID ) );
                    break;
                }
            }
            
            return packet;
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
        public ClientAgent( long id, EventGenerator evGenerator )
        {
            super( NetworkAgent.FULL_DUPLEX, NetworkLayer.APPLICATION, id );
            addEventGenerator( evGenerator );
        }
    }
    
    
    
    
    private static class BrokerGenerator extends EventGenerator
    {
        public BrokerGenerator( Time duration )
        {
            super( duration, Time.ZERO, PACKET, PACKET );
            setDelayedResponse( true );
        }
        
        @Override
        public Packet makePacket( Event e, long destination )
        {
            Packet packet;
            if (e instanceof RequestEvent) {
                packet = getResponsePacket();
            } else {
                // New request from client: get a copy.
                packet = e.getPacket();
            }
            
            return packet;
        }
        
//        @Override
//        public Packet makePacket( Event e, long destination )
//        {
//            if (e instanceof RequestEvent) {
//                return super.makePacket( e, destination );
//            } else {
//                // New request from client: clone the packet.
//                return e.getPacket().clone();
//            }
//        }
    }
    
    private static class BrokerAgent extends Agent implements EventHandler
    {
        private PrintWriter writer;
        private List<QueryLatency> queries;
        private PEGASUS pegasus;
        
        private long queryDistrId = -1;
        
        public BrokerAgent( long id, long target, EventGenerator evGenerator,
                            List<CPU> nodes, String model ) throws IOException
        {
            super( NetworkAgent.FULL_DUPLEX, NetworkLayer.APPLICATION, id );
            addEventGenerator( evGenerator );
            addEventHandler( this );
            
            writer = new PrintWriter( "Log/Distributed_" + model + "_Tail_Latency.log", "UTF-8" );
            queries = new ArrayList<>( 1 << 10 );
            
            if (PEGASUS_CONTROLLER) {
                pegasus = new PEGASUS( target, nodes );
            }
        }
        
        @Override
        public Time handle( Event e, EventType type )
        {
            if (type == EventType.RECEIVED) {
                Packet p = e.getPacket();
                if (e.getSource().getId() == 0) {
                    //System.out.println( "RICEVUTO: " + p.getContents() );
                    queryDistrId = (queryDistrId + 1) % Long.MAX_VALUE;
                    p.addContent( Global.QUERY_DISTR_ID, queryDistrId );
                    queries.add( new QueryLatency( queryDistrId, e.getTime() ) );
                } else {
                    long queryDistrId = p.getContent( Global.QUERY_DISTR_ID );
                    for (int index = 0; index < queries.size(); index++) {
                        QueryLatency query = queries.get( index );
                        if (query.id == queryDistrId) {
                            Time endTime = e.getTime();
                            Time completionTime = e.getTime().subTime( query.startTime );
                            
                            if (++query.count == NODES) {
                                // Save on file and remove from list.
                                writer.println( endTime + " " + completionTime );
                                queries.remove( index );
                            }
                            
                            if (PEGASUS_CONTROLLER) {
                                pegasus.setCompletedQuery( endTime, e.getSource().getId(), completionTime );
                            }
                            break;
                        }
                    }
                }
            }
            
            return _node.getTcalc();
        }
        
        @Override
        public void shutdown() throws IOException
        {
            writer.close();
            queries.clear();
            super.shutdown();
        }
        
        private static class QueryLatency
        {
            private long id;
            private Time startTime;
            private int count;
            
            public QueryLatency( long id, Time startTime )
            {
                this.id = id;
                this.startTime = startTime;
                count = 0;
            }
        }
    }
    
    
    
    
    
    
    private static class ServerConsGenerator extends CBRGenerator
    {
        public static final Time PERIOD = new Time( CONSmodel.PERIOD, TimeUnit.MILLISECONDS );
        
        public ServerConsGenerator( Time duration ) {
            super( Time.ZERO, duration, PERIOD, PACKET, PACKET );
        }
        
        @Override
        public Packet makePacket( Event e, long destination )
        {
            Packet packet = getRequestPacket();
            packet.addContent( Global.CONS_CONTROL, "" );
            return packet;
        }
    }
    
    private static class MulticoreGenerator extends EventGenerator
    {
        public MulticoreGenerator( Time duration ) {
            super( duration, Time.ZERO, PACKET, PACKET );
        }
        
        /*@Override
        protected Packet makePacket( Event e, long destination )
        {
            //System.out.println( "SERVER PACKET: " + e.getPacket().hasContent( Global.PESOS_CPU_FREQUENCY ) );
            if (e.getPacket().hasContent( Global.PESOS_TIME_BUDGET )) {
                return null;
            } else {
                return super.makePacket( e, destination );
            }
        }*/
    }
    
    private static class MulticoreAgent extends Agent implements EventHandler
    {
        private long _versionId = 0;
        
        public MulticoreAgent( long id, EventGenerator evtGenerator )
        {
            super( NetworkAgent.FULL_DUPLEX, NetworkLayer.APPLICATION, id );
            addEventGenerator( evtGenerator );
            addEventHandler( this );
        }
        
        @Override
        public void addEventOnQueue( Event e )
        {
            Packet p = e.getPacket();
            CPU cpu = getDevice( EnergyCPU.class );
            CPUModel model = (CPUModel) cpu.getModel();
            
            //System.out.println( "RECEIVED QUERY: " + e.getPacket().getContents() );
            
            if (p.hasContent( Global.PESOS_TIME_BUDGET )) {
                //PESOSmessage message = p.getContent( Global.PESOS_TIME_BUDGET );
                //PESOScore core = (PESOScore) cpu.getCore( message.getCoreID() );
                //core.setTimeBudget( e.getTime(), message.getTimeBudget(), message.getQueryID() );
            }
            
            if (p.hasContent( Global.CONS_CONTROL )) {
                cpu.evalCONSparameters( e.getTime() );
            } else {
                QueryInfo query = model.getQuery( p.getContent( Global.QUERY_ID ) );
                query.setDistributedId( p.getContent( Global.QUERY_DISTR_ID ) );
                //System.out.println( "RECEIVED QUERY: " + p.getContent( Global.QUERY_ID ) );
                query.setEvent( e );
                query.setArrivalTime( e.getTime() );
                long coreID = cpu.selectCore( e.getTime(), query );
                cpu.addQuery( coreID, query );
                
                if (PESOS_CONTROLLER) {
                    long versionId = _versionId++ % Long.MAX_VALUE;
                    controller.addQuery( e.getTime(), getId(), coreID, query.getId(), versionId );
                }
            }
        }
        
        @Override
        public Time handle( Event e, EventType type )
        {
            CPU cpu = getDevice( EnergyCPU.class );
            if (e instanceof ResponseEvent) {
                if (type == EventType.GENERATED) {
                    QueryInfo query = cpu.getLastQuery();
                    Packet p = e.getPacket();
                    //p.addContent( Global.QUERY_ID, query.getId() );
                    p.addContent( Global.QUERY_DISTR_ID, query.getDistributedId() );
                    query.setEvent( e );
                } else { // EventType.SENT event.
                    // Set the time of the cpu as (at least) the time of the sending event.
                    cpu.setTime( e.getTime() );
                    if (!PESOS_CONTROLLER) {
                        cpu.checkQueryCompletion( e.getTime() );
                    } else {
                        for (long i = 0; i < cpu.getCPUcores(); i++) {
                            if (cpu.getCore( i ).checkQueryCompletion( e.getTime() )) {
                                controller.completedQuery( e.getTime(), getId(), i );
                            }
                        }
                    }
                }
            } else {
                if (e.getPacket().hasContent( Global.QUERY_ID )) {
                    // Compute the time to complete the query.
                    return cpu.timeToCompute( null );
                } else {
                    return Time.ZERO;
                }
            }
            
            return _node.getTcalc();
        }
        
        @Override
        public double getNodeUtilization( Time time ) {
            return getDevice( EnergyCPU.class ).getUtilization( time );
        }
        
        @Override
        public void shutdown() throws IOException
        {
            CPU cpu = getDevice( EnergyCPU.class );
            cpu.computeIdleEnergy( getEventScheduler().getTimeDuration() );
            super.shutdown();
        }
    }
    
    
    
    
    
    
    
    protected static void createDistributedIndex() throws Exception
    {
        final String dir = "Models/Monolithic/PESOS/MaxScore/";
        final double MIN_RANGE = 0;
        final double MAX_RANGE = 2;
        final Random rand = new Random();
        
        for (int i = 2; i <= NODES; i++) {
            String file = dir + "predictions.txt";
            InputStream loader = ResourceLoader.getResourceAsStream( file );
            BufferedReader reader = new BufferedReader( new InputStreamReader( loader ) );
            
            Map<Long,Double> queryRange = new HashMap<>();
            
            // Predictions.
            PrintWriter writer = new PrintWriter( "Models/Distributed/Node_" + i + "/PESOS/MaxScore/predictions.txt", "UTF-8" );
            String line = null;
            while ((line = reader.readLine()) != null) {
                double range = (rand.nextDouble() * (MAX_RANGE-MIN_RANGE) + MIN_RANGE)/100d;
                int sign = rand.nextInt( 2 ) == 0 ? 1 : -1;
                range *= sign;
                
                String[] values = line.split( "\\t+" );
                long queryID    = Long.parseLong( values[0] );
                int terms       = Integer.parseInt( values[1] );
                int postings    = Integer.parseInt( values[2] );
                int difference  = (int) (postings * range);
                if (postings == postings + difference) {
                    range = 0;
                }
                queryRange.put( queryID, range );
                postings += difference;
                writer.println( queryID + "\t" + terms + "\t" + postings );
            }
            
            writer.close();
            reader.close();
            
            // Time and energy.
            file = dir + "time_energy.txt";
            loader = ResourceLoader.getResourceAsStream( file );
            reader = new BufferedReader( new InputStreamReader( loader ) );
            
            writer = new PrintWriter( "Models/Distributed/Node_" + i + "/PESOS/MaxScore/time_energy.txt", "UTF-8" );
            
            while ((line = reader.readLine()) != null) {
                String[] values = line.split( "\\s+" );
                long queryID = (long) Double.parseDouble( values[0] );
                writer.print( queryID + " " );
                
                double range = queryRange.get( queryID );
                for (int j = 1; j < values.length; j+=2) {
                    double qTime  = Double.parseDouble( values[j] );
                    qTime += qTime * range;
                    double energy = Double.parseDouble( values[j+1] );
                    energy += energy * range;
                    
                    if (j < values.length - 2) {
                        writer.print( qTime + " " + energy + " " );
                    } else {
                        writer.print( qTime + " " + energy );
                    }
                }
                writer.println();
            }
            
            writer.close();
            reader.close();
            
            queryRange.clear();
        }
    }
    
    public static void main( String[] args ) throws Exception
    {
        //createDistributedIndex();
        
        if (System.getProperty( "showGUI" ) != null) {
            Global.showGUI = System.getProperty( "showGUI" ).equalsIgnoreCase( "true" );
        }
        
        Utils.VERBOSE = false;
        PESOS_CONTROLLER = false;
        
        testNetwork( Type.PESOS, Mode.TIME_CONSERVATIVE,  500 );
        //testNetwork( Type.PESOS, Mode.TIME_CONSERVATIVE, 1000 );
        //testNetwork( Type.PESOS, Mode.ENERGY_CONSERVATIVE,  500 );
        //testNetwork( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 1000 );
        
        //testNetwork( Type.PERF, null, 0 );
        //testNetwork( Type.CONS, null, 0 );
        
        //testNetwork( Type.PEGASUS, null,  500 );
        //testNetwork( Type.PEGASUS, null, 1000 );
        
        /* Controller OFF
        500ms => pochissimo sopra la tail latency
        CPU: 0, Energy: 510184.29294978790
        CPU: 1, Energy: 517436.83143501723
        CPU: 2, Energy: 505464.09523287165
        CPU: 3, Energy: 515423.68479431875
        CPU: 4, Energy: 505098.75745063850
        
        490ms => tail latency rispettata
        CPU: 0, Energy: 514987.66590927080
        CPU: 1, Energy: 522922.51765498940
        CPU: 2, Energy: 510727.52246600826
        CPU: 3, Energy: 520718.18692186824
        CPU: 4, Energy: 510331.94529571820
        */
        
        /* Controller ON
        500ms
        CPU: 0, Energy: 509779.17505582990
        CPU: 1, Energy: 516857.99737529930
        CPU: 2, Energy: 505372.26596979290
        CPU: 3, Energy: 515128.59815541330
        CPU: 4, Energy: 504986.23915390630
        
        490ms
        CPU: 0, Energy: 514688.90826270124
        CPU: 1, Energy: 522372.51843498480
        CPU: 2, Energy: 510648.55334194500
        CPU: 3, Energy: 520457.69276704575
        CPU: 4, Energy: 510206.59131988365
        */
        
        // PEGASUS 500ms
        // 
        
        // PEGASUS 1000ms
        // 
    }
    
    private static CPUModel getModel( Type type, Mode mode, long timeBudget, int node )
    {
        CPUModel model = null;
        switch ( type ) {
            case PESOS  : model = new PESOSmodel( timeBudget, mode, "Models/Distributed/Node_" + node + "/PESOS/MaxScore/" ); break;
            case PERF   : model = new PERFmodel( "Models/Distributed/Node_" + node + "/PESOS/MaxScore/" ); break;
            case CONS   : model = new CONSmodel( "Models/Distributed/Node_" + node + "/PESOS/MaxScore/" ); break;
            case PEGASUS: model = new PEGASUSmodel( timeBudget, "Models/Distributed/Node_" + node + "/PESOS/MaxScore/" ); break;
            default     : break;
        }
        return model;
    }
    
    private static CPUModel loadModel( Type type, Mode mode, long timeBudget, int node ) throws Exception
    {
        CPUModel model = getModel( type, mode, timeBudget, node );
        model.loadModel();
        return model;
    }
    
    public static void testNetwork( Type type, Mode mode, long timeBudget ) throws Exception
    {
        final Time duration = new Time( 24, TimeUnit.HOURS );
        PEGASUS_CONTROLLER = (type == Type.PEGASUS);
        
        //NetworkTopology net = new NetworkTopology( "Topology/Animation/Topology_distributed_multiCore.json" );
        NetworkTopology net = new NetworkTopology( "Topology/Topology_distributed_multiCore.json" );
        //net.setTrackingEvent( "Results/distr_multi_core.txt" );
        System.out.println( net.toString() );
        
        CPUModel model = getModel( type, mode, timeBudget, 1 );
        String modelType = model.getModelType( true );
        
        Simulator sim = new Simulator( net );
        
        List<CPU> cpus = new ArrayList<>( NODES );
        
        // Create client.
        EventGenerator generator = new ClientGenerator( PACKET, PACKET );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        // Create broker.
        EventGenerator brokerGen = new BrokerGenerator( duration );
        Agent broker = new BrokerAgent( 1, timeBudget * 1000, brokerGen, cpus, modelType );
        net.addAgent( broker );
        client.getEventGenerator( 0 ).connect( broker );
        
        Plotter plotter = null;
        if (Global.showGUI) {
            plotter = new Plotter( "DISTRIBUTED VERSION - " + model.getModelType( false ), 800, 600 );
        }
        Time samplingTime = new Time( 5, TimeUnit.MINUTES );
        for (int i = 0; i < NODES; i++) {
            CPU cpu = new EnergyCPU( "Models/cpu_spec.json", getCoreClass( model.getType() ) );
            cpus.add( cpu );
            
            // Add the model to the corresponding cpu.
            CPUModel p_model = loadModel( type, mode, timeBudget, i+1 );
            cpu.setModel( p_model );
            
            // Create PESOS controller.
            if (type == Type.PESOS && PESOS_CONTROLLER && controller == null) {
                controller = new PESOScontroller( timeBudget * 1000, mode, cpus, NODES, cpu.getCPUcores() );
            }
            
            EventGenerator sink = new MulticoreGenerator( duration );
            Agent node = new MulticoreAgent( 2 + i, sink );
            node.addDevice( cpu );
            net.addAgent( node );
            
            node.addSampler( Global.ENERGY_SAMPLING, new Sampler( samplingTime, "Log/Distributed_" + modelType + "_Node_" + (i+1) + "_Energy.log", Sampling.CUMULATIVE ) );
            node.addSampler( Global.IDLE_ENERGY_SAMPLING, new Sampler( samplingTime, null, Sampling.CUMULATIVE ) );
            
            if (type == Type.CONS) {
                EventGenerator evtGen = new ServerConsGenerator( duration );
                evtGen.connect( node );
                node.addEventGenerator( evtGen );
            }
            
            if (Global.showGUI) {
                plotter.addPlot( node.getSampledValues( Global.ENERGY_SAMPLING ), "Node " + (i+1) );
            }
            brokerGen.connect( node );
            
            if (type == Type.PESOS && PESOS_CONTROLLER) {
                controller.connect( node );
            }
        }
        
        if (Global.showGUI) {
            plotter.setAxisName( "Time (h)", "Energy (J)" );
            plotter.setTicks( Axis.Y, 10 );
            plotter.setTicks( Axis.X, 23, 2 );
            plotter.setRange( Axis.Y, 0, 4300 );
            plotter.setScaleX( 60d * 60d * 1000d * 1000d );
            plotter.setVisible( true );
        }
        
        sim.start( duration, false );
        //sim.start( new Time( 10000000, TimeUnit.MICROSECONDS ), false );
        sim.close();
        
        double totalEnergy = 0;
        for (int i = 0; i < NODES; i++) {
            CPU cpu = cpus.get( i );
            double energy = cpu.getAgent().getResultSampled( Global.ENERGY_SAMPLING );
            totalEnergy += energy;
            System.out.println( "CPU: " + i + ", Energy: " + energy );
        }
        System.out.println( "Total energy: " + totalEnergy );
        
        // Show the animation.
        /*AnimationNetwork an = new AnimationNetwork( 800, 600, modelType );
        an.loadSimulation( "Topology/Animation/Topology_distributed_multiCore.json", "./Results/distr_multi_core.txt" );
        an.setTargetFrameRate( 90 );
        an.setForceExit( false );
        an.start();*/
        
        // PERF
        // Total energy: 4749979.250899715
        
        // PESOS TC 500ms
        // Total energy: 2599029.532406005
        
        // PEGASUS 500ms
        // Total energy: 2894209.2631946327
        
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
            default             : return null;
        }
    }
    
    public static void plotTailLatency( Type type, Mode mode, long time_budget ) throws IOException
    {
        final int percentile = 95;
        final long timeBudget = (time_budget == 0) ? 1000000 : (time_budget * 1000);
        final double interval = TimeUnit.MINUTES.toMicros( 5 );
        
        Plotter plotter = new Plotter( "DISTRIBUTED Tail Latency " + percentile + "-th Percentile", 800, 600 );
        plotter.setAxisName( "Time (h)", percentile + "th-tile response time (ms)" );
        double yRange = timeBudget + 200000d;
        plotter.setRange( Axis.Y, 0, yRange );
        plotter.setTicks( Axis.Y, (int) (yRange / 100000) );
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
        
        final String folder = "Results/Latency/Distributed/";
        List<Pair<Double,Double>> percentiles;
        switch ( type ) {
            case PESOS :
                percentiles = Utils.getPercentiles( percentile, interval,
                                                    "Log/Distributed_PESOS_" + mode + "_" + time_budget + "ms_Tail_Latency.log",
                                                    folder + "PESOS_" + mode + "_" + time_budget + "ms_Tail_Latency_" + percentile + "th_Percentile.txt" );
                plotter.addPlot( percentiles, "PESOS (" + mode + ", t=" + time_budget + "ms)" );
                break;
            case PERF :
                percentiles = Utils.getPercentiles( percentile, interval,
                                                    "Log/Distributed_PERF_Tail_Latency.log",
                                                    folder + "PERF_Tail_Latency_" + percentile + "th_Percentile.txt" );
                plotter.addPlot( percentiles, "PERF" );
                break;
            case CONS :
                percentiles = Utils.getPercentiles( percentile, interval,
                                                    "Log/Distributed_CONS_Tail_Latency.log",
                                                    folder + "CONS_Tail_Latency_" + percentile + "th_Percentile.txt" );
                plotter.addPlot( percentiles, "CONS" );
                break;
            case PEGASUS :
                percentiles = Utils.getPercentiles( percentile, interval,
                                                    "Log/Distributed_PEGASUS_" + time_budget + "ms_Tail_Latency.log",
                                                    folder + "PEGASUS_" + time_budget + "ms_Tail_Latency_" + percentile + "th_Percentile.txt" );
                plotter.addPlot( percentiles, "PEGASUS (t=" + time_budget + "ms)" );
                break;
            default : break;
        }
        
        plotter.setVisible( true );
    }
}