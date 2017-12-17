
package simulator.test.energy;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
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
import simulator.graphics.AnimationNetwork;
import simulator.graphics.plotter.Plotter;
import simulator.graphics.plotter.Plotter.Axis;
import simulator.graphics.plotter.Plotter.Line;
import simulator.network.NetworkAgent;
import simulator.network.NetworkLayer;
import simulator.test.energy.CPU.Core;
import simulator.test.energy.CPUModel.CONSmodel;
import simulator.test.energy.CPUModel.LOAD_SENSITIVEmodel;
import simulator.test.energy.CPUModel.MY_model;
import simulator.test.energy.CPUModel.Mode;
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

public class EnergyTestMONO
{
    private static final int CPU_CORES = 4;
    private static final Packet PACKET = new Packet( 20, SizeUnit.BYTE );
    
    private static boolean CENTRALIZED_QUEUE;
    
    
    private static class ClientGenerator extends EventGenerator
    {
        private static final String QUERY_TRACE = "Models/msn.day2.arrivals.txt";
        //private static final String QUERY_TRACE = "Models/test_arrivals.txt";
        private static final int NUM_QUERIES = 10000;
        // Random generator seed.
        //private static final int SEED = 50000;
        public static int SEED = 50000;
        //private static final Random RANDOM = new Random( SEED );
        private Random RANDOM;
        
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
            
            RANDOM = new Random( SEED );
        }
        
