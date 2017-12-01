
package simulator.test.energy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
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
import simulator.network.NetworkAgent;
import simulator.network.NetworkLayer;
import simulator.test.energy.CPUModel.CONSmodel;
import simulator.test.energy.CPUModel.Mode;
import simulator.test.energy.CPUModel.PEGASUSmodel;
import simulator.test.energy.CPUModel.PERFmodel;
import simulator.test.energy.CPUModel.PESOSmodel;
import simulator.test.energy.CPUModel.QueryInfo;
import simulator.test.energy.CPUModel.Type;
import simulator.topology.NetworkTopology;
import simulator.utils.Sampler;
import simulator.utils.Sampler.Sampling;
import simulator.utils.SizeUnit;
import simulator.utils.Time;
import simulator.utils.Utils;
import simulator.utils.resources.ResourceLoader;

public class EnergyTestDIST2
{
    private static final int NODES = 5;
    private static final int CPU_CORES = 4;
    
    private static final Packet PACKET = new Packet( 20, SizeUnit.BYTE );
    
    private static boolean PESOS_CONTROLLER = true;
    private static boolean PEGASUS_CONTROLLER = false;
    
    private static final EnergyCPU CPU = new EnergyCPU();
    
    private static List<EnergyCPU> cpus = new ArrayList<>( NODES );
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
            // TODO estendere inserendo i file di tutti i nodi, ma solo se necessario
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
    
    
    
    
    private static class SwitchGenerator extends EventGenerator
    {
        public SwitchGenerator( Time duration )
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
    }
    
    private static class SwitchAgent extends Agent implements EventHandler
    {
        private PrintWriter writer;
        private List<QueryLatency> queries;
        private PEGASUS pegasus;
        
        public SwitchAgent( long id, long target, EventGenerator evGenerator ) throws IOException
        {
            super( NetworkAgent.FULL_DUPLEX, NetworkLayer.APPLICATION, id );
            addEventGenerator( evGenerator );
            addEventHandler( this );
            
            writer = new PrintWriter( "Log/Distributed_Latencies.txt", "UTF-8" );
            queries = new ArrayList<>( 1 << 10 );
            
            if (PEGASUS_CONTROLLER) {
                pegasus = new PEGASUS( cpus, target );
            }
        }
        
        @Override
        public Time handle( Event e, EventType type )
        {
            if (type == EventType.RECEIVED) {
                long queryId = e.getPacket().getContent( Global.QUERY_ID );
                if (e.getSource().getId() == 0) {
                    queries.add( new QueryLatency( queryId, e.getTime() ) );
                    //System.out.println( "RICEVUTO: " + p.getContents() );
                } else {
                    QueryLatency query = null;
                    int index;
                    for (index = 0; index < queries.size(); index++) {
                        QueryLatency ql = queries.get( index );
                        if (ql.id == queryId) {
                            query = ql;
                            break;
                        }
                    }
                    
                    if (++query.count == NODES) {
                        // Save on file and remove from list.
                        Time endTime = e.getTime();
                        Time completionTime = e.getTime().subTime( query.startTime );
                        writer.println( endTime + " " + completionTime );
                        queries.remove( index );
                        
                        if (PEGASUS_CONTROLLER) {
                            pegasus.setCompletedQuery( endTime, getId(), completionTime );
                        }
                    }
                }
            }
            
            return _node.getTcalc();
        }
        
