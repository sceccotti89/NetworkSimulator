
package simulator.test.energy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import simulator.core.Agent;
import simulator.core.Device.Sampler.Sampling;
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
import simulator.test.energy.CPUEnergyModel.CONSmodel;
import simulator.test.energy.CPUEnergyModel.Mode;
import simulator.test.energy.CPUEnergyModel.PERFmodel;
import simulator.test.energy.CPUEnergyModel.PESOSmodel;
import simulator.test.energy.CPUEnergyModel.QueryInfo;
import simulator.test.energy.CPUEnergyModel.Type;
import simulator.topology.NetworkTopology;
import simulator.utils.SizeUnit;
import simulator.utils.Time;
import simulator.utils.Utils;

public class EnergyTestMONO
{
    private static final int CPU_CORES = 4;
    
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
        
        
        
        public ClientGenerator( final Packet reqPacket, final Packet resPacket ) throws IOException
        {
            super( Time.INFINITE, Time.DYNAMIC, reqPacket, resPacket, false );
            startAt( Time.ZERO );
            
            // Open the associated file.
            queryReader = new BufferedReader( new FileReader( QUERY_TRACE ) );
            model = new ClientModel();
            model.loadModel();
        }
        
        @Override
        public Packet makePacket( final Event e, final long destination )
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
        public Time computeDepartureTime( final Event e )
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
        public ClientAgent( final long id, final EventGenerator evGenerator )
        {
            super( id );
            addEventGenerator( evGenerator );
        }
    }
    
    
    
    
    private static class AnycastGenerator extends EventGenerator
    {
        public AnycastGenerator( final Time duration,
                                 final Packet reqPacket,
                                 final Packet resPacket ) {
            super( duration, Time.ZERO, reqPacket, resPacket, true );
        }
        
        @Override
        public Packet makePacket( final Event e, final long destination )
        {
            if (e instanceof RequestEvent) {
                return super.makePacket( e, destination );
            } else {
                // New request from client: clone the packet.
                return e.getPacket().clone();
            }
        }
        
        @Override
        protected int selectDestination( final Time time )
        {
            double minUtilization = Double.MAX_VALUE;
            int nextDestination = -1;
            for (int i = 0; i < _destinations.size(); i++) {
                Agent agent = _destinations.get( i );
                //EnergyCPU device = agent.getDevice( new EnergyCPU() );
                //double nodeUtilization = device.getUtilization( time );
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
        public SwitchAgent( final long id, final EventGenerator evGenerator ) {
            super( id );
            addEventGenerator( evGenerator );
            addEventHandler( this );
        }

        @Override
        public Time handle( final Event e, final EventType type )
        {
            if (type == EventType.GENERATED) {
                // Event generated by a node as a response to the client.
                computeIdleEnergy( e.getSource().getId(), e.getTime() );
            }
            
            return _node.getTcalc();
        }
        
        public void computeIdleEnergy( final long sourceId, final Time time )
        {
            for (Agent dest : _evtGenerators.get( 0 ).getDestinations()) {
                if (dest.getNode().getId() != sourceId) {
                    EnergyCPU cpu = dest.getDevice( new EnergyCPU() );
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
        public double getNodeUtilization( final Time time )
        {
            double utilization = 0;
            for (Agent agent : _evtGenerators.get( 0 ).getDestinations()) {
                utilization += agent.getNodeUtilization( time );
            }
            return utilization;
        }
    }
    
    public static class ServerConsGenerator extends CBRGenerator
    {
        public static final Time PERIOD = new Time( CONSmodel.PERIOD, TimeUnit.MILLISECONDS );
        
        public ServerConsGenerator( final Time duration, final Packet reqPacket, final Packet resPacket ) {
            super( Time.ZERO, duration, PERIOD, reqPacket, resPacket );
        }
        
        @Override
        public Packet makePacket( final Event e, final long destination )
        {
            Packet packet = getRequestPacket();
            packet.addContent( Global.CONS_CTRL_EVT, "" );
            return packet;
        }
    }
    
    
    
    
    
    
    private static class MulticoreGenerator extends EventGenerator
    {
        public MulticoreGenerator( final Time duration, final Packet reqPacket, final Packet resPacket ) {
            super( duration, Time.ZERO, reqPacket, resPacket, false );
        }
    }
    
    private static class MulticoreAgent extends Agent implements EventHandler
    {
        public MulticoreAgent( final long id, final EventGenerator evtGenerator )
        {
            super( id );
            addEventGenerator( evtGenerator );
            addEventHandler( this );
        }
        
        @Override
        public void addEventOnQueue( final Event e )
        {
            Packet p = e.getPacket();
            EnergyCPU cpu = getDevice( new EnergyCPU() );
            
            if (p.getContent( Global.CONS_CTRL_EVT ) != null) {
                cpu.evalCONSparameters( e.getTime() );
            } else {
                CPUEnergyModel model = (CPUEnergyModel) cpu.getModel();
                QueryInfo query = model.getQuery( p.getContent( Global.QUERY_ID ) );
                //System.out.println( "RECEIVED QUERY: " + p.getContent( Global.QUERY_ID ) );
                query.setEvent( e );
                query.setArrivalTime( e.getArrivalTime() );
                cpu.addQuery( cpu.selectCore( e.getArrivalTime() ), query );
            }
        }
        
        @Override
        public Time handle( final Event e, final EventType type )
        {
            EnergyCPU cpu = getDevice( new EnergyCPU() );
            Packet p = e.getPacket();
            if (p.getContent( Global.CONS_CTRL_EVT ) != null) {
                return Time.ZERO;
            } else {
                if (e instanceof ResponseEvent) {
                    if (type == EventType.GENERATED) {
                        QueryInfo query = cpu.getLastQuery();
                        query.setEvent( e );
                    } else { // EventType.SENT event.
                        // Set the time of the cpu as (at least) the time of the sending event.
                        cpu.setTime( e.getTime() );
                        cpu.checkQueryCompletion( e.getTime() );
                    }
                } else {
                    // Compute the time to complete the query.
                    return cpu.timeToCompute( null );
                }
            }
            
            return null;
        }
        
        @Override
        public double getNodeUtilization( final Time time ) {
            return getDevice( new EnergyCPU() ).getUtilization( time );
        }
    }
    
    
    
    
    
    
    
    public static void main( final String[] args ) throws Exception
    {
        Utils.VERBOSE = false;
        
        CPUEnergyModel model = null;
        
        model = loadModel( Type.PESOS, Mode.PESOS_TIME_CONSERVATIVE,  500 );
        //model = loadModel( Type.PESOS, Mode.PESOS_TIME_CONSERVATIVE, 1000 );
        //model = loadModel( Type.PESOS, Mode.PESOS_ENERGY_CONSERVATIVE,  500 );
        //model = loadModel( Type.PESOS, Mode.PESOS_ENERGY_CONSERVATIVE, 1000 );
        
        //model = loadModel( Type.PERF );
        //model = loadModel( Type.CONS );
        
        testMultiCore( model );
        //testSingleCore( model );
        //testAnimationNetwork( model );
    }
    
    protected static CPUEnergyModel loadModel( final Type type, final Mode mode, final long timeBudget ) throws Exception
    {
        // PESOS loading model.
        CPUEnergyModel model;
        if (mode == Mode.PESOS_TIME_CONSERVATIVE) {
            model = new PESOSmodel( timeBudget, mode, "Models/Monolithic/PESOS/MaxScore/",
                                    "predictions.txt", "time_energy.txt", "regressors.txt" );
        } else {
            model = new PESOSmodel( timeBudget, mode, "Models/Monolithic/PESOS/MaxScore/",
                                    "predictions.txt", "time_energy.txt", "regressors_normse.txt" );
        }
        model.loadModel();
        return model;
    }
    
    protected static CPUEnergyModel loadModel( final Type type ) throws Exception
    {
        CPUEnergyModel model = null;
        switch ( type ) {
            case PERF:  model = new PERFmodel( "Models/Monolithic/PESOS/MaxScore/",
                                               "predictions.txt", "time_energy.txt" ); break;
            case CONS:  model = new CONSmodel( "Models/Monolithic/PESOS/MaxScore/",
                                               "predictions.txt", "time_energy.txt" ); break;
            default:    break;
        }
        model.loadModel();
        return model;
    }
    
    public static void testAnimationNetwork( final CPUEnergyModel model, final ClientModel clientModel ) throws Exception
    {
        NetworkTopology net = new NetworkTopology( "Topology/Topology_animation_test.json" );
        net.setTrackingEvent( "./Results/packets.txt" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        EventGenerator generator = new ClientGenerator( new Packet( 20, SizeUnit.MEGABYTE ),
                                                        new Packet( 20, SizeUnit.MEGABYTE ) );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        EventGenerator anyGen = new AnycastGenerator( Time.INFINITE,
                                                      new Packet( 20, SizeUnit.MEGABYTE ),
                                                      new Packet( 20, SizeUnit.MEGABYTE ) );
        Agent switchAgent = new SwitchAgent( 1, anyGen );
        net.addAgent( switchAgent );
        
        String modelType = model.getModelType( true );
        
        client.getEventGenerator( 0 ).connect( switchAgent );
        
        List<EnergyCPU> cpus = new ArrayList<>( CPU_CORES );
        Plotter plotter = new Plotter( model.getModelType( false ), 800, 600 );
        for (int i = 0; i < CPU_CORES; i++) {
            EnergyCPU cpu = new EnergyCPU( "Intel i7-4770K", 1, 1, "Models/cpu_frequencies.txt" );
            cpu.addSampler( Global.ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, "Log/" + modelType + "_Energy.log" );
            cpu.addSampler( Global.IDLE_ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, null );
            cpu.addSampler( Global.TAIL_LATENCY_SAMPLING, null, null, "Log/" + modelType + "_Tail_Latency.log" );
            cpu.setModel( model );
            cpus.add( cpu );
            
            EventGenerator sink = new MulticoreGenerator( Time.INFINITE,
                                                          new Packet( 20, SizeUnit.MEGABYTE ),
                                                          new Packet( 20, SizeUnit.MEGABYTE ) );
            Agent agentCore = new MulticoreAgent( 2 + i, sink );
            agentCore.addDevice( cpu );
            net.addAgent( agentCore );
            
            switchAgent.getEventGenerator( 0 ).connect( agentCore );
            
            plotter.addPlot( cpu.getSampledValues( Global.ENERGY_SAMPLING ), null, "Node " + i + " " + model.getModelType( true ) );
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
            totalEnergy += cpu.getResultSampled( Global.ENERGY_SAMPLING );
            totalIdleEnergy += cpu.getResultSampled( Global.IDLE_ENERGY_SAMPLING );
            Utils.LOGGER.info( "CPU " + i + ", Energy consumed: " + cpu.getResultSampled( Global.ENERGY_SAMPLING ) + "J" );
            totalQueries += cpu.getExecutedQueries();
        }
        Utils.LOGGER.info( model.getModelType( false ) + " - Total energy:      " + totalEnergy + "J" );
        Utils.LOGGER.info( model.getModelType( false ) + " - Total idle energy: " + totalIdleEnergy + "J" );
        System.out.println( "QUERIES: " + totalQueries );
        
        // Show the animation.
        AnimationNetwork an = new AnimationNetwork( 800, 600, modelType );
        an.loadSimulation( "Topology/Topology_animation_test.json", "./Results/packets.txt" );
        an.setTargetFrameRate( 90 );
        an.setForceExit( false );
        an.start();
    }
    
    public static void testMultiCore( final CPUEnergyModel model ) throws Exception
    {
        /*
                   1000Mb,0ms
        client <---------------> server
          0ms                    dynamic
        */
        
        final Time duration = new Time( 24, TimeUnit.HOURS );
        
        EnergyCPU cpu = new EnergyCPU( "Intel i7-4770K", CPU_CORES, 1, "Models/cpu_frequencies.txt" );
        cpu.setModel( model );
        
        String modelType = model.getModelType( true );
        
        cpu.addSampler( Global.ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, "Log/" + modelType + "_Energy.log" );
        cpu.addSampler( Global.IDLE_ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, null );
        cpu.addSampler( Global.TAIL_LATENCY_SAMPLING, null, null, "Log/" + modelType + "_Tail_Latency.log" );
        
        NetworkTopology net = new NetworkTopology( "Topology/Topology_mono_multiCore.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        EventGenerator generator = new ClientGenerator( new Packet( 40, SizeUnit.BYTE ),
                                                        new Packet( 40, SizeUnit.BYTE ) );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        EventGenerator sink = new MulticoreGenerator( Time.INFINITE,
                                                      new Packet( 40, SizeUnit.BYTE ),
                                                      new Packet( 20, SizeUnit.BYTE ) );
        Agent server = new MulticoreAgent( 1, sink );
        if (model.getType() == Type.CONS) {
            EventGenerator evtGen = new ServerConsGenerator( duration,
                                                             new Packet( 1, SizeUnit.BYTE ),
                                                             new Packet( 1, SizeUnit.BYTE ) );
            evtGen.connect( server );
            server.addEventGenerator( evtGen );
        }
        server.setParallelTransmission( false ).addDevice( cpu );
        net.addAgent( server );
        
        client.getEventGenerator( 0 ).connect( server );
        
        Plotter plotter = new Plotter( "MONOLITHIC MULTI_CORE - " + model.getModelType( false ), 800, 600 );
        plotter.addPlot( cpu.getSampledValues( Global.ENERGY_SAMPLING ), null, model.getModelType( false ) );
        plotter.setAxisName( "Time (h)", "Energy (J)" );
        plotter.setTicks( Axis.Y, 10 );
        plotter.setTicks( Axis.X, 24, 2 );
        plotter.setRange( Axis.Y, 0, 4350 );
        plotter.setScaleX( 60d * 60d * 1000d * 1000d );
        plotter.setVisible( true );
        
        sim.start( duration, false );
        sim.close();

        Utils.LOGGER.debug( model.getModelType( false ) + " - Total energy:      " + cpu.getResultSampled( Global.ENERGY_SAMPLING ) + "J" );
        Utils.LOGGER.debug( model.getModelType( false ) + " - Total Idle energy: " + cpu.getResultSampled( Global.IDLE_ENERGY_SAMPLING ) + "J" );
        
        System.out.println( "QUERIES: " + cpu.getExecutedQueries() );
        
        // PARAMETERS                                                                    0.03 0.03 0.01
        //              COEFFICIENTS           QUERY_FILE            NORMALIZED             PARAMETER                      MATTEO               IDLE 0
        //
        // TIME CONSERVATIVE 500ms
        // TARGET: 601670
        // 
        // SIMULATOR:  754304.0207093941    594471.46526191400    477342.23502861796     541913.46662896510 (10%)     528119.30975986700
        // IDLE:       196257.8677944194     75247.89537985546     75247.89537980038      71066.81117570659            75247.89537980038    36100 Joule in meno
        
        // TIME CONSERVATIVE 1000ms
        // TARGET: 443730
        //
        // SIMULATOR:  647974.3624506982    468742.25049613975    293876.55404496676     436421.00188829670 (5%)      396748.66580365730
        // IDLE:       159577.0287529062     61183.97035269940     61183.97035269940      58061.62555988143            61183.97035273697
        
        // ENERGY CONSERVATIVE 500ms
        // TARGET: 531100
        //
        // SIMULATOR:  719986.3432093372    548442.27624212660    401489.20500958500     503171.36868913420 (5%)      481063.69885676424
        // IDLE:       178953.7990861060     68613.28364962358     68613.28364962358      64970.61726727840            68613.28364967453
        
        // ENERGY CONSERVATIVE 1000ms
        // TARGET: 412060
        //
        // SIMULATOR:  642008.7743643910    458369.63022473130    270107.72210220364     427332.62510339730 (5%)
        // IDLE:       152041.1380861678     58294.60892807464     58294.60892807464      55344.06185887506
        
        // PERF
        // TARGET: 790400
        //
        // SIMULATOR: 1145401.6003241960    992317.15024121070    940141.72685316140     862323.60355950530 (10%)     954884.43320349800
        // IDLE:       247582.8117109840     94926.56637327410     94926.56637327410      82491.18617838359            75247.89537980030    60560 Joule in meno
        
        // CONS
        // TARGET: 575000
        // 
        // SIMULATOR:  911862.87644774050   749175.27901627180    639790.48538287170     668200.88972794660 (16%)
        // IDLE:       207028.73735399803    79377.59104444605     79377.59104444605      74412.69254638738
    }
    
    public static void testSingleCore( final CPUEnergyModel model ) throws Exception
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
        
        EventGenerator generator = new ClientGenerator( new Packet( 20, SizeUnit.BYTE ),
                                                        new Packet( 20, SizeUnit.BYTE ) );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        EventGenerator anyGen = new AnycastGenerator( Time.INFINITE,
                                                      new Packet( 20, SizeUnit.BYTE ),
                                                      new Packet( 20, SizeUnit.BYTE ) );
        Agent switchAgent = new SwitchAgent( 1, anyGen );
        net.addAgent( switchAgent );
        client.getEventGenerator( 0 ).connect( switchAgent );
        
        String modelType = model.getModelType( true );
        
        List<EnergyCPU> cpus = new ArrayList<>( CPU_CORES );
        Plotter plotter = new Plotter( "MONOLITHIC SINGLE_CORE - " + model.getModelType( false ), 800, 600 );
        for (int i = 0; i < CPU_CORES; i++) {
            EnergyCPU cpu = new EnergyCPU( "Intel i7-4770K", 1, 1, "Models/cpu_frequencies.txt" );
            cpu.addSampler( Global.ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, "Log/" + modelType + "_Energy.log" );
            cpu.addSampler( Global.IDLE_ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, null );
            cpu.addSampler( Global.TAIL_LATENCY_SAMPLING, null, null, "Log/" + modelType + "_Tail_Latency.log" );
            cpu.setModel( model.clone() );
            cpus.add( cpu );
            
            EventGenerator sink = new MulticoreGenerator( Time.INFINITE,
                                                          new Packet( 20, SizeUnit.BYTE ),
                                                          new Packet( 20, SizeUnit.BYTE ) );
            Agent agentCore = new MulticoreAgent( 2 + i, sink );
            agentCore.addDevice( cpu );
            net.addAgent( agentCore );
            
            switchAgent.getEventGenerator( 0 ).connect( agentCore );
            
            if (model.getType() == Type.CONS) {
                EventGenerator evtGen = new ServerConsGenerator( duration, new Packet( 1, SizeUnit.BYTE ),
                                                                           new Packet( 1, SizeUnit.BYTE ) );
                evtGen.connect( agentCore );
                agentCore.addEventGenerator( evtGen );
            }
            
            plotter.addPlot( cpu.getSampledValues( Global.ENERGY_SAMPLING ), null, "Node " + i + " " + model.getModelType( true ) );
        }
        
        plotter.setAxisName( "Time (h)", "Energy (J)" );
        plotter.setTicks( Axis.Y, 10 );
        plotter.setTicks( Axis.X, 24, 2 );
        plotter.setRange( Axis.Y, 0, 1800 );
        plotter.setScaleX( 60d * 60d * 1000d * 1000d );
        plotter.setVisible( true );
        
        sim.start( duration, false );
        sim.close();
        
        double totalEnergy = 0;
        double totalIdleEnergy = 0;
        int totalQueries = 0;
        for (int i = 0; i < CPU_CORES; i++) {
            EnergyCPU cpu = cpus.get( i );
            totalEnergy += cpu.getResultSampled( Global.ENERGY_SAMPLING );
            totalIdleEnergy += cpu.getResultSampled( Global.IDLE_ENERGY_SAMPLING );
            Utils.LOGGER.info( "CPU " + i + ", Energy consumed: " + cpu.getResultSampled( Global.ENERGY_SAMPLING ) + "J" );
            totalQueries += cpu.getExecutedQueries();
        }
        Utils.LOGGER.info( model.getModelType( false ) + " - Total energy:      " + totalEnergy + "J" );
        Utils.LOGGER.info( model.getModelType( false ) + " - Total idle energy: " + totalIdleEnergy + "J" );
        System.out.println( "QUERIES: " + totalQueries );
    }
}