        @Override
        public Packet makePacket( Event e, long destination )
        {
            Packet packet = getRequestPacket();
            while (true) {
                long queryID = RANDOM.nextInt( NUM_QUERIES ) + 1;
                if (model.isQueryAvailable( queryID )) {
                    packet.addContent( Global.QUERY_ID, queryID );
                    //System.out.println( "New Query: " + queryID );
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
    
    
    
    
    private static class AnycastGenerator extends EventGenerator
    {
        public AnycastGenerator( Time duration ) {
            super( duration, Time.ZERO, PACKET, PACKET );
            setDelayedResponse( true );
            setMaximumFlyingPackets( 1 );
        }
        
        @Override
        public Packet makePacket( Event e, long destination )
        {
            if (e instanceof RequestEvent) {
                return super.makePacket( e, destination );
            } else {
                // New request from client: clone the packet.
                return e.getPacket().clone();
            }
        }
        
        @Override
        protected int selectDestination( Time time )
        {
            double minUtilization = Double.MAX_VALUE;
            int nextDestination = -1;
            for (int i = 0; i < _destinations.size(); i++) {
                Agent agent = _destinations.get( i );
                double nodeUtilization = agent.getNodeUtilization( time );
                if (nodeUtilization < minUtilization) {
                    nextDestination = i;
                    minUtilization = nodeUtilization;
                }
            }
            
            return nextDestination;
        }
    }
    
    private static class SwitchAgent extends Agent implements EventHandler
    {
        public SwitchAgent( long id, EventGenerator evGenerator ) {
            super( NetworkAgent.FULL_DUPLEX, NetworkLayer.APPLICATION, id );
            addEventGenerator( evGenerator );
            addEventHandler( this );
        }

        @Override
        public Time handle( Event e, EventType type )
        {
            if (type == EventType.RECEIVED) {
                // Event generated by a node as a response to the client.
                computeIdleEnergy( e.getSource().getId(), e.getTime() );
            }
            
            return _node.getTcalc();
        }
        
        public void computeIdleEnergy( long sourceId, Time time )
        {
            for (Agent dest : getEventGenerator( 0 ).getDestinations()) {
                if (dest.getNode().getId() != sourceId) {
                    EnergyCPU cpu = dest.getDevice( EnergyCPU.class );
                    if (cpu != null) {
                        cpu.computeIdleEnergy( time.clone() );
                    } else {
                        // Destination is a switch node.
                        SwitchAgent agent = (SwitchAgent) dest;
                        agent.computeIdleEnergy( sourceId, time );
                    }
                }
            }
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
    }
    
    private static class MulticoreAgent extends Agent implements EventHandler
    {
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
            
            if (p.hasContent( Global.CONS_CONTROL )) {
                cpu.evalCONSparameters( e.getTime() );
            } else {
                CPUModel model = (CPUModel) cpu.getModel();
                QueryInfo query = model.getQuery( p.getContent( Global.QUERY_ID ) );
                //System.out.println( "RECEIVED QUERY: " + p.getContent( Global.QUERY_ID ) );
                query.setEvent( e );
                query.setArrivalTime( e.getArrivalTime() );
                cpu.addQuery( cpu.selectCore( e.getArrivalTime(), query ), query );
            }
        }
        
        @Override
        public Time handle( Event e, EventType type )
        {
            CPU cpu = getDevice( EnergyCPU.class );
            Packet p = e.getPacket();
            if (p.hasContent( Global.CONS_CONTROL )) {
                return Time.ZERO;
            } else {
                if (e instanceof ResponseEvent) {
                    if (type == EventType.GENERATED) {
                        QueryInfo query = cpu.getLastQuery();
                        query.setEvent( e );
                    } else { // EventType.SENT event.
                        // Set the time of the cpu as (at least) the time of the sending event.
                        cpu.setTime( e.getTime() );
                        //cpu.checkQueryCompletion( e.getTime() );
                        for (long i = 0; i < CPU_CORES; i++) {
                            cpu.getCore( i ).checkQueryCompletion( e.getTime() );
                        }
                    }
                } else {
                    // Compute the time to complete the last received query.
                    return cpu.timeToCompute( null );
                }
            }
            
            return null;
        }
        
        @Override
        public double getNodeUtilization( Time time )
        {
            CPU cpu = getDevice( EnergyCPU.class );
            return cpu.getUtilization( time );
        }
    }
    
    
    
    
    
    
    
    //public static int executed;
    public static void main( String[] args ) throws Exception
    {
        Utils.VERBOSE = false;
        CENTRALIZED_QUEUE = false;
        
        if (System.getProperty( "showGUI" ) != null) {
            Global.showGUI = System.getProperty( "showGUI" ).equalsIgnoreCase( "true" );
        }
        
        CPUModel model = null;
        
        model = loadModel( Type.PESOS, Mode.TIME_CONSERVATIVE,  500 );
        //model = loadModel( Type.PESOS, Mode.TIME_CONSERVATIVE, 1000 );
        //model = loadModel( Type.PESOS, Mode.ENERGY_CONSERVATIVE,  500 );
        //model = loadModel( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 1000 );
        
        //model = loadModel( Type.LOAD_SENSITIVE, Mode.TIME_CONSERVATIVE,  500 );
        //model = loadModel( Type.LOAD_SENSITIVE, Mode.TIME_CONSERVATIVE, 1000 );
        //model = loadModel( Type.LOAD_SENSITIVE, Mode.ENERGY_CONSERVATIVE,  500 );
        //model = loadModel( Type.LOAD_SENSITIVE, Mode.ENERGY_CONSERVATIVE, 1000 );
        
        //model = loadModel( Type.MY_MODEL, Mode.TIME_CONSERVATIVE,  500 );
        //model = loadModel( Type.MY_MODEL, Mode.TIME_CONSERVATIVE, 1000 );
        
        //model = loadModel( Type.PERF );
        //model = loadModel( Type.CONS );
        
        //while (true) {
            //System.out.println( "SEED: " + ClientGenerator.SEED );
            testMultiCore( model );
            //testSingleCore( model );
            //testAnimationNetwork( model );
            //if (executed != 5) break;
            //ClientGenerator.SEED++;
        //}
        
        //System.out.println( "EXEC: " + executed + ", SEED: " + ClientGenerator.SEED );
        
        //SIMUL: 467776
    }
    
    protected static CPUModel loadModel( Type type, Mode mode, long timeBudget ) throws Exception
    {
        CPUModel model = null;
        switch ( type ) {
            case PESOS          : model = new PESOSmodel( timeBudget, mode, "Models/Monolithic/PESOS/MaxScore/" ); break;
            case LOAD_SENSITIVE : model = new LOAD_SENSITIVEmodel( timeBudget, mode, "Models/Monolithic/PESOS/MaxScore/" ); break;
            case MY_MODEL       : model = new MY_model( timeBudget, mode, "Models/Monolithic/PESOS/MaxScore/" ); break;
            default             : break;
        }
        model.loadModel();
        return model;
    }
    
    protected static CPUModel loadModel( Type type ) throws Exception
    {
        CPUModel model = null;
        switch ( type ) {
            case PERF : model = new PERFmodel( "Models/Monolithic/PESOS/MaxScore/" ); break;
            case CONS : model = new CONSmodel( "Models/Monolithic/PESOS/MaxScore/" ); break;
            default   : break;
        }
        model.loadModel();
        return model;
    }
    
    public static void testAnimationNetwork( CPUModel model, ClientModel clientModel ) throws Exception
    {
        NetworkTopology net = new NetworkTopology( "Topology/Topology_animation_test.json" );
        net.setTrackingEvent( "./Results/packets.txt" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        EventGenerator generator = new ClientGenerator( new Packet( 20, SizeUnit.MEGABYTE ),
                                                        new Packet( 20, SizeUnit.MEGABYTE ) );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        EventGenerator anyGen = new AnycastGenerator( Time.INFINITE );
        Agent switchAgent = new SwitchAgent( 1, anyGen );
        net.addAgent( switchAgent );
        
        final String modelType = model.getModelType( true );
        
        client.getEventGenerator( 0 ).connect( switchAgent );
        
        List<EnergyCPU> cpus = new ArrayList<>( CPU_CORES );
        Plotter plotter = new Plotter( model.getModelType( false ), 800, 600 );
        final Time samplingTime = new Time( 5, TimeUnit.MINUTES );
        for (int i = 0; i < CPU_CORES; i++) {
            EnergyCPU cpu = new EnergyCPU( "Intel i7-4770K", 1, 1, "Models/cpu_frequencies.txt" );
            //cpu.addSampler( Global.ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, "Log/" + modelType + "_Energy.log" );
            //cpu.addSampler( Global.IDLE_ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, null );
            //cpu.addSampler( Global.TAIL_LATENCY_SAMPLING, null, null, "Log/" + modelType + "_Tail_Latency.log" );
            cpu.setModel( model );
            cpus.add( cpu );
            
            EventGenerator sink = new MulticoreGenerator( Time.INFINITE );
            Agent node = new MulticoreAgent( 2 + i, sink );
            node.addDevice( cpu );
            net.addAgent( node );
            
            node.addSampler( Global.ENERGY_SAMPLING, new Sampler( samplingTime, "Log/" + modelType + "_Energy.log", Sampling.CUMULATIVE ) );
            node.addSampler( Global.IDLE_ENERGY_SAMPLING, new Sampler( samplingTime, null, Sampling.CUMULATIVE ) );
            node.addSampler( Global.TAIL_LATENCY_SAMPLING, new Sampler( null, "Log/" + modelType + "_Tail_Latency.log", null ) );
            
            switchAgent.getEventGenerator( 0 ).connect( node );
            
            plotter.addPlot( node.getSampledValues( Global.ENERGY_SAMPLING ), "Node " + i + " " + model.getModelType( true ) );
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
            double energy = cpu.getAgent().getResultSampled( Global.ENERGY_SAMPLING );
            totalEnergy += energy;
            totalIdleEnergy += cpu.getAgent().getResultSampled( Global.IDLE_ENERGY_SAMPLING );
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
    
    public static void testMultiCore( CPUModel model ) throws Exception
    {
        /*
                   1000Mb,0ms
        client <---------------> server
          0ms                    dynamic
        */
        
        final Time duration = new Time( 24, TimeUnit.HOURS );
        
        CPU cpu = new EnergyCPU( "Models/cpu_spec.json", getCoreClass( model.getType() ) );
        cpu.setCentralizedQueue( CENTRALIZED_QUEUE );
        cpu.setModel( model );
        
        String modelType = model.getModelType( true );
        final Time samplingTime = new Time( 5, TimeUnit.MINUTES );
        final Time meanSamplingTime = new Time( 15, TimeUnit.MINUTES );
        
        NetworkTopology net = new NetworkTopology( "Topology/Topology_mono_multiCore.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        EventGenerator generator = new ClientGenerator( PACKET, PACKET );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        EventGenerator sink = new MulticoreGenerator( Time.INFINITE );
        Agent server = new MulticoreAgent( 1, sink );
        if (model.getType() == Type.CONS) {
            EventGenerator evtGen = new ServerConsGenerator( duration );
            evtGen.connect( server );
            server.addEventGenerator( evtGen );
        }
        server.setParallelTransmission( false ).addDevice( cpu );
        net.addAgent( server );
        
        server.addSampler( Global.ENERGY_SAMPLING, new Sampler( samplingTime, "Log/" + modelType + "_Energy.log", Sampling.CUMULATIVE ) );
        server.addSampler( Global.IDLE_ENERGY_SAMPLING, new Sampler( samplingTime, null, Sampling.CUMULATIVE ) );
        server.addSampler( Global.TAIL_LATENCY_SAMPLING, new Sampler( null, "Log/" + modelType + "_Tail_Latency.log", null ) );
        if (model.getType() == Type.PERF) {
            server.addSampler( Global.MEAN_COMPLETION_TIME, new Sampler( meanSamplingTime, "Log/MeanCompletionTime.log", Sampling.AVERAGE ) );
            server.addSampler( Global.QUERY_PER_TIME_SLOT, new Sampler( meanSamplingTime, "Log/QueryPerTimeSlot.log", Sampling.CUMULATIVE ) );
        }
        
        client.getEventGenerator( 0 ).connect( server );
        
        if (Global.showGUI) {
            Plotter plotter = new Plotter( "MONOLITHIC MULTI_CORE - " + model.getModelType( false ), 800, 600 );
            plotter.addPlot( server.getSampledValues( Global.ENERGY_SAMPLING ), model.getModelType( false ) );
            plotter.setAxisName( "Time (h)", "Energy (J)" );
            plotter.setTicks( Axis.Y, 10 );
            plotter.setTicks( Axis.X, 23, 2 );
            plotter.setRange( Axis.Y, 0, 4350 );
            plotter.setScaleX( 60d * 60d * 1000d * 1000d );
            plotter.setVisible( true );
        }
        
        sim.start( duration, false );
        sim.close();
        
        System.out.println( model.getModelType( false ) + " - Total energy:      " + server.getResultSampled( Global.ENERGY_SAMPLING ) + "J" );
        System.out.println( model.getModelType( false ) + " - Total Idle energy: " + server.getResultSampled( Global.IDLE_ENERGY_SAMPLING ) + "J" );
        
        System.out.println( "QUERIES: " + cpu.getExecutedQueries() );
        
        if (Global.showGUI) {
            plotTailLatency( model.getType(), model.getMode(), model.getTimeBudget() );
        }
        
        // PARAMETERS                         0.03 0.03 0.01 (NOW: 0.01 0.06 0.01)
        //                QUERY_FILE            PARAMETER
        //
        // PESOS TC 500ms
        // TARGET: 601670
        // 
        // SIMULATOR:  510183.06404994790
        // IDLE:        48653.39354192962
        
        // PESOS TC 1000ms
        // TARGET: 443730
        //
        // SIMULATOR:  384469.73002268150
        // IDLE:        39627.18491384348
        
        // PESOS EC 500ms
        // TARGET: 531100
        //
        // SIMULATOR:  469209.54237407040
        // IDLE:        44385.01033179365
        
        // PESOS EC 1000ms
        // TARGET: 412060
        //
        // SIMULATOR:  371587.08914869634
        // IDLE:        37802.37742955076
        
        // PERF
        // TARGET: 790400
        //
        // SIMULATOR:  949242.00703909280
        // IDLE:        61336.88540427402
        
        // CONS
        // TARGET: 575000
        // 
        // SIMULATOR:  457310.72095110260
        // IDLE:        45471.40043715234
        
        // LOAD SENSITIVE TC 500ms
        // TARGET: 
        //
        // SIMULATOR:  
        // IDLE:       
        
        // LOAD SENSITIVE TC 1000ms
        // TARGET: 
        //
        // SIMULATOR:  
        // IDLE:       
        
        // Con la tecnica nuova (quella di scegliere il core con la minor frequenza predittata)
        // OLD: 
        // NEW: 
        
        // Con il JOB STEALING
        // OLD: 
        // NEW: 
        
        // Nuova strategia (MY_MODEL): prendo il massimo tra il budget predittato da PESOS e da LOAD_SENSITIVE
        // La Tail Latency e' rispettata.
        //
        // 500ms:  
        // 1000ms: 
        
        // PESOS SINGOLA CODA
        // 
    }
    
    public static void testSingleCore( CPUModel model ) throws Exception
    {
        /*
        Links have 0ms of latency and "infinite" bandwith since they act as "cpu cores".
        
                                   / node0    / node2
                                  / dynamic  / dynamic
               1000Mb,0ms        /          /
        client ---------- switch  ---------
          0ms               0ms  \          \
                                  \          \
                                   \ node1    \ node3
                                    dynamic    dynamic
        */
        
        final Time duration = new Time( 24, TimeUnit.HOURS );
        
        NetworkTopology net = new NetworkTopology( "Topology/Topology_mono_singleCore.json" );
        net.setTrackingEvent( "./Results/packets2.txt" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        EventGenerator generator = new ClientGenerator( PACKET, PACKET );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        EventGenerator anyGen = new AnycastGenerator( Time.INFINITE );
        Agent switchAgent = new SwitchAgent( 1, anyGen );
        net.addAgent( switchAgent );
        client.getEventGenerator( 0 ).connect( switchAgent );
        
        String modelType = model.getModelType( true );
        
        List<EnergyCPU> cpus = new ArrayList<>( CPU_CORES );
        Plotter plotter = new Plotter( "MONOLITHIC SINGLE_CORE - " + model.getModelType( false ), 800, 600 );
        final Time samplingTime = new Time( 5, TimeUnit.MINUTES );
        for (int i = 0; i < CPU_CORES; i++) {
            EnergyCPU cpu = new EnergyCPU( "Intel i7-4770K", 1, 1, "Models/cpu_frequencies.txt" );
            //cpu.addSampler( Global.ENERGY_SAMPLING, samplingTime, Sampling.CUMULATIVE, "Log/" + modelType + "_Energy.log" );
            //cpu.addSampler( Global.IDLE_ENERGY_SAMPLING, samplingTime, Sampling.CUMULATIVE, null );
            //cpu.addSampler( Global.TAIL_LATENCY_SAMPLING, null, null, "Log/" + modelType + "_Tail_Latency.log" );
            cpu.setModel( model.clone() );
            cpus.add( cpu );
            
            EventGenerator sink = new MulticoreGenerator( Time.INFINITE );
            Agent node = new MulticoreAgent( 2 + i, sink );
            node.addDevice( cpu );
            net.addAgent( node );
            
            node.addSampler( Global.ENERGY_SAMPLING, new Sampler( samplingTime, "Log/" + modelType + "_Energy.log", Sampling.CUMULATIVE ) );
            node.addSampler( Global.IDLE_ENERGY_SAMPLING, new Sampler( samplingTime, null, Sampling.CUMULATIVE ) );
            node.addSampler( Global.TAIL_LATENCY_SAMPLING, new Sampler( null, "Log/" + modelType + "_Tail_Latency.log", null ) );
            
            switchAgent.getEventGenerator( 0 ).connect( node );
            
            if (model.getType() == Type.CONS) {
                EventGenerator evtGen = new ServerConsGenerator( duration );
                evtGen.connect( node );
                node.addEventGenerator( evtGen );
            }
            
            plotter.addPlot( node.getSampledValues( Global.ENERGY_SAMPLING ), "Node " + i );
        }
        
        plotter.setAxisName( "Time (h)", "Energy (J)" );
        plotter.setTicks( Axis.Y, 10 );
        plotter.setTicks( Axis.X, 23, 2 );
        plotter.setRange( Axis.Y, 0, 1800 );
        plotter.setScaleX( TimeUnit.HOURS.toMicros( 1 ) );
        plotter.setVisible( true );
        
        sim.start( duration, false );
        sim.close();
        
        double totalEnergy = 0;
        double totalIdleEnergy = 0;
        int totalQueries = 0;
        for (int i = 0; i < CPU_CORES; i++) {
            EnergyCPU cpu = cpus.get( i );
            double energy = cpu.getAgent().getResultSampled( Global.ENERGY_SAMPLING );
            totalEnergy += energy;
            totalIdleEnergy += cpu.getAgent().getResultSampled( Global.IDLE_ENERGY_SAMPLING );
            System.out.println( "CPU " + i + ", Energy consumed: " + energy + "J" );
            totalQueries += cpu.getExecutedQueries();
        }
        System.out.println( model.getModelType( false ) + " - Total energy:      " + totalEnergy + "J" );
        System.out.println( model.getModelType( false ) + " - Total idle energy: " + totalIdleEnergy + "J" );
        System.out.println( "QUERIES: " + totalQueries );
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
    
    public static void plotTailLatency( Type type, Mode mode, Time timeBudget ) throws IOException
    {
        final int percentile = 95;
        final double interval = TimeUnit.MINUTES.toMicros( 5 );
        final long time_budget = (timeBudget == null) ? 1000000 : timeBudget.getTimeMicros();
        final long plotTimeBudget = time_budget / 1000;
        
        Plotter plotter = new Plotter( "DISTRIBUTED Tail Latency " + percentile + "-th Percentile", 800, 600 );
        plotter.setAxisName( "Time (h)", percentile + "th-tile response time (ms)" );
        double yRange = time_budget + 200000d;
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
        
        final String folder = "Results/Monolithic/";
        List<Pair<Double,Double>> percentiles;
        switch ( type ) {
            case PESOS :
                percentiles = Utils.getPercentiles( percentile, interval,
                                                    "Log/PESOS_" + mode + "_" + plotTimeBudget + "ms_Tail_Latency.log",
                                                    folder + "PESOS_" + mode + "_" + plotTimeBudget + "ms_Tail_Latency_" + percentile + "th_Percentile.txt" );
                plotter.addPlot( percentiles, "PESOS (" + mode + ", t=" + plotTimeBudget + "ms)" );
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
                plotter.addPlot( percentiles, "MY_Model (" + mode + ", t=" + plotTimeBudget + "ms)" );
                break;
            default : break;
        }
        
        plotter.setVisible( true );
    }
}