        @Override
        public double getNodeUtilization( Time time )
        {
            double utilization = 0;
            for (Agent agent : getEventGenerator( 0 ).getDestinations()) {
                utilization += agent.getNodeUtilization( time );
            }
            return utilization;
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
        
        @Override
        protected Packet makePacket( Event e, long destination )
        {
            //System.out.println( "SERVER PACKET: " + e.getPacket().hasContent( Global.PESOS_CPU_FREQUENCY ) );
            if (e.getPacket().hasContent( Global.PESOS_TIME_BUDGET )) {
                return null;
            } else {
                return super.makePacket( e, destination );
            }
        }
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
            EnergyCPU cpu = getDevice( CPU );
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
                //System.out.println( "RECEIVED QUERY: " + p.getContent( Global.QUERY_ID ) );
                query.setEvent( e );
                query.setArrivalTime( e.getArrivalTime() );
                long coreID = cpu.selectCore( e.getArrivalTime(), query );
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
            if (e instanceof ResponseEvent) {
                EnergyCPU cpu = getDevice( CPU );
                if (type == EventType.GENERATED) {
                    QueryInfo query = cpu.getLastQuery();
                    Packet p = e.getPacket();
                    p.addContent( Global.QUERY_ID, query.getId() );
                    query.setEvent( e );
                } else { // EventType.SENT event.
                    // Set the time of the cpu as (at least) the time of the sending event.
                    cpu.setTime( e.getTime() );
                    if (!PESOS_CONTROLLER) {
                        cpu.checkQueryCompletion( e.getTime() );
                    } else {
                        for (long i = 0; i < CPU_CORES; i++) {
                            if (cpu.getCore( i ).checkQueryCompletion( e.getTime() )) {
                                controller.completedQuery( e.getTime(), getId()-2, i );
                            }
                        }
                    }
                }
            } else {
                if (e.getPacket().hasContent( Global.QUERY_ID )) {
                    // Compute the time to complete the query.
                    EnergyCPU cpu = getDevice( CPU );
                    return cpu.timeToCompute( null );
                } else {
                    return Time.ZERO;
                }
            }
            
            return _node.getTcalc();
        }
        
        @Override
        public double getNodeUtilization( Time time ) {
            return getDevice( CPU ).getUtilization( time );
        }
        
        @Override
        public void shutdown() throws IOException
        {
            EnergyCPU cpu = getDevice( CPU );
            cpu.computeIdleEnergy( getEventScheduler().getTimeDuration() );
            super.shutdown();
        }
    }
    
    
    
    
    
    
    
    public static void createDistributedIndex() throws Exception
    {
        final String dir = "Models/Monolithic/PESOS/MaxScore/";
        List<PrintWriter> writers = new ArrayList<>( NODES );
        final double BASE_RANGE = 20;
        final double RANDOM_RANGE = 10;
        
        List<Double> ranges = new ArrayList<>( NODES );
        for (int i = 0; i < NODES; i++) {
            ranges.add( (Math.random() * RANDOM_RANGE + BASE_RANGE)/100d );
        }
        
        String file = dir + "predictions.txt";
        InputStream loader = ResourceLoader.getResourceAsStream( file );
        BufferedReader reader = new BufferedReader( new InputStreamReader( loader ) );
        
        for (int i = 1; i <= NODES; i++) {
            writers.add( new PrintWriter( "Models/Distributed/Node_" + i + "/PESOS/MaxScore/predictions.txt", "UTF-8" ) );
        }
        
        String line = null;
        while ((line = reader.readLine()) != null) {
            String[] values = line.split( "\\t+" );
            long queryID    = Long.parseLong( values[0] );
            int terms       = Integer.parseInt( values[1] );
            int postings    = Integer.parseInt( values[2] );
            for (int i = 0; i < NODES; i++) {
                int difference = (int) (postings * ranges.get( i ));
                if (i % 2 == 0) {
                    difference *= -1;
                }
                postings += difference;
                writers.get( i ).println( queryID + "\t" + terms + "\t" + postings );
            }
        }
        
        for (int i = 0; i < NODES; i++) {
            writers.get( i ).close();
        }
        writers.clear();
        loader.close();
        
        // TODO qui non so se va cambiato qualcosa
        /*file = dir + "regressors_normse.txt";
        loader = ResourceLoader.getResourceAsStream( file );
        reader = new BufferedReader( new InputStreamReader( loader ) );
        
        for (int i = 1; i <= NODES; i++) {
            writers.add( new PrintWriter( "Models/Distributed/Node_" + i + "/PESOS/MaxScore/predictions.txt", "UTF-8" ) );
        }
        
        
        
        for (int i = 1; i <= NODES; i++) {
            writers.get( i ).close();
        }
        writers.clear();
        loader.close();
        
        file = dir + "regressors.txt";
        loader = ResourceLoader.getResourceAsStream( file );
        reader = new BufferedReader( new InputStreamReader( loader ) );
        
        loader.close();*/
        
        file = dir + "time_energy.txt";
        loader = ResourceLoader.getResourceAsStream( file );
        reader = new BufferedReader( new InputStreamReader( loader ) );
        
        for (int i = 1; i <= NODES; i++) {
            writers.add( new PrintWriter( "Models/Distributed/Node_" + i + "/PESOS/MaxScore/time_energy.txt", "UTF-8" ) );
        }
        
        while ((line = reader.readLine()) != null) {
            String[] values = line.split( "\\s+" );
            long queryID = Long.parseLong( values[0] );
            for (int j = 0; j < NODES; j++) {
                writers.get( j ).print( queryID + " " );
            }
            
            for (int i = 1; i < values.length; i+=2) {
                double qTime  = Double.parseDouble( values[i] );
                double energy = Double.parseDouble( values[i+1] );
                for (int j = 0; j < NODES; j++) {
                    int difference = (int) (energy * ranges.get( j ));
                    if (j % 2 == 0) {
                        difference *= -1;
                    }
                    energy += difference;
                    
                    difference = (int) (qTime * ranges.get( j ));
                    if (j % 2 == 0) {
                        difference *= -1;
                    }
                    qTime += difference;
                    
                    if (i < values.length - 2) {
                        writers.get( j ).print( qTime + " " + energy + " " );
                    } else {
                        writers.get( j ).print( qTime + " " + energy );
                    }
                }
            }
            
            for (int j = 0; j < NODES; j++) {
                writers.get( j ).println();
            }
        }
        
        for (int i = 0; i < NODES; i++) {
            writers.get( i ).close();
        }
        writers.clear();
        
        loader.close();
    }
    
    public static void main( String[] args ) throws Exception
    {
        //createDistributedIndex();
        
        Utils.VERBOSE = false;
        PESOS_CONTROLLER   = false;
        PEGASUS_CONTROLLER = false;
        
        //CPUEnergyModel model = null;
        
        //model = loadModel( Type.PESOS, Mode.TIME_CONSERVATIVE,  500 );
        //model = loadModel( Type.PESOS, Mode.TIME_CONSERVATIVE, 1000 );
        //model = loadModel( Type.PESOS, Mode.ENERGY_CONSERVATIVE,  500 );
        //model = loadModel( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 1000 );
        
        //model = loadModel( Type.PERF );
        //model = loadModel( Type.CONS );
        
        //model = loadModel( Type.PEGASUS, null,  500 );
        //model = loadModel( Type.PEGASUS, null, 1000 );
        
        //model.loadModel();
        
        //testNetwork( model );
        
        testNetwork( Type.PESOS, Mode.TIME_CONSERVATIVE,  500 );
        //testNetwork( Type.PESOS, Mode.TIME_CONSERVATIVE, 1000 );
        //testNetwork( Type.PESOS, Mode.ENERGY_CONSERVATIVE,  500 );
        //testNetwork( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 1000 );
        
        //testNetwork( Type.PERF, null, 0 );
        //testNetwork( Type.CONS, null, 0 );
        
        //testNetwork( Type.PEGASUS, null,  500 );
        //testNetwork( Type.PEGASUS, null, 1000 );
        
        /* Controller OFF
        CPU: 0, Energy: 505852.3532120938
        CPU: 1, Energy: 598058.5811507587
        CPU: 2, Energy: 493391.7082380914
        CPU: 3, Energy: 545540.8606326433
        CPU: 4, Energy: 472541.7751084604
        */
        
        /* Controller ON
        
        */
    }
    
    /*protected static CPUEnergyModel loadModel( Type type, Mode mode, long timeBudget ) throws Exception
    {
        CPUEnergyModel model = null;
        switch ( type ) {
            case PESOS  : model = new PESOSmodel( timeBudget, mode, "Models/Monolithic/PESOS/MaxScore/" );
            case PEGASUS: model = new PEGASUSmodel( timeBudget, "Models/Monolithic/PESOS/MaxScore/" );
            default     : break;
        }
        model.loadModel();
        return model;
    }
    
    protected static CPUEnergyModel loadModel( Type type ) throws Exception
    {
        CPUEnergyModel model = null;
        switch ( type ) {
            case PERF: model = new PERFmodel( "Models/Monolithic/PESOS/MaxScore/" ); break;
            case CONS: model = new CONSmodel( "Models/Monolithic/PESOS/MaxScore/" ); break;
            default  : break;
        }
        model.loadModel();
        return model;
    }*/
    
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
        
        //NetworkTopology net = new NetworkTopology( "Topology/Animation/Topology_distributed_multiCore.json" );
        NetworkTopology net = new NetworkTopology( "Topology/Topology_distributed_multiCore.json" );
        //net.setTrackingEvent( "Results/distr_multi_core.txt" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        // Create client.
        EventGenerator generator = new ClientGenerator( PACKET, PACKET );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        // Create switch.
        EventGenerator switchGen = new SwitchGenerator( Time.INFINITE );
        Agent switchAgent = new SwitchAgent( 1, timeBudget * 1000, switchGen );
        net.addAgent( switchAgent );
        client.getEventGenerator( 0 ).connect( switchAgent );
        
        // Create PESOS controller.
        if (type == Type.PESOS) {
            controller = new PESOScontroller( timeBudget * 1000, mode, cpus, NODES, CPU_CORES );
        }
        
        CPUModel model = getModel( type, mode, timeBudget, 1 );
        final String modelType = model.getModelType( true );
        Plotter plotter = new Plotter( "DISTRIBUTED MULTI_CORE - " + modelType, 800, 600 );
        final Time samplingTime = new Time( 5, TimeUnit.MINUTES );
        for (int i = 0; i < NODES; i++) {
            EnergyCPU cpu = new EnergyCPU( "Intel i7-4770K", CPU_CORES, 1, "Models/cpu_frequencies.txt" );
            //cpu.addSampler( Global.ENERGY_SAMPLING, samplingTime, Sampling.CUMULATIVE, "Log/Distributed_" + modelType + "_Energy.log" );
            //cpu.addSampler( Global.IDLE_ENERGY_SAMPLING, samplingTime, Sampling.CUMULATIVE, null );
            //cpu.addSampler( Global.TAIL_LATENCY_SAMPLING, null, null, "Log/Distributed_" + modelType + "_Node" + (i+1) + "_Tail_Latency.log" );
            cpus.add( cpu );
            
            // Add the model to the corresponding cpu.
            CPUModel p_model = loadModel( type, mode, timeBudget, i+1 );
            p_model.loadModel();
            cpu.setModel( p_model );
            
            EventGenerator sink = new MulticoreGenerator( Time.INFINITE );
            Agent agentCore = new MulticoreAgent( 2 + i, sink );
            agentCore.addDevice( cpu );
            net.addAgent( agentCore );
            
            agentCore.addSampler( Global.ENERGY_SAMPLING, new Sampler( samplingTime, "Log/Distributed_" + modelType + "_Energy.log", Sampling.CUMULATIVE ) );
            agentCore.addSampler( Global.IDLE_ENERGY_SAMPLING, new Sampler( samplingTime, null, Sampling.CUMULATIVE ) );
            
            if (type == Type.CONS) {
                EventGenerator evtGen = new ServerConsGenerator( duration );
                evtGen.connect( agentCore );
                agentCore.addEventGenerator( evtGen );
            }
            
            plotter.addPlot( agentCore.getSampledValues( Global.ENERGY_SAMPLING ), "Node " + (i+1) );
            
            switchGen.connect( agentCore );
            
            if (type == Type.PESOS) {
                controller.connect( agentCore );
            }
        }
        
        plotter.setAxisName( "Time (h)", "Energy (J)" );
        plotter.setTicks( Axis.Y, 10 );
        plotter.setTicks( Axis.X, 23, 2 );
        plotter.setRange( Axis.Y, 0, 4300 );
        plotter.setScaleX( 60d * 60d * 1000d * 1000d );
        plotter.setVisible( true );
        
        sim.start( duration, false );
        //sim.start( new Time( 53100, TimeUnit.MICROSECONDS ), false );
        sim.close();
        
        double totalEnergy = 0;
        for (int i = 0; i < NODES; i++) {
            EnergyCPU cpu = cpus.get( i );
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
        // Total energy: 4825103.293381959
        
        // PESOS TC 500ms
        // Total energy: 2599029.532406005
    }
}
