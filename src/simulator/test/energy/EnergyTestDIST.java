
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
import simulator.test.energy.CPUEnergyModel.Mode;
import simulator.test.energy.CPUEnergyModel.PESOSmodel;
import simulator.test.energy.CPUEnergyModel.QueryInfo;
import simulator.topology.NetworkTopology;
import simulator.utils.SizeUnit;
import simulator.utils.Time;
import simulator.utils.Utils;

public class EnergyTestDIST
{
    private static final int NODES = 5;
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
    
    private static class MulticastGenerator extends EventGenerator
    {
        public MulticastGenerator( final Time duration,
                                   final Packet reqPacket,
                                   final Packet resPacket ) {
            super( duration, Time.ZERO, Utils.INFINITE, reqPacket, resPacket, false, true, false );
            setMulticast( true, true );
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
                } else { // EventType.SENT event.
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
        
        @Override
        public double getNodeUtilization( final Time time ) {
            return getDevice( new EnergyCPU() ).getUtilization( time );
        }
    }
    
    
    
    
    
    
    
    public static void main( final String[] args ) throws Exception
    {
        //Utils.VERBOSE = false;
        
        execute( Mode.PESOS_TIME_CONSERVATIVE,  500 );
        //execute( Mode.PESOS_TIME_CONSERVATIVE, 1000 );
        //execute( Mode.PESOS_ENERGY_CONSERVATIVE,  500 );
        //execute( Mode.PESOS_ENERGY_CONSERVATIVE, 1000 );
    }
    
    private static void execute( final Mode mode, final long timeBudget ) throws Exception
    {
        testMultiCore( timeBudget, mode );
        //testSingleCore( timeBudget, mode );
        //testNetworkAnimation( timeBudget, mode );
    }
    
    public static void testNetworkAnimation( final long timeBudget, final Mode mode ) throws Exception
    {
        NetworkTopology net = new NetworkTopology( "Topology/Animation/Topology_distributed_multiCore.json" );
        net.setTrackingEvent( "Results/distr_multi_core.txt" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        CPUEnergyModel model = new PESOSmodel( timeBudget, mode, "Models/cpu_frequencies.txt" );
        model.loadModel();
        EventGenerator generator = new ClientGenerator( new Packet( 20, SizeUnit.BYTE ),
                                                        new Packet( 20, SizeUnit.BYTE ),
                                                        model );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        EventGenerator anyGen = new MulticastGenerator( Time.INFINITE,
                                                        new Packet( 20, SizeUnit.BYTE ),
                                                        new Packet( 20, SizeUnit.BYTE ) );
        Agent switchAgent = new SwitchAgent( 1, anyGen );
        net.addAgent( switchAgent );
        client.getEventGenerator( 0 ).connect( switchAgent );
        
        String modelType = model.getModelType( true );
        
        List<EnergyCPU> cpus = new ArrayList<>( NODES );
        for (int i = 0; i < NODES; i++) {
            EnergyCPU cpu = new EnergyCPU( "Intel i7-4770K", 1, 1, "Models/cpu_frequencies.txt" );
            cpu.addSampler( Global.ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, "Log/" + modelType + "_Energy.log" );
            cpu.addSampler( Global.IDLE_ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, null );
            cpu.addSampler( Global.TAIL_LATENCY_SAMPLING, null, null, "Log/" + modelType + "_Tail_Latency.log" );
            cpus.add( cpu );
            
            // Add the PESOS model to the corresponding cpu.
            if (mode == Mode.PESOS_TIME_CONSERVATIVE) {
                model = new PESOSmodel( timeBudget, mode, "Models/cpu_frequencies.txt",
                                        "predictions.txt", "time_energy.txt", "regressors.txt" );
            } else {
                model = new PESOSmodel( timeBudget, mode, "Models/cpu_frequencies.txt",
                                        "predictions_node"+i+".txt", "time_energy_node"+i+".txt", "regressors_normse_node"+i+".txt" );
            }
            model.loadModel();
            cpu.setModel( model );
            
            EventGenerator sink = new SinkGenerator( Time.INFINITE,
                                                     new Packet( 20, SizeUnit.BYTE ),
                                                     new Packet( 20, SizeUnit.BYTE ) );
            Agent agentCore = new MulticoreAgent( 2 + i, sink );
            agentCore.addDevice( cpu );
            net.addAgent( agentCore );
            
            switchAgent.getEventGenerator( 0 ).connect( agentCore );
        }
        
        sim.start( new Time( 24, TimeUnit.HOURS ) );
        
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
        
        CPUEnergyModel model = new PESOSmodel( timeBudget, mode, "Models/cpu_frequencies.txt" );
        model.loadModel();
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
        
        List<EnergyCPU> cpus = new ArrayList<>( NODES * CPU_CORES );
        Plotter plotter = new Plotter( "DISTR SINGLE_CORE - " + model.getModelType( false ), 800, 600 );
        
        String modelType = model.getModelType( true );
        
        // Add the CPU nodes.
        for (int i = 0; i < NODES; i++) {
            anyGen = new AnycastGenerator( Time.INFINITE,
                                           new Packet( 20, SizeUnit.BYTE ),
                                           new Packet( 20, SizeUnit.BYTE ) );
            Agent switchNode = new SwitchAgent( i * CPU_CORES + 2 + i, anyGen );
            net.addAgent( switchNode );
            switchAgent.getEventGenerator( 0 ).connect( switchNode );
            
            // Add the core to each CPU node.
            for (int j = 0; j < NODES; j++) {
                EnergyCPU cpu = new EnergyCPU( "Intel i7-4770K", 1, 1, "Models/cpu_frequencies.txt" );
                cpu.addSampler( Global.ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, "Log/" + modelType + "_Energy.log" );
                cpu.addSampler( Global.IDLE_ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, null );
                cpu.addSampler( Global.TAIL_LATENCY_SAMPLING, null, null, "Log/" + modelType + "_Tail_Latency.log" );
                cpus.add( cpu );
                
                // Add the PESOS model to the corresponding cpu.
                if (mode == Mode.PESOS_TIME_CONSERVATIVE) {
                    model = new PESOSmodel( timeBudget, mode, "Models/cpu_frequencies.txt",
                                            "predictions_node"+i+".txt", "time_energy_node"+i+".txt", "regressors_node"+i+".txt" );
                } else {
                    model = new PESOSmodel( timeBudget, mode, "Models/cpu_frequencies.txt",
                                            "predictions_node"+i+".txt", "time_energy_node"+i+".txt", "regressors_normse_node"+i+".txt" );
                }
                model.loadModel();
                cpu.setModel( model );
                
                EventGenerator sink = new SinkGenerator( Time.INFINITE,
                                                         new Packet( 20, SizeUnit.BYTE ),
                                                         new Packet( 20, SizeUnit.BYTE ) );
                long id = i * (CPU_CORES + 1) + 3 + j;
                Agent agentCore = new MulticoreAgent( id, sink );
                agentCore.addDevice( cpu );
                net.addAgent( agentCore );
                
                switchNode.getEventGenerator( 0 ).connect( agentCore );
                
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
        
        //             COEFFICIENTS          QUERY_FILE            NORMALIZED
        //
        // TIME CONSERVATIVE 500ms
        // TARGET:    
        // SIMULATOR: 
        // IDLE:      
        
        // TIME CONSERVATIVE 1000ms
        // TARGET:    
        // SIMULATOR: 
        // IDLE:      
        
        // ENERGY CONSERVATIVE 500ms
        // TARGET:    
        // SIMULATOR: 
        // IDLE:      
        
        // ENERGY CONSERVATIVE 1000ms
        // TARGET:    
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
        
        CPUEnergyModel model = new PESOSmodel( timeBudget, mode, "Models/cpu_frequencies.txt" );
        model.loadModel();
        EventGenerator generator = new ClientGenerator( new Packet( 20, SizeUnit.BYTE ),
                                                        new Packet( 20, SizeUnit.BYTE ),
                                                        model );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        EventGenerator anyGen = new MulticastGenerator( Time.INFINITE,
                                                        new Packet( 20, SizeUnit.BYTE ),
                                                        new Packet( 20, SizeUnit.BYTE ) );
        Agent switchAgent = new SwitchAgent( 1, anyGen );
        net.addAgent( switchAgent );
        client.getEventGenerator( 0 ).connect( switchAgent );
        
        String modelType = model.getModelType( true );
        
        List<EnergyCPU> cpus = new ArrayList<>( NODES );
        Plotter plotter = new Plotter( "DISTR MULTI_CORE - " + model.getModelType( false ), 800, 600 );
        for (int i = 0; i < NODES; i++) {
            EnergyCPU cpu = new EnergyCPU( "Intel i7-4770K", 1, 1, "Models/cpu_frequencies.txt" );
            cpu.addSampler( Global.ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, "Log/" + modelType + "_Energy.log" );
            cpu.addSampler( Global.IDLE_ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, null );
            cpu.addSampler( Global.TAIL_LATENCY_SAMPLING, null, null, "Log/" + modelType + "_Tail_Latency.log" );
            cpus.add( cpu );
            
            // Add the PESOS model to the corresponding cpu.
            if (mode == Mode.PESOS_TIME_CONSERVATIVE) {
                model = new PESOSmodel( timeBudget, mode, "Models/cpu_frequencies.txt",
                                        "predictions_node"+i+".txt", "time_energy_node"+i+".txt", "regressors_node"+i+".txt" );
            } else {
                model = new PESOSmodel( timeBudget, mode, "Models/cpu_frequencies.txt",
                                        "predictions_node"+i+".txt", "time_energy_node"+i+".txt", "regressors_normse_node"+i+".txt" );
            }
            model.loadModel();
            cpu.setModel( model );
            
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
        plotter.setRange( Axis.Y, 0, 5800 );
        plotter.setScaleX( 60d * 60d * 1000d * 1000d );
        plotter.setVisible( true );
        
        sim.start( new Time( 24, TimeUnit.HOURS ) );
        
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
        
        //      COEFFICIENTS          QUERY_FILE            NORMALIZED
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
    }
}