
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
        private static final String QUERY_TRACE = "Models/PESOS/msn.day2.arrivals.txt";
        //private static final String QUERY_TRACE = "Models/test_arrivals.txt";
        private static final int NUM_QUERIES = 10000;
        // Random generator seed.
        private static final int SEED = 50000;
        private static final Random RANDOM = new Random( SEED );
        
        private BufferedReader queryReader;
        private boolean closed = false;
        
        private long lastDeparture = 0;
        private CPUEnergyModel model;
        
        
        
        public ClientGenerator( final Packet reqPacket, final Packet resPacket, final CPUEnergyModel model )
                throws IOException
        {
            super( Time.INFINITE, Time.DYNAMIC, Utils.INFINITE,
                   reqPacket, resPacket, true, false, false );
            
            // Open the associated file.
            queryReader = new BufferedReader( new FileReader( QUERY_TRACE ) );
            this.model = model;
        }
        
        @Override
        public Packet makePacket( final Event e )
        {
            Packet packet = _reqPacket.clone();
            while (true) {
                long queryID = RANDOM.nextInt( NUM_QUERIES ) + 1;
                QueryInfo query = model.getQuery( queryID );
                if (query.isAvailable()) {
                    packet.addContent( Global.QUERY_ID, queryID );
                    //System.out.println( "New Query: " + packet.getContent( Global.QUERY_ID ) );
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
            super( duration, Time.ZERO, Utils.INFINITE, reqPacket, resPacket, false, true, false );
        }
        
        @Override
        public Time computeDepartureTime( final Event e ) {
            // Empty method.
            return null;
        }
        
        @Override
        public Packet makePacket( final Event e )
        {
            if (e instanceof RequestEvent) {
                return super.makePacket( e );
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
                //EnergyCPU2 device = agent.getDevice( new EnergyCPU2() );
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
            for (Agent dest : _evGenerators.get( 0 ).getDestinations()) {
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
            for (Agent agent : _evGenerators.get( 0 ).getDestinations()) {
                utilization += agent.getNodeUtilization( time );
            }
            return utilization;
        }
    }
    
    protected static class ServerConsGenerator extends CBRGenerator
    {
        public static final Time interval = new Time( 1, TimeUnit.SECONDS );
        
        public ServerConsGenerator( final Time duration, final Packet reqPacket, final Packet resPacket ) {
            super( duration, interval, reqPacket, resPacket );
        }
        
        @Override
        public Packet makePacket( final Event e )
        {
            Packet packet = _reqPacket.clone();
            packet.addContent( Global.CONS_CTRL_EVT, "" );
            return packet;
        }
    }
    
    private static class SinkGenerator extends EventGenerator
    {
        public SinkGenerator( final Time duration,
                              final Packet reqPacket,
                              final Packet resPacket )
        {
            super( duration, Time.ZERO, 1L, reqPacket, resPacket, false, false, true );
        }
        
        @Override
        public Time computeDepartureTime( final Event e ) {
            // Empy method.
            return null;
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
                cpu.evalCONSfrequency();
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
            Packet p = e.getPacket();
            if (p.getContent( Global.CONS_CTRL_EVT ) != null) {
                return Time.ZERO;
            } else {
                EnergyCPU cpu = getDevice( new EnergyCPU() );
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
        //Utils.VERBOSE = false;
        
        //CPUEnergyModel model = loadModel( Type.CONS, Mode.CONS_CONSERVATIVE );
        CPUEnergyModel model = loadModel( Type.CONS, Mode.CONS_LOAD );
        
        //CPUEnergyModel model = loadModel( Type.PESOS, Mode.PESOS_TIME_CONSERVATIVE,  500 );
        //CPUEnergyModel model = loadModel( Type.PESOS, Mode.PESOS_TIME_CONSERVATIVE, 1000 );
        //CPUEnergyModel model = loadModel( Type.PESOS, Mode.PESOS_ENERGY_CONSERVATIVE,  500 );
        //CPUEnergyModel model = loadModel( Type.PESOS, Mode.PESOS_ENERGY_CONSERVATIVE, 1000 );
        
        //CPUEnergyModel model = loadModel( Type.PERF, null );
        
        testMultiCore( model );
        //testSingleCore( model );
        //testNetworkAnimation( model );
    }
    
    protected static CPUEnergyModel loadModel( final Type type, final Mode mode, final long timeBudget ) throws Exception
    {
        // PESOS loading model.
        CPUEnergyModel model = new PESOSmodel( timeBudget, mode, "Models/PESOS/cpu_frequencies.txt" );
        model.loadModel();
        return model;
    }
    
    protected static CPUEnergyModel loadModel( final Type type, final Mode mode ) throws Exception
    {
        CPUEnergyModel model = null;
        switch ( type ) {
            case PERF:  model = new PERFmodel( "Models/PESOS/cpu_frequencies.txt" ); break;
            case CONS:  model = new CONSmodel( mode, "Models/PESOS/cpu_frequencies.txt" ); break;
            default:    break;
        }
        model.loadModel();
        return model;
    }
    
    public static void testNetworkAnimation( final CPUEnergyModel model ) throws Exception
    {
        NetworkTopology net = new NetworkTopology( "Topology/Topology_animation_test.json" );
        net.setTrackingEvent( "./Results/packets.txt" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        EventGenerator generator = new ClientGenerator( new Packet( 20, SizeUnit.MEGABYTE ),
                                                        new Packet( 20, SizeUnit.MEGABYTE ),
                                                        model );
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
            EnergyCPU cpu = new EnergyCPU( "Intel i7-4770K", 1, 1, "Models/PESOS/cpu_frequencies.txt" );
            cpu.addSampler( Global.ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, "Log/" + modelType + "_Energy.log" );
            cpu.addSampler( Global.IDLE_ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, null );
            cpu.addSampler( Global.TAIL_LATENCY_SAMPLING, null, null, "Log/" + modelType + "_Tail_Latency.log" );
            cpu.setModel( model );
            cpus.add( cpu );
            
            EventGenerator sink = new SinkGenerator( Time.INFINITE,
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
        
        sim.start( new Time( 1, TimeUnit.HOURS ) );
        
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
        
        EnergyCPU cpu = new EnergyCPU( "Intel i7-4770K", CPU_CORES, 1, "Models/PESOS/cpu_frequencies.txt" );
        cpu.setModel( model );
        
        String modelType = model.getModelType( true );
        
        cpu.addSampler( Global.ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, "Log/" + modelType + "_Energy.log" );
        cpu.addSampler( Global.IDLE_ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, null );
        cpu.addSampler( Global.TAIL_LATENCY_SAMPLING, null, null, "Log/" + modelType + "_Tail_Latency.log" );
        
        NetworkTopology net = new NetworkTopology( "Topology/Topology_mono_multiCore.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        EventGenerator generator = new ClientGenerator( new Packet( 40, SizeUnit.BYTE ),
                                                        new Packet( 40, SizeUnit.BYTE ),
                                                        model );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        EventGenerator sink = new SinkGenerator( Time.INFINITE,
                                                 new Packet( 40, SizeUnit.BYTE ),
                                                 new Packet( 20, SizeUnit.BYTE ) );
        
        Agent server = new MulticoreAgent( 1, sink );
        EventGenerator evtGen = new ServerConsGenerator( duration, new Packet( 1, SizeUnit.BYTE ),
                                                                   new Packet( 1, SizeUnit.BYTE ) );
        evtGen.connect( server );
        server.addEventGenerator( evtGen );
        server.setParallelTransmission( false ).addDevice( cpu );
        net.addAgent( server );
        
        client.getEventGenerator( 0 ).connect( server );
        
        Plotter plotter = new Plotter( "MONO MULTI_CORE - " + model.getModelType( false ), 800, 600 );
        plotter.addPlot( cpu.getSampledValues( Global.ENERGY_SAMPLING ), null, model.getModelType( false ) );
        plotter.setAxisName( "Time (h)", "Energy (J)" );
        plotter.setTicks( Axis.Y, 10 );
        plotter.setTicks( Axis.X, 24, 2 );
        plotter.setRange( Axis.Y, 0, 4350 );
        plotter.setScaleX( 60d * 60d * 1000d * 1000d );
        plotter.setVisible( true );
        
        sim.start( duration );

        Utils.LOGGER.debug( model.getModelType( false ) + " - Total energy:      " + cpu.getResultSampled( Global.ENERGY_SAMPLING ) + "J" );
        Utils.LOGGER.debug( model.getModelType( false ) + " - Total Idle energy: " + cpu.getResultSampled( Global.IDLE_ENERGY_SAMPLING ) + "J" );
        
        System.out.println( "QUERIES: " + cpu.getExecutedQueries() );
        
        // PARAMETERS                        0.03 0.03 0.01
        //              COEFFICIENTS           QUERY_FILE            NORMALIZED             PARAMETER              MATTEO                  IDLE 0
        //
        // TIME CONSERVATIVE 500ms
        // TARGET:    601670.0000000000
        // SIMULATOR: 754304.0207093941    594471.46526191400    477342.23502861796     541913.46662896510 (10%)     528119.30975986700
        // IDLE:      196257.8677944194     75247.89537985546     75247.89537980038      71066.81117570659            75247.89537980038    36100 Joule in meno
        
        // TIME CONSERVATIVE 1000ms
        // TARGET:    443730.0000000000
        // SIMULATOR: 647974.3624506982    468742.25049613975    293876.55404496676     436421.00188829670 (5%)      396748.66580365730
        // IDLE:      159577.0287529062     61183.97035269940     61183.97035269940      58061.62555988143            61183.97035273697
        
        // ENERGY CONSERVATIVE 500ms
        // TARGET:    531100.0000000000
        // SIMULATOR: 719986.3432093372    548442.27624212660    401489.20500958500     503171.36868913420 (5%)      481063.69885676424
        // IDLE:      178953.7990861060     68613.28364962358     68613.28364962358      64970.61726727840            68613.28364967453
        
        // ENERGY CONSERVATIVE 1000ms
        // TARGET:    412060.0000000000
        // SIMULATOR: 642008.7743643910    458369.63022473130    270107.72210220364     427332.62510339730 (5%)
        // IDLE:      152041.1380861678     58294.60892807464     58294.60892807464      55344.06185887506
        
        // PERF
        // TARGET:     790400.000000000
        // SIMULATOR: 1145401.600324196    992317.15024121070    940141.72685316140     862323.60355950530 (10%)    954884.43320349800
        // IDLE:       247582.811710984     94926.56637327410     94926.56637327410      82491.18617838359           75247.89537980030     60560 Joule in meno
        
        // TODO Il modello CONS e' ancora UNDER-CONSTRUCTION
        // CONS CONSERVATIVE
        // TARGET:    575000.0000000000
        // SIMULATOR:                                                                   
        // IDLE:                                                                         
        
        // CONS LOAD
        // TARGET:    575000.0000000000
        // SIMULATOR: 992317.1502406223                                                 862323.60355952530
        // IDLE:       94926.5663733480                                                  82491.18617838345
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
        
        NetworkTopology net = new NetworkTopology( "Topology/Topology_mono_singleCore.json" );
        net.setTrackingEvent( "./Results/packets2.txt" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        EventGenerator generator = new ClientGenerator( new Packet( 20, SizeUnit.BYTE ),
                                                        new Packet( 20, SizeUnit.BYTE ),
                                                        model );
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
        Plotter plotter = new Plotter( "MONO SINGLE_CORE - " + model.getModelType( false ), 800, 600 );
        for (int i = 0; i < CPU_CORES; i++) {
            EnergyCPU cpu = new EnergyCPU( "Intel i7-4770K", 1, 1, "Models/PESOS/cpu_frequencies.txt" );
            cpu.addSampler( Global.ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, "Log/" + modelType + "_Energy.log" );
            cpu.addSampler( Global.IDLE_ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, null );
            cpu.addSampler( Global.TAIL_LATENCY_SAMPLING, null, null, "Log/" + modelType + "_Tail_Latency.log" );
            cpu.setModel( model.clone() );
            cpus.add( cpu );
            
            EventGenerator sink = new SinkGenerator( Time.INFINITE,
                                                     new Packet( 20, SizeUnit.BYTE ),
                                                     new Packet( 20, SizeUnit.BYTE ) );
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
        
        sim.start( new Time( 24, TimeUnit.HOURS ) );
        
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
        
        //              COEFFICIENTS          QUERY_FILE            NORMALIZED
        //
        // TIME CONSERVATIVE 500ms
        // TARGET:    601670.0000000000
        // SIMULATOR: 754304.3610332942    594471.46526191400    477342.23502861796
        // IDLE:      196257.8677944194     75247.89537985546     75247.89537980038
        
        // TIME CONSERVATIVE 1000ms
        // TARGET:    443730.0000000000
        // SIMULATOR: 647974.3624506982    468742.25049613975    293876.55404496676
        // IDLE:      159577.0287529062     61183.97035269940     61183.97035269940
        
        // ENERGY CONSERVATIVE 500ms
        // TARGET:    531100.0000000000
        // SIMULATOR: 719986.3432093372    548442.27624212660    401489.20500958500
        // IDLE:      178953.7990861060     68613.28364962358     68613.28364962358
        
        // ENERGY CONSERVATIVE 1000ms
        // TARGET:    412060.0000000000
        // SIMULATOR: 642008.7743643910    458369.63022473130    270107.72210220364
        // IDLE:      152041.1380861678     58294.60892807464     58294.60892807464
        
        // PERF
        // TARGET:     790400.000000000
        // SIMULATOR: 1145401.600324196    992317.15024121070    940141.72685316140
        // IDLE:       247582.811710984     94926.56637327410     94926.56637327410
    }
}