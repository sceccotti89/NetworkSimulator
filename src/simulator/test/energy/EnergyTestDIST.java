
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
import simulator.events.generator.EventGenerator;
import simulator.events.impl.RequestEvent;
import simulator.events.impl.ResponseEvent;
import simulator.graphics.AnimationNetwork;
import simulator.graphics.plotter.Plotter;
import simulator.graphics.plotter.Plotter.Axis;
import simulator.test.energy.CPUModel.Mode;
import simulator.test.energy.CPUModel.PESOSmodel;
import simulator.test.energy.CPUModel.QueryInfo;
import simulator.topology.NetworkTopology;
import simulator.utils.SizeUnit;
import simulator.utils.Time;
import simulator.utils.Utils;

public class EnergyTestDIST
{
    private static final int NODES = 1;
    private static final int CPU_CORES = 4;
    
    private static final Packet PACKET = new Packet( 20, SizeUnit.BYTE );
    
    
    
    
    
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
            super( Time.INFINITE, Time.ZERO, reqPacket, resPacket );
            startAt( Time.ZERO );
            
            // Open the associated file.
            queryReader = new BufferedReader( new FileReader( QUERY_TRACE ) );
            // TODO estendere inserendo i file di tutti i nodi, ma solo se necessario
            model = new ClientModel( "Models/Monolithic/PESOS/MaxScore/time_energy.txt" );
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
                                 final Packet resPacket )
        {
            super( duration, Time.ZERO, reqPacket, resPacket );
            setDelayedResponse( true );
            setMaximumFlyingPackets( 1 );
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
                double nodeUtilization = agent.getNodeUtilization( time );
                if (nodeUtilization < minUtilization) {
                    nextDestination = i;
                    minUtilization = nodeUtilization;
                }
            }
            
            return nextDestination;
        }
    }
    
    private static class SwitchGenerator extends EventGenerator
    {
        public SwitchGenerator( final Time duration )
        {
            super( duration, Time.ZERO, PACKET, PACKET );
            setDelayedResponse( true );
        }
        
        @Override
        public Packet makePacket( final Event e, final long destination )
        {
            Packet packet;
            if (e instanceof RequestEvent) {
                packet = getResponsePacket();
            } else {
                // New request from client: get a copy.
                //System.out.println( "INPUT EVENT: " + e + ", PACKET: " + e.getPacket() );
                packet = e.getPacket();
                /*if (packet.hasContent( Global.QUERY_ID ) && PREDICTION_ON_SWITCH) {
                    //System.out.println( "INOLTRO RICHIESTA: " + packet.getContent( Global.QUERY_ID ) );
                    //System.out.println( "DESTINATION: " + destination );
                    
                }*/
            }
            
            return packet;
        }
    }
    
    private static class SwitchAgent extends Agent //implements EventHandler
    {
        public SwitchAgent( final long id, final EventGenerator evGenerator )
        {
            super( id );
            addEventGenerator( evGenerator );
            //addEventHandler( this );
        }
        
        /*@Override
        public Time handle( final Event e, final EventType type )
        {
            /*System.out.println( "SWITCH HANDLE: " + e + ", TYPE: " + type );
            if (type == EventType.RECEIVED) {
                if (e.getSource().getId() == getId() && e.getPacket().hasContent( Global.PESOS_CPU_FREQUENCY)) {
                    // Message generated by this agent.
                    
                    //System.out.println( "RECEIVED: " + e.getPacket().getContent( Global.QUERY_ID ) );
                    return Time.ZERO;
                }
            } else {*/
                /*if (type == EventType.RECEIVED) {
                    // Event generated by a node as a response to the client.
                    computeIdleEnergy( e.getSource().getId(), e.getTime() );
                }*/
            //}
            
            /*return _node.getTcalc();
        }*/
        
        /*public void computeIdleEnergy( final long sourceId, final Time time )
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
        }*/
        
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
    
    
    
    
    
    
    private static class MulticoreGenerator extends EventGenerator
    {
        public MulticoreGenerator( final Time duration ) {
            super( duration, Time.ZERO, PACKET, PACKET );
        }
        
        @Override
        protected Packet makePacket( final Event e, final long destination )
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
            PESOSmodel model = (PESOSmodel) cpu.getModel();
            
            if (p.hasContent( Global.PESOS_TIME_BUDGET )) {
                // TODO implementare
                /*model.timeBudget = p.getContent( Global.PESOS_TIME_BUDGET );
                long coreID = p.getContent( Global.PESOS_TIME_BUDGET );
                cpu.evalFrequency( e.getTime(), cpu.getCore( coreID ) );*/
            } else {
                //System.out.println( "AGGIUNGO EVENTO: " + e );
                QueryInfo query = model.getQuery( p.getContent( Global.QUERY_ID ) );
                //System.out.println( "RECEIVED QUERY: " + p.getContent( Global.QUERY_ID ) );
                System.out.println( "TIME: " + e.getTime() + ", AGGIUNTA QUERY AL NODO: " + getId() + ", QUERY: " + query );
                query.setEvent( e );
                query.setArrivalTime( e.getTime() );
                cpu.addQuery( cpu.selectCore( e.getTime(), query ), query );
            }
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
                if (e.getPacket().hasContent( Global.QUERY_ID )) {
                    System.out.println( "NODO: " + getId() + ", ESEGUO PROSSIMA QUERY SUL NODO: " + getId() );
                    // Compute the time to complete the query.
                    EnergyCPU cpu = getDevice( new EnergyCPU() );
                    return cpu.timeToCompute( null );
                } else {
                    return Time.ZERO;
                }
            }
            
            return null;
        }
        
        @Override
        public double getNodeUtilization( final Time time ) {
            return getDevice( new EnergyCPU() ).getUtilization( time );
        }
        
        @Override
        public void shutdown()
        {
            EnergyCPU cpu = getDevice( new EnergyCPU() );
            cpu.computeIdleEnergy( getEventScheduler().getTimeDuration() );
            super.shutdown();
        }
    }
    
    
    
    
    
    
    
    public static void main( final String[] args ) throws Exception
    {
        Utils.VERBOSE = false;
        
        execute( Mode.TIME_CONSERVATIVE,  500 );
        //execute( Mode.TIME_CONSERVATIVE, 1000 );
        //execute( Mode.ENERGY_CONSERVATIVE,  500 );
        //execute( Mode.ENERGY_CONSERVATIVE, 1000 );
    }
    
    private static void execute( final Mode mode, final long timeBudget ) throws Exception
    {
        testMultiCore( timeBudget, mode );
        //testSingleCore( timeBudget, mode );
        //testAnimationNetwork( timeBudget, mode );
    }
    
    public static void testAnimationNetwork( final long timeBudget, final Mode mode ) throws Exception
    {
        NetworkTopology net = new NetworkTopology( "Topology/Animation/Topology_distributed_multiCore.json" );
        net.setTrackingEvent( "Results/distr_multi_core.txt" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        // Create client.
        EventGenerator generator = new ClientGenerator( PACKET, PACKET );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        EventGenerator switchGen = new SwitchGenerator( Time.INFINITE );
        
        // Create switch.
        Agent switchAgent = new SwitchAgent( 1, switchGen );
        net.addAgent( switchAgent );
        client.getEventGenerator( 0 ).connect( switchAgent );
        
        final String modelType = "PESOS_" + mode + "_" + timeBudget + "ms";
        
        List<EnergyCPU> cpus = new ArrayList<>( NODES );
        for (int i = 0; i < NODES; i++) {
            EnergyCPU cpu = new EnergyCPU( "Intel i7-4770K", CPU_CORES, 1, "Models/cpu_frequencies.txt" );
            cpu.addSampler( Global.ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, "Log/" + modelType + "_Energy.log" );
            cpu.addSampler( Global.IDLE_ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, null );
            cpu.addSampler( Global.TAIL_LATENCY_SAMPLING, null, null, "Log/" + modelType + "_Tail_Latency.log" );
            cpus.add( cpu );
            
            // Add the PESOS model to the corresponding cpu.
            final String directory = "Models/Distributed/Node_" + (i+1) + "/PESOS/MaxScore/";
            CPUModel model = new PESOSmodel( timeBudget, mode, directory );
            model.loadModel();
            cpu.setModel( model );
            
            EventGenerator sink = new MulticoreGenerator( Time.INFINITE );
            Agent agentCore = new MulticoreAgent( 2 + i, sink );
            agentCore.addDevice( cpu );
            net.addAgent( agentCore );
            
            switchGen.connect( agentCore );
        }
        
        sim.start( new Time( 24, TimeUnit.HOURS ), false );
        //sim.start( new Time( 13100, TimeUnit.MICROSECONDS ), false );
        sim.close();
        
        // Show the animation.
        AnimationNetwork an = new AnimationNetwork( 800, 600, modelType );
        an.loadSimulation( "Topology/Animation/Topology_distributed_multiCore.json", "./Results/distr_multi_core.txt" );
        an.setTargetFrameRate( 90 );
        an.setForceExit( false );
        an.start();
    }
    
    public static void testSingleCore( final long timeBudget, final Mode mode ) throws IOException
    {
        /*
                                   / node_0   / node_2
                                  / dynamic  / dynamic
               1000Mb,0ms        /          /
        client ---------- switch  ----------  --------- node_4
          0ms               0ms  \          \          dynamic
                                  \          \
                                   \ node_1   \ node_3
                                    dynamic    dynamic
        
                  / core_i_0  / core_i_2
                 /  dynamic  /  dynamic
                /           /
        node_i  ----------
          0ms   \           \
                 \           \
                  \ core_i_2  \ core_i_3
                    dynamic     dynamic
        */
        
        NetworkTopology net = new NetworkTopology( "Topology/Topology_distributed_singleCore.json" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        
        EventGenerator generator = new ClientGenerator( PACKET, PACKET );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        EventGenerator switchGen = new SwitchGenerator( Time.INFINITE );
        Agent switchAgent = new SwitchAgent( 1, switchGen );
        net.addAgent( switchAgent );
        client.getEventGenerator( 0 ).connect( switchAgent );
        
        CPUModel model = new PESOSmodel( timeBudget, mode, "Models/Monolithic/PESOS/MaxScore/",
                                               "predictions.txt", "time_energy.txt", "regressors.txt" );
        model.loadModel();
        String modelType = model.getModelType( true );
        
        Plotter plotter = new Plotter( "DISTRIBUTED SINGLE_CORE - " + model.getModelType( false ), 800, 600 );
        List<EnergyCPU> cpus = new ArrayList<>( NODES * CPU_CORES );
        final Time samplingTime = new Time( 5, TimeUnit.MINUTES );
        // Add the CPU nodes.
        for (int i = 0; i < NODES; i++) {
            EventGenerator anyGen = new AnycastGenerator( Time.INFINITE, PACKET, PACKET );
            Agent switchNode = new SwitchAgent( i * CPU_CORES + 2 + i, anyGen );
            net.addAgent( switchNode );
            switchAgent.getEventGenerator( 0 ).connect( switchNode );
            
            // Add the core to each CPU node.
            for (int j = 0; j < NODES; j++) {
                EnergyCPU cpu = new EnergyCPU( "Intel i7-4770K", 1, 1, "Models/cpu_frequencies.txt" );
                cpu.addSampler( Global.ENERGY_SAMPLING, samplingTime, Sampling.CUMULATIVE, "Log/" + modelType + "_Energy.log" );
                cpu.addSampler( Global.IDLE_ENERGY_SAMPLING, samplingTime, Sampling.CUMULATIVE, null );
                cpu.addSampler( Global.TAIL_LATENCY_SAMPLING, null, null, "Log/" + modelType + "_Tail_Latency.log" );
                cpus.add( cpu );
                
                // Add the PESOS model to the corresponding cpu.
                final String directory = "Models/Distributed/Node_" + (i+1) + "/PESOS/MaxScore/";
                model = new PESOSmodel( timeBudget, mode, directory );
                model.loadModel();
                cpu.setModel( model );
                
                EventGenerator sink = new MulticoreGenerator( Time.INFINITE );
                final long id = i * (CPU_CORES + 1) + 3 + j;
                Agent agentCore = new MulticoreAgent( id, sink );
                agentCore.addDevice( cpu );
                net.addAgent( agentCore );
                
                switchNode.getEventGenerator( 0 ).connect( agentCore );
                
                plotter.addPlot( cpu.getSampledValues( Global.ENERGY_SAMPLING ), "Node " + id + " " + model.getModelType( true ) );
            }
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
        for (int i = 0; i < NODES * CPU_CORES; i++) {
            EnergyCPU cpu = cpus.get( i );
            totalEnergy += cpu.getResultSampled( Global.ENERGY_SAMPLING );
            totalIdleEnergy += cpu.getResultSampled( Global.IDLE_ENERGY_SAMPLING );
            Utils.LOGGER.info( "CPU " + i + ", Energy consumed: " + cpu.getResultSampled( Global.ENERGY_SAMPLING ) + "J" );
            totalQueries += cpu.getExecutedQueries();
        }
        Utils.LOGGER.info( model.getModelType( false ) + " - Total energy:      " + totalEnergy + "J" );
        Utils.LOGGER.info( model.getModelType( false ) + " - Total idle energy: " + totalIdleEnergy + "J" );
        System.out.println( "QUERIES: " + totalQueries );
        
        
        // TIME CONSERVATIVE 500ms
        // 
        // SIMULATOR: 
        // IDLE:      
        
        // TIME CONSERVATIVE 1000ms
        // 
        // SIMULATOR: 
        // IDLE:      
        
        // ENERGY CONSERVATIVE 500ms
        // 
        // SIMULATOR: 
        // IDLE:      
        
        // ENERGY CONSERVATIVE 1000ms
        // 
        // SIMULATOR: 
        // IDLE:         
    }
    
    public static void testMultiCore( final long timeBudget, final Mode mode ) throws Exception
    {
        /*
        FIXME I link dallo switch ai nodi hanno tutti una banda di 1Gb/s e 0 ms di latenza.
        
                                   / node0    / node2
                                  / dynamic  / dynamic
               1000Mb,0ms        /          /
        client ---------- switch  ---------  --------- node4
          0ms               0ms  \          \         dynamic
                                  \          \
                                   \ node1    \ node3
                                    dynamic    dynamic
        */
        
        NetworkTopology net = new NetworkTopology( "Topology/Topology_distributed_multiCore.json" );
        net.setTrackingEvent( "Results/distr_multi_core.txt" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        EventGenerator generator = new ClientGenerator( PACKET, PACKET );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        EventGenerator switchGen = new SwitchGenerator( Time.INFINITE );
        
        Agent switchAgent = new SwitchAgent( 1, switchGen );
        net.addAgent( switchAgent );
        client.getEventGenerator( 0 ).connect( switchAgent );
        
        CPUModel model = new PESOSmodel( timeBudget, mode, "Models/Monolithic/PESOS/MaxScore/" );
        model.loadModel();
        String modelType = model.getModelType( true );
        
        List<EnergyCPU> cpus = new ArrayList<>( NODES );
        Plotter plotter = new Plotter( "DISTRIBUTED MULTI_CORE - " + model.getModelType( false ), 800, 600 );
        for (int i = 0; i < NODES; i++) {
            EnergyCPU cpu = new EnergyCPU( "Intel i7-4770K", CPU_CORES, 1, "Models/cpu_frequencies.txt" );
            cpu.addSampler( Global.ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, "Log/" + modelType + "_Energy.log" );
            cpu.addSampler( Global.IDLE_ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, null );
            cpu.addSampler( Global.TAIL_LATENCY_SAMPLING, null, null, "Log/" + modelType + "_Tail_Latency.log" );
            cpus.add( cpu );
            
            // Add the PESOS model to the corresponding cpu.
            final String directory = "Models/Distributed/Node_" + (i+1) + "/PESOS/MaxScore/";
            model = new PESOSmodel( timeBudget, mode, directory );
            model.loadModel();
            cpu.setModel( model );
            
            EventGenerator sink = new MulticoreGenerator( Time.INFINITE );
            Agent agentCore = new MulticoreAgent( 2 + i, sink );
            agentCore.addDevice( cpu );
            net.addAgent( agentCore );
            
            switchGen.connect( agentCore );
            
            plotter.addPlot( cpu.getSampledValues( Global.ENERGY_SAMPLING ), "Node " + (i+1) + " " + model.getModelType( true ) );
        }
        
        plotter.setAxisName( "Time (h)", "Energy (J)" );
        plotter.setTicks( Axis.Y, 10 );
        plotter.setTicks( Axis.X, 24, 2 );
        plotter.setRange( Axis.Y, 0, 4350 );
        plotter.setScaleX( 60d * 60d * 1000d * 1000d );
        plotter.setVisible( true );
        
        sim.start( new Time( 24, TimeUnit.HOURS ), false );
        sim.close();
        
        double totalEnergy = 0;
        double totalIdleEnergy = 0;
        int totalQueries = 0;
        for (int i = 0; i < NODES; i++) {
            EnergyCPU cpu = cpus.get( i );
            totalEnergy += cpu.getResultSampled( Global.ENERGY_SAMPLING );
            totalIdleEnergy += cpu.getResultSampled( Global.IDLE_ENERGY_SAMPLING );
            Utils.LOGGER.info( "CPU " + i + ", Energy consumed: " + cpu.getResultSampled( Global.ENERGY_SAMPLING ) + "J" );
            totalQueries += cpu.getExecutedQueries();
        }
        Utils.LOGGER.info( model.getModelType( false ) + " - Total energy:      " + totalEnergy + "J" );
        Utils.LOGGER.info( model.getModelType( false ) + " - Total idle energy: " + totalIdleEnergy + "J" );
        System.out.println( "QUERIES: " + totalQueries );
        
        
        // TIME CONSERVATIVE 500ms
        // 
        // SIMULATOR: 
        // IDLE:      
        
        // TIME CONSERVATIVE 1000ms
        // 
        // SIMULATOR: 
        // IDLE:      
        
        // ENERGY CONSERVATIVE 500ms
        // 
        // SIMULATOR: 
        // IDLE:      
        
        // ENERGY CONSERVATIVE 1000ms
        // 
        // SIMULATOR:  
        // IDLE:       
    }
}