
package simulator.test.energy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import simulator.core.Agent;
import simulator.core.Device.Sampler.Sampling;
import simulator.core.Simulator;
import simulator.core.Task;
import simulator.events.Event;
import simulator.events.EventGenerator;
import simulator.events.EventHandler;
import simulator.events.Packet;
import simulator.events.impl.RequestEvent;
import simulator.events.impl.ResponseEvent;
import simulator.graphics.plotter.Plotter;
import simulator.graphics.plotter.Plotter.Axis;
import simulator.test.energy.CPUEnergyModel.Mode;
import simulator.test.energy.CPUEnergyModel.*;
import simulator.test.energy.CPUEnergyModel.QueryInfo;
import simulator.topology.NetworkTopology;
import simulator.utils.SizeUnit;
import simulator.utils.Time;
import simulator.utils.Utils;

public class EnergyTest
{
    private static final int CPU_CORES = 4;
    private static final int SWITCHES = 2;
    
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
            super( id, evGenerator );
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
            super( id, evGenerator );
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
            for (Agent dest : _destinations) {
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
            for (Agent agent : _destinations) {
                utilization += agent.getNodeUtilization( time );
            }
            return utilization;
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
    
    private static class CoreAgent extends Agent implements EventHandler
    {
        public CoreAgent( final long id, final EventGenerator evtGen )
        {
            super( id, evtGen );
            addEventHandler( this );
        }
        
        @Override
        public void addEventOnQueue( final Event e )
        {
            Packet p = e.getPacket();
            EnergyCPU cpu = getDevice( new EnergyCPU() );
            CPUEnergyModel model = (CPUEnergyModel) cpu.getModel();
            QueryInfo query = model.getQuery( p.getContent( Global.QUERY_ID ) );
            query.setEvent( e );
            query.setArrivalTime( e.getArrivalTime() );
            cpu.addQuery( cpu.selectCore( e.getArrivalTime() ), query );
        }
        
        @Override
        public Time handle( final Event e, final EventType type )
        {
            if (e instanceof ResponseEvent) {
                EnergyCPU cpu = getDevice( new EnergyCPU() );
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
                EnergyCPU cpu = getDevice( new EnergyCPU() );
                Task task = new Task();
                task.addContent( Global.EVENT_ID, e.getId() );
                return cpu.timeToCompute( task );
            }
            
            return null;
        }
        
        @Override
        public double getNodeUtilization( final Time time ) {
            return getDevice( new EnergyCPU() ).getUtilization( time );
        }
    }
    
    
    
    
    
    
    private static class MulticoreAgent extends Agent implements EventHandler
    {
        public MulticoreAgent( final long id, final EventGenerator evGenerator )
        {
            super( id, evGenerator );
            addEventHandler( this );
        }
        
        @Override
        public void addEventOnQueue( final Event e )
        {
            Packet p = e.getPacket();
            EnergyCPU cpu = getDevice( new EnergyCPU() );
            CPUEnergyModel model = (CPUEnergyModel) cpu.getModel();
            QueryInfo query = model.getQuery( p.getContent( Global.QUERY_ID ) );
            //System.out.println( "RECEIVED QUERY: " + p.getContent( Global.QUERY_ID ) );
            query.setEvent( e );
            query.setArrivalTime( e.getArrivalTime() );
            cpu.addQuery( cpu.selectCore( e.getArrivalTime() ), query );
        }
        
        @Override
        public Time handle( final Event e, final EventType type )
        {
            if (e instanceof ResponseEvent) {
                EnergyCPU cpu = getDevice( new EnergyCPU() );
                if (type == EventType.GENERATED) {
                    QueryInfo query = cpu.getLastQuery();
                    query.setEvent( e );
                } else {
                    // Set the time of the cpu as (at least) the time of the sending event.
                    cpu.setTime( e.getTime() );
                    cpu.checkQueryCompletion( e.getTime() );
                }
            } else {
                // Compute the time to complete the query.
                EnergyCPU cpu = getDevice( new EnergyCPU() );
                return cpu.timeToCompute( null );
            }
            
            return null;
        }
    }
    
    
    
    
    
    
    
    public static void main( final String[] args ) throws IOException
    {
        Utils.VERBOSE = false;
        
        Global.eventWriter = new PrintWriter( "./Results/packets.txt" );
        execute( Mode.TIME_CONSERVATIVE,  500 );
        //execute( Mode.TIME_CONSERVATIVE, 1000 );
        //execute( Mode.ENERGY_CONSERVATIVE,  500 );
        //execute( Mode.ENERGY_CONSERVATIVE, 1000 );
        //execute( null, 0 );
        Global.eventWriter.close();
    }
    
    private static void execute( final Mode mode, final long timeBudget ) throws IOException
    {
        CPUEnergyModel model;
        if (mode == null)
            model = new PERFmodel( timeBudget, mode, "Models/PESOS/cpu_frequencies.txt" );
        else
            model = new PESOSmodel( timeBudget, mode, "Models/PESOS/cpu_frequencies.txt" );
        model.loadModel();
        
        testDistributedSingleNode( model );
        //testDistributedMultipleNodes( model );
        //testNodeMulticore( model );
        //testDistributedMulticore( model );
    }
    
    public static void testDistributedSingleNode( final CPUEnergyModel model ) throws IOException
    {
        /*
                                   / core0    / core2
                                  / dynamic  / dynamic
               1000Mb,0ms        /          /
        client ---------- switch  ---------
          0ms               0ms  \          \
                                  \          \
                                   \ core1    \ core3
                                    dynamic    dynamic
        */
        
        NetworkTopology net = new NetworkTopology( "Topology/Topology_multicore.json" );
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
        
        String modelType = model.getModelType( true );
        
        client.connect( switchAgent );
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
                                                     new Packet( 20, SizeUnit.BYTE ),
                                                     new Packet( 20, SizeUnit.BYTE ) );
            Agent agentCore = new CoreAgent( 2 + i, sink );
            agentCore.addDevice( cpu );
            net.addAgent( agentCore );
            
            switchAgent.connect( agentCore );
            
            plotter.addPlot( cpu.getSampledValues( Global.ENERGY_SAMPLING ), null, "Node " + i + " " + model.getModelType( true ) );
        }
        
        plotter.setAxisName( "Time (h)", "Energy (J)" );
        plotter.setTicks( Axis.Y, 10 );
        plotter.setTicks( Axis.X, 24, 2 );
        plotter.setRange( Axis.Y, 0, 1800 );
        plotter.setScaleX( 60d * 60d * 1000d * 1000d );
        plotter.setVisible( true );
        
        sim.start( new Time( 24, TimeUnit.HOURS ) );
        
        sim.stop();
        
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

    public static void testDistributedMultipleNodes( final CPUEnergyModel model ) throws IOException
    {
        // TODO Per adesso uso soltanto 2 switch
        /*
                                   / switch_0  / switch_2
                                  /  dynamic  /  dynamic
               1000Mb,0ms        /           /
        client ---------- switch  ----------
          0ms               0ms  \           \
                                  \           \
                                   \ switch_1  \ switch_3
                                     dynamic     dynamic
        
                   / node_i_0  / node_i_2
                  /  dynamic  /  dynamic
                 /           /
        switch_i  ----------
          0ms    \           \
                  \           \
                   \ node_i_2  \ node_i_3
                     dynamic     dynamic
        */
        
        NetworkTopology net = new NetworkTopology( "Topology/Topology_distributed.json" );
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
        client.connect( switchAgent );
        
        List<EnergyCPU> cpus = new ArrayList<>( CPU_CORES * SWITCHES );
        Plotter plotter = new Plotter( model.getModelType( false ), 800, 600 );
        
        String modelType = model.getModelType( true );
        
        // Add the switch nodes.
        for (int i = 0; i < SWITCHES; i++) {
            anyGen = new AnycastGenerator( Time.INFINITE,
                                           new Packet( 20, SizeUnit.BYTE ),
                                           new Packet( 20, SizeUnit.BYTE ) );
            Agent switchNode = new SwitchAgent( i * CPU_CORES + 2 + i, anyGen );
            net.addAgent( switchNode );
            switchAgent.connect( switchNode );
            
            // Add the CPU nodes to each switch.
            for (int j = 0; j < CPU_CORES; j++) {
                EnergyCPU cpu = new EnergyCPU( "Intel i7-4770K", 1, 1, "Models/PESOS/cpu_frequencies.txt" );
                cpu.addSampler( Global.ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, "Log/" + modelType + "_Energy.log" );
                cpu.addSampler( Global.IDLE_ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, null );
                cpu.addSampler( Global.TAIL_LATENCY_SAMPLING, null, null, "Log/" + modelType + "_Tail_Latency.log" );
                cpu.setModel( model );
                cpus.add( cpu );
                
                EventGenerator sink = new SinkGenerator( Time.INFINITE,
                                                         new Packet( 20, SizeUnit.BYTE ),
                                                         new Packet( 20, SizeUnit.BYTE ) );
                long id = i * (CPU_CORES + 1) + 3 + j;
                Agent agentCore = new CoreAgent( id, sink );
                agentCore.addDevice( cpu );
                net.addAgent( agentCore );
                
                switchNode.connect( agentCore );
                
                plotter.addPlot( cpu.getSampledValues( Global.ENERGY_SAMPLING ), null, "Node " + id + " " + model.getModelType( true ) );
            }
        }
        
        plotter.setAxisName( "Time (h)", "Energy (J)" );
        plotter.setTicks( Axis.Y, 10 );
        plotter.setTicks( Axis.X, 24, 2 );
        plotter.setRange( Axis.Y, 0, 1800 );
        plotter.setScaleX( 60d * 60d * 1000d * 1000d );
        plotter.setVisible( true );
        
        sim.start( new Time( 24, TimeUnit.HOURS ) );
        
        sim.stop();
        
        double totalEnergy = 0;
        double totalIdleEnergy = 0;
        int totalQueries = 0;
        for (int i = 0; i < CPU_CORES * SWITCHES; i++) {
            EnergyCPU cpu = cpus.get( i );
            totalEnergy += cpu.getResultSampled( Global.ENERGY_SAMPLING );
            totalIdleEnergy += cpu.getResultSampled( Global.IDLE_ENERGY_SAMPLING );
            Utils.LOGGER.info( "CPU " + i + ", Energy consumed: " + cpu.getResultSampled( Global.ENERGY_SAMPLING ) + "J" );
            totalQueries += cpu.getExecutedQueries();
        }
        Utils.LOGGER.info( model.getModelType( false ) + " - Total energy:      " + totalEnergy + "J" );
        Utils.LOGGER.info( model.getModelType( false ) + " - Total idle energy: " + totalIdleEnergy + "J" );
        System.out.println( "QUERIES: " + totalQueries );
        
        //             COEFFICIENTS          QUERY_FILE            NORMALIZED
        //
        // TIME CONSERVATIVE 500ms
        // TARGET:    601670.0000000000
        // SIMULATOR: 
        // IDLE:      
        
        // TIME CONSERVATIVE 1000ms
        // TARGET:    443730.0000000000
        // SIMULATOR: 
        // IDLE:      
        
        // ENERGY CONSERVATIVE 500ms
        // TARGET:    531100.0000000000
        // SIMULATOR: 
        // IDLE:      
        
        // ENERGY CONSERVATIVE 1000ms
        // TARGET:    412060.0000000000
        // SIMULATOR: 
        // IDLE:      
        
        // PERF
        // TARGET:     790400.000000000
        // SIMULATOR:                ??    1104635.5244105107    
        // IDLE:                     ??    207244.94054257227    
    }

    public static void testNodeMulticore( final CPUEnergyModel model ) throws IOException
    {
        /*
                   1000Mb,0ms
        client <---------------> server
          0ms                    DYNAMIC
        */
        
        EnergyCPU cpu = new EnergyCPU( "Intel i7-4770K", CPU_CORES, 1, "Models/PESOS/cpu_frequencies.txt" );
        cpu.setModel( model );
        
        String modelType = model.getModelType( true );
        
        cpu.addSampler( Global.ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, "Log/" + modelType + "_Energy.log" );
        cpu.addSampler( Global.IDLE_ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, null );
        cpu.addSampler( Global.TAIL_LATENCY_SAMPLING, null, null, "Log/" + modelType + "_Tail_Latency.log" );
        
        NetworkTopology net = new NetworkTopology( "Topology/Topology_nodeMulticore.json" );
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
        server.setParallelTransmission( false ).addDevice( cpu );
        net.addAgent( server );
        
        client.connect( server );
        
        Plotter plotter = new Plotter( model.getModelType( false ), 800, 600 );
        plotter.addPlot( cpu.getSampledValues( Global.ENERGY_SAMPLING ), null, model.getModelType( false ) );
        plotter.setAxisName( "Time (h)", "Energy (J)" );
        plotter.setTicks( Axis.Y, 10 );
        plotter.setTicks( Axis.X, 24, 2 );
        plotter.setRange( Axis.Y, 0, 4350 );
        plotter.setScaleX( 60d * 60d * 1000d * 1000d );
        plotter.setVisible( true );
        
        sim.start( new Time( 24, TimeUnit.HOURS ) );
        
        sim.stop();

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
    }
    
    public static void testDistributedMulticore( final CPUEnergyModel model ) throws IOException
    {
        // TODO Per adesso utilizzo soltanto 2 nodi, ciascuno con 4 core.
        /*
        TODO i link dallo switch ai vari nodi sono per adesso di 1Gb e 0 ms di latenza.
                                   / node0    / node2
                                  / dynamic  / dynamic
               1000Mb,0ms        /          /
        client ---------- switch  ---------
          0ms               0ms  \          \
                                  \          \
                                   \ node1    \ node3
                                    dynamic    dynamic
        */
        
        NetworkTopology net = new NetworkTopology( "Topology/Topology_multicore.json" );
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
        client.connect( switchAgent );
        
        final int CPU_NODES = 2;
        String modelType = model.getModelType( true );
        List<EnergyCPU> cpus = new ArrayList<>( CPU_CORES );
        Plotter plotter = new Plotter( model.getModelType( false ), 800, 600 );
        for (int i = 0; i < CPU_NODES; i++) {
            EnergyCPU cpu = new EnergyCPU( "Intel i7-4770K", CPU_CORES, 1, "Models/PESOS/cpu_frequencies.txt" );
            cpu.addSampler( Global.ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, "Log/" + modelType + "_Energy.log" );
            cpu.addSampler( Global.IDLE_ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, null );
            cpu.addSampler( Global.TAIL_LATENCY_SAMPLING, null, null, "Log/" + modelType + "_Tail_Latency.log" );
            cpu.setModel( model );
            cpus.add( cpu );
            
            EventGenerator sink = new SinkGenerator( Time.INFINITE,
                                                     new Packet( 20, SizeUnit.BYTE ),
                                                     new Packet( 20, SizeUnit.BYTE ) );
            Agent agentCore = new CoreAgent( CPU_NODES + i, sink );
            agentCore.addDevice( cpu );
            net.addAgent( agentCore );
            
            switchAgent.connect( agentCore );
            
            plotter.addPlot( cpu.getSampledValues( Global.ENERGY_SAMPLING ), null, "Node " + i + " " + model.getModelType( true ) );
        }
        
        plotter.setAxisName( "Time (h)", "Energy (J)" );
        plotter.setTicks( Axis.Y, 10 );
        plotter.setTicks( Axis.X, 24, 2 );
        plotter.setRange( Axis.Y, 0, 1800 );
        plotter.setScaleX( 60d * 60d * 1000d * 1000d );
        plotter.setVisible( true );
        
        sim.start( new Time( 24, TimeUnit.HOURS ) );
        
        sim.stop();
        
        double totalEnergy = 0;
        double totalIdleEnergy = 0;
        int totalQueries = 0;
        for (int i = 0; i < 2; i++) {
            EnergyCPU cpu = cpus.get( i );
            totalEnergy += cpu.getResultSampled( Global.ENERGY_SAMPLING );
            totalIdleEnergy += cpu.getResultSampled( Global.IDLE_ENERGY_SAMPLING );
            Utils.LOGGER.info( "CPU " + i + ", Energy consumed: " + cpu.getResultSampled( Global.ENERGY_SAMPLING ) + "J" );
            totalQueries += cpu.getExecutedQueries();
        }
        Utils.LOGGER.info( model.getModelType( false ) + " - Total energy:      " + totalEnergy + "J" );
        Utils.LOGGER.info( model.getModelType( false ) + " - Total idle energy: " + totalIdleEnergy + "J" );
        System.out.println( "QUERIES: " + totalQueries );
        
         //             COEFFICIENTS          QUERY_FILE            NORMALIZED
         //
         // TIME CONSERVATIVE 500ms
         // SIMULATOR: 
         // IDLE:      
         
         // TIME CONSERVATIVE 1000ms
         // SIMULATOR: 
         // IDLE:      
         
         // ENERGY CONSERVATIVE 500ms
         // SIMULATOR: 
         // IDLE:      
         
         // ENERGY CONSERVATIVE 1000ms
         // SIMULATOR:  
         // IDLE:       
         
         // PERF
         // SIMULATOR:  
         // IDLE:       
    }
}