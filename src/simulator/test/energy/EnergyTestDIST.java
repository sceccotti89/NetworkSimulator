
package simulator.test.energy;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import simulator.core.Agent;
import simulator.core.Simulator;
import simulator.events.Event;
import simulator.events.EventGenerator;
import simulator.events.EventHandler;
import simulator.events.Packet;
import simulator.events.impl.ResponseEvent;
import simulator.graphics.plotter.Plotter;
import simulator.graphics.plotter.Plotter.Axis;
import simulator.graphics.plotter.Plotter.Line;
import simulator.graphics.plotter.Plotter.Theme;
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
import simulator.test.energy.PEGASUS.PEGASUSmessage;
import simulator.test.energy.PESOScontroller.PESOSmessage;
import simulator.topology.NetworkTopology;
import simulator.utils.Pair;
import simulator.utils.Sampler;
import simulator.utils.Sampler.Sampling;
import simulator.utils.SizeUnit;
import simulator.utils.Time;
import simulator.utils.Utils;

public class EnergyTestDIST
{
    private static final int NODES = 5;
    
    private static boolean PESOS_CONTROLLER   = false;
    private static boolean PEGASUS_CONTROLLER = false;
    
    
    
    
    
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
        public void notifyEvent( Event e )
        {
            // Send the packet to the server.
            Agent dest = getConnectedAgent( 1 );
            sendMessage( dest, makePacket(), true );
        }
    }
    
    
    
    
    private static class BrokerAgent extends Agent
    {
        private PrintWriter writer;
        private List<QueryLatency> queries;
        private PEGASUS pegasus;
        private PESOScontroller controller;
        
        private long queryDistrId = -1;
        
        public BrokerAgent( long id, long target, Type type, Mode mode, String model ) throws IOException
        {
            super( NetworkAgent.FULL_DUPLEX, NetworkLayer.APPLICATION, id );
            
            writer = new PrintWriter( "Log/Distributed_" + model + "_Tail_Latency.log", "UTF-8" );
            queries = new ArrayList<>( 1 << 10 );
            
            // Create the PEGASUS controller.
            if (type == Type.PEGASUS && PEGASUS_CONTROLLER) {
                pegasus = new PEGASUS( this, target );
            }
            
            // Create the PESOS controller.
            if (type == Type.PESOS && PESOS_CONTROLLER) {
                controller = new PESOScontroller( target, mode );
            }
        }
        
        @Override
        public void notifyEvent( Event e ) {
            // Empty body.
        }
        
        @Override
        public void connect( Agent destination )
        {
            super.connect( destination );
            if (controller != null) {
                controller.connect( destination );
            }
            if (pegasus != null) {
                pegasus.connect( destination );
            }
        }
        
        @Override
        public void receivedMessage( Event e )
        {
            Packet p = e.getPacket();
            if (e.getSource().getId() == 0) {
                // From client.
                //System.out.println( "RICEVUTO: " + p.getContents() );
                queryDistrId = (queryDistrId + 1) % Long.MAX_VALUE;
                p.addContent( Global.QUERY_DISTR_ID, queryDistrId );
                queries.add( new QueryLatency( queryDistrId, e.getTime() ) );
                
                for (Agent destination : getConnectedAgents()) {
                    if (destination.getId() != 0) {
                        sendMessage( destination, p, true );
                    }
                }
            } else {
                // From server.
                if (p.hasContent( Global.PESOS_CONTROLLER_ADD )) {
                    // Get the query parameters.
                    PESOSmessage message = p.getContent( Global.PESOS_CONTROLLER_ADD );
                    Agent server = e.getSource();
                    controller.addQuery( e.getTime(), server.getId(), message.getCoreID(), message.getQueryID(), message.getVersionId() );
                    controller.analyzeSystem( this, e.getTime() );
                    Packet packet = new Packet( 20, SizeUnit.BYTE );
                    packet.addContent( Global.PESOS_TIME_BUDGET, message );
                    sendMessage( server, packet, false );
                } else {
                    long queryDistrId = p.getContent( Global.QUERY_DISTR_ID );
                    for (int index = 0; index < queries.size(); index++) {
                        QueryLatency query = queries.get( index );
                        if (query.id == queryDistrId) {
                            if (++query.count == NODES) {
                                Time endTime = e.getTime();
                                Time completionTime = e.getTime().subTime( query.startTime );
                                // Save on file and remove from list.
                                writer.println( endTime + " " + completionTime );
                                queries.remove( index );
                                
                                if (PEGASUS_CONTROLLER) {
                                    pegasus.setCompletedQuery( endTime, completionTime );
                                }
                                
                                // Send-back a message to the client.
                                sendMessage( getConnectedAgent( 0 ), p, false );
                            }
                            
                            break;
                        }
                    }
                    
                    if (p.hasContent( Global.PESOS_CONTROLLER_COMPLETED )) {
                        PESOSmessage message = p.getContent( Global.PESOS_CONTROLLER_COMPLETED );
                        // Here queryID is used in place of nodeID, just to reuse the same object.
                        controller.completedQuery( e.getTime(), message.getQueryID(), message.getCoreID() );
                        controller.analyzeSystem( this, e.getTime() );
                    }
                }
            }
        }
        
        @Override
        public void shutdown() throws IOException
        {
            writer.close();
            queries.clear();
            if (controller != null) {
                //controller.shutdown();
            }
            if (pegasus != null) {
                //pegasus.shutdown();
            }
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
    
    
    
    
    
    
    private static class ServerConsGenerator extends EventGenerator
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
        private long _versionId = 0;
        
        public MulticoreAgent( long id )
        {
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
            
            if (p.hasContent( Global.PESOS_TIME_BUDGET )) {
                PESOSmessage message = p.getContent( Global.PESOS_TIME_BUDGET );
                PESOScore core = (PESOScore) cpu.getCore( message.getCoreID() );
                core.setTimeBudget( e.getTime(), message.getTimeBudget(), message.getQueryID() );
                return;
            }
            
            if (p.hasContent( Global.PEGASUS_CONTROLLER )) {
                // Set a new CPU power cap.
                PEGASUSmessage message = p.getContent( Global.PEGASUS_CONTROLLER );
                if (message.isMaximumPower()) {
                    cpu.setPower( e.getTime(), cpu.getMaxPower() );
                } else {
                    double coefficient = message.getCoefficient();
                    cpu.setPower( e.getTime(), cpu.getPower() + cpu.getPower() * coefficient );
                }
                return;
            }
            
            CPUModel model = (CPUModel) cpu.getModel();
            QueryInfo query = model.getQuery( p.getContent( Global.QUERY_ID ) );
            if (p.hasContent( Global.QUERY_DISTR_ID )) {
                query.setDistributedId( p.getContent( Global.QUERY_DISTR_ID ) );
            }
            query.setEvent( e );
            query.setArrivalTime( e.getTime() );
            long coreId = cpu.selectCore( e.getTime(), query );
            cpu.addQuery( coreId, query );
            
            if (PESOS_CONTROLLER) {
                long versionId = _versionId++ % Long.MAX_VALUE;
                
                // Send the information of the incoming query to the broker.
                PESOSmessage message = new PESOSmessage( coreId, query.getId(), 0 );
                message.setVersionId( versionId );
                Packet packet = new Packet( 20, SizeUnit.BYTE );
                packet.addContent( Global.PESOS_CONTROLLER_ADD, message );
                sendMessage( getConnectedAgent( 1 ), packet, true );
            }
            
            Time time = cpu.timeToCompute( null );
            if (time != null) {
                // Prepare the message to the broker.
                Agent broker = getConnectedAgents().get( 0 );
                sendMessage( cpu.getCore( coreId ).getTime(), broker, p, false );
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
                    for (long i = 0; i < cpu.getCPUcores(); i++) {
                        Core core = cpu.getCore( i );
                        if (core.checkQueryCompletion( e.getTime() ) && !core.getQueue().isEmpty()) {
                            Packet msg = new Packet( 20, SizeUnit.BYTE );
                            if (PESOS_CONTROLLER) {
                                // Send the information to the broker.
                                PESOSmessage message = new PESOSmessage( i, getId(), 0 );
                                msg.addContent( Global.PESOS_CONTROLLER_COMPLETED, message );
                            }
                            
                            msg.addContent( Global.QUERY_DISTR_ID, core.getLastQueryInQueue().getDistributedId() );
                            // Prepare the response message for the broker.
                            sendMessage( core.getTime(), getConnectedAgent( 1 ), msg, false );
                        }
                    }
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
    
    
    
    public static void main( String[] args ) throws Exception
    {
        //createDistributedIndex();
        
        //System.setProperty( "showGUI", "false" );
        if (System.getProperty( "showGUI" ) != null) {
            Global.showGUI = System.getProperty( "showGUI" ).equalsIgnoreCase( "true" );
        }
        
        Utils.VERBOSE = false;
        PESOS_CONTROLLER = false;
        
        // Nuovo target.
        //testNetwork( Type.PESOS, Mode.TIME_CONSERVATIVE, 425 ); // 425 => 3150561
        
        //testNetwork( Type.PESOS, Mode.TIME_CONSERVATIVE,  500 );
        //testNetwork( Type.PESOS, Mode.TIME_CONSERVATIVE, 1000 );
        //testNetwork( Type.PESOS, Mode.ENERGY_CONSERVATIVE,  500 );
        //testNetwork( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 1000 );
        
        //testNetwork( Type.PERF, null, 0 );
        //testNetwork( Type.CONS, null, 0 );
        
        testNetwork( Type.PEGASUS, null,  500 );
        //testNetwork( Type.PEGASUS, null, 1000 );
        
        //plotTailLatency( Type.PEGASUS, null,  500 );
        
        //plotTailLatency( Type.PESOS, Mode.TIME_CONSERVATIVE,  500 );
        //testNetwork( Type.PESOS, Mode.TIME_CONSERVATIVE, 1000 );
        //plotTailLatency( Type.PESOS, Mode.ENERGY_CONSERVATIVE,  500 );
        //plotTailLatency( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 1000 );
        //plotTailLatency( Type.PERF, null, 0 );
        
        //plotAllTailLatencies();
    }
    
    private static CPUModel getModel( Type type, Mode mode, long timeBudget, int node )
    {
        String directory = "Models/Shards/";
        CPUModel model = null;
        switch ( type ) {
            case PESOS  : model = new PESOSmodel( timeBudget, mode, directory + "Node_" + node + "/" ); break;
            case PERF   : model = new PERFmodel( directory + "Node_" + node + "/" ); break;
            case CONS   : model = new CONSmodel( directory + "Node_" + node + "/" ); break;
            case PEGASUS: model = new PEGASUSmodel( timeBudget, directory + "Node_" + node + "/" ); break;
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
        
        //plotTailLatency( type, mode, timeBudget );
        
        //NetworkTopology net = new NetworkTopology( "Topology/Animation/Topology_distributed_multiCore.json" );
        NetworkTopology net = new NetworkTopology( "Topology/Topology_distributed_multiCore.json" );
        //net.setTrackingEvent( "Results/distr_multi_core.txt" );
        //System.out.println( net.toString() );
        
        CPUModel model = getModel( type, mode, timeBudget, 1 );
        String modelType = model.getModelType( true );
        
        Simulator sim = new Simulator( net );
        
        List<CPU> cpus = new ArrayList<>( NODES );
        
        // Create client.
        EventGenerator generator = new ClientGenerator();
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        // Create broker.
        Agent broker = new BrokerAgent( 1, timeBudget * 1000, type, mode, modelType );
        net.addAgent( broker );
        client.connect( broker );
        
        Plotter plotter = null;
        if (Global.showGUI) {
            plotter = new Plotter( "DISTRIBUTED VERSION - " + model.getModelType( false ), 800, 600 );
        }
        
        final Time samplingTime = new Time( 5, TimeUnit.MINUTES );
        for (int i = 0; i < NODES; i++) {
            CPU cpu = new EnergyCPU( "Models/cpu_spec.json", getCoreClass( type ) );
            cpus.add( cpu );
            
            // Add the model to the corresponding cpu.
            CPUModel p_model = loadModel( type, mode, timeBudget, i+1 );
            cpu.setModel( p_model );
            
            // Create PESOS controller.
            //if (type == Type.PESOS && PESOS_CONTROLLER && controller == null) {
            //    controller = new PESOScontroller( timeBudget * 1000, mode, cpus, NODES, cpu.getCPUcores() );
            //}
            
            Agent node = new MulticoreAgent( 2 + i );
            node.addDevice( cpu );
            net.addAgent( node );
            
            node.addSampler( Global.ENERGY_SAMPLING, new Sampler( samplingTime, "Log/Distributed_" + modelType + "_Node_" + (i+1) + "_Energy.log", Sampling.CUMULATIVE ) );
            node.addSampler( Global.IDLE_ENERGY_SAMPLING, new Sampler( samplingTime, null, Sampling.CUMULATIVE ) );
            
            if (type == Type.CONS) {
                EventGenerator evtGen = new ServerConsGenerator( duration );
                node.addEventGenerator( evtGen );
            }
            
            if (Global.showGUI) {
                plotter.addPlot( node.getSampler( Global.ENERGY_SAMPLING ).getValues(), "Node " + (i+1) );
            }
            broker.connect( node );
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
            double energy = cpu.getAgent().getSampler( Global.ENERGY_SAMPLING ).getTotalResult();
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
        final double interval = TimeUnit.MINUTES.toMicros( 5 );
        
        Plotter plotter = new Plotter( "DISTRIBUTED Tail Latency " + percentile + "-th Percentile", 800, 600 );
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
        final String folder = "Results/Latency/Distributed/";
        List<Pair<Double,Double>> percentiles = null;
        switch ( type ) {
            case PESOS :
                percentiles = Utils.getPercentiles( percentile, interval,
                                                    "Log/Distributed_PESOS_" + mode + "_" + time_budget + "ms_Tail_Latency.log",
                                                    folder + "PESOS_" + mode + "_" + time_budget + "ms_Tail_Latency_" + percentile + "th_Percentile.txt" );
                plotter.addPlot( percentiles, "PESOS (" + mode + ", " + tau + "=" + time_budget + "ms)" );
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
                plotter.addPlot( percentiles, "PEGASUS (" + tau + "=" + time_budget + "ms)" );
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
        Plotter plotter = new Plotter( "DISTRIBUTED Tail Latency 95-th Percentile", 800, 600 );
        plotter.setAxisName( "Time (h)", "95th-tile response time (ms)" );
        plotter.setScaleY( 1000d );
        
        plotter.setTheme( Theme.WHITE );
        
        plotter.setRange( Axis.X, 0, TimeUnit.HOURS.toMicros( 24 ) );
        plotter.setTicks( Axis.X, 23, 2 );
        plotter.setScaleX( TimeUnit.HOURS.toMicros( 1 ) );
        
        final String tau = new String( ("\u03C4").getBytes(), Charset.defaultCharset() );
        final String folder = "Results/Latency/Distributed/";
        plotter.addPlot( folder + "PESOS_TC_500ms_Tail_Latency_95th_Percentile.txt", Line.UNIFORM, "PESOS TC (" + tau + " = 500 ms)" );
        plotter.addPlot( folder + "PESOS_EC_500ms_Tail_Latency_95th_Percentile.txt", Line.UNIFORM, "PESOS EC (" + tau + " = 500 ms)" );
        plotter.addPlot( folder + "PESOS_TC_1000ms_Tail_Latency_95th_Percentile.txt", Line.UNIFORM, "PESOS TC (" + tau + " = 1000 ms)" );
        plotter.addPlot( folder + "PESOS_EC_1000ms_Tail_Latency_95th_Percentile.txt", Line.UNIFORM, "PESOS EC (" + tau + " = 1000 ms)" );
        plotter.addPlot( folder + "PEGASUS_500ms_Tail_Latency_95th_Percentile.txt", Line.UNIFORM, "PEGASUS (" + tau + " = 500 ms)" );
        plotter.addPlot( folder + "PEGASUS_1000ms_Tail_Latency_95th_Percentile.txt", Line.UNIFORM, "PEGASUS (" + tau + " = 1000 ms)" );
        
        List<Pair<Double, Double>> tl_500ms  = new ArrayList<>( 2 );
        List<Pair<Double, Double>> tl_1000ms = new ArrayList<>( 2 );
        for(int i = 0; i <= 1; i++) {
            tl_500ms.add( new Pair<>( (double) (TimeUnit.HOURS.toMicros( i * 24 )), 500000d ) );
            tl_1000ms.add( new Pair<>( (double) (TimeUnit.HOURS.toMicros( i * 24 )), 1000000d ) );
        }
        plotter.addPlot( tl_500ms, Color.BLACK, Line.DASHED, "Tail latency (" + 500 + "ms)" );
        plotter.addPlot( tl_1000ms, Color.BLACK, Line.DASHED, "Tail latency (" + 1000 + "ms)" );
        
        double yRange = 1600000d;
        plotter.setRange( Axis.Y, 0, yRange );
        plotter.setTicks( Axis.Y, 15, 2 );
        
        plotter.setVisible( true );
    }
}