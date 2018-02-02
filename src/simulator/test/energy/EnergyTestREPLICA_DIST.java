
package simulator.test.energy;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
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
import simulator.test.energy.CPU.Core.State;
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
import simulator.topology.NetworkLink;
import simulator.topology.NetworkTopology;
import simulator.utils.Pair;
import simulator.utils.Sampler;
import simulator.utils.Sampler.Sampling;
import simulator.utils.SizeUnit;
import simulator.utils.Time;
import simulator.utils.Utils;
import simulator.utils.resources.ResourceLoader;

public class EnergyTestREPLICA_DIST
{
    private static final int NODES = 1;
    private static final int REPLICAS_PER_NODE = 2;
    
    private static final Packet PACKET = new Packet( 20, SizeUnit.BYTE );
    
    private static boolean PESOS_CONTROLLER = false;
    private static boolean PEGASUS_CONTROLLER = false;
    private static boolean SWITCH_OFF_MACHINES = false;
    
    private static List<CPU> cpus = new ArrayList<>( NODES );
    
    private static PESOScontroller controller;
    
    private static int arrivalEstimator;
    private static int latencyNormalization;
    private static double lambda;
    
    private static boolean REPLICA_SWITCH = false;
    
    private static Map<String,Double> testResults;
    
    
    
    
    
    private static class ClientGenerator extends EventGenerator
    {
        private static final String QUERY_TRACE = "Models/msn.day2.arrivals.txt";
        //private static final String QUERY_TRACE = "Models/test_arrivals.txt";
        private static final int NUM_QUERIES = 10000;
        // Random generator seed.
        private static final int SEED = 50000;
        private final Random RANDOM = new Random( SEED );
        
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
            //System.out.println( "DESTINATION: " + destination );
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
    
    private static class BrokerAgent extends Agent implements EventHandler
    {
        private PrintWriter writer;
        private List<QueryLatency> queries;
        private PEGASUS pegasus;
        private long queryDistrId = -1;
        
        public BrokerAgent( long id, long target, EventGenerator evGenerator, String model )
                throws IOException
        {
            super( NetworkAgent.FULL_DUPLEX, NetworkLayer.APPLICATION, id );
            addEventGenerator( evGenerator );
            addEventHandler( this );
            
            writer = new PrintWriter( "Log/Distributed_Replica_" + model + "_Tail_Latency.log", "UTF-8" );
            queries = new ArrayList<>( 1 << 10 );
            
            if (PEGASUS_CONTROLLER) {
                pegasus = new PEGASUS( target, cpus );
            }
        }
        
        @Override
        public Time handle( Event e, EventType type )
        {
            if (type == EventType.RECEIVED) {
                if (e.getSource().getId() == 0) {
                    queryDistrId = (queryDistrId + 1) % Long.MAX_VALUE;
                    e.getPacket().addContent( Global.QUERY_DISTR_ID, queryDistrId );
                    queries.add( new QueryLatency( queryDistrId, e.getTime() ) );
                } else {
                    long queryDistrId = e.getPacket().getContent( Global.QUERY_DISTR_ID );
                    QueryLatency query = null;
                    int index;
                    for (index = 0; index < queries.size(); index++) {
                        QueryLatency ql = queries.get( index );
                        if (ql.id == queryDistrId) {
                            query = ql;
                            break;
                        }
                    }
                    
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
    
    
    
    
    
    
    private static class TimeSlotGenerator extends CBRGenerator
    {
        public static final Time PERIOD = new Time( 1, TimeUnit.SECONDS );
        
        public TimeSlotGenerator( Time duration ) {
            super( Time.ZERO, duration, PERIOD, PACKET, PACKET );
        }
        
        @Override
        public Packet makePacket( Event e, long destination )
        {
            Packet packet = getRequestPacket();
            packet.addContent( Global.SWITCH_TIME_SLOT, "" );
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
            EnergyCPU cpu = getDevice( EnergyCPU.class );
            //EnergyCPU cpu = getDevice( CPU );
            CPUModel model = (CPUModel) cpu.getModel();
            
            //System.out.println( "RECEIVED QUERY: " + e.getPacket().getContents() );
            
            if (p.hasContent( Global.PESOS_TIME_BUDGET )) {
                //PESOSmessage message = p.getContent( Global.PESOS_TIME_BUDGET );
                //PESOScore core = (PESOScore) cpu.getCore( message.getCoreID() );
                //core.setTimeBudget( e.getTime(), message.getTimeBudget(), message.getQueryID() );
            }
            
            if (p.hasContent( Global.SWITCH_TIME_SLOT )) {
                cpu.evalCONSparameters( e.getTime() );
            } else {
                QueryInfo query = model.getQuery( p.getContent( Global.QUERY_ID ) );
                query.setDistributedId( p.getContent( Global.QUERY_DISTR_ID ) );
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
                EnergyCPU cpu = getDevice( EnergyCPU.class );
                if (type == EventType.GENERATED) {
                    QueryInfo query = cpu.getLastQuery();
                    Packet p = e.getPacket();
                    p.addContent( Global.QUERY_ID, query.getId() );
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
                    EnergyCPU cpu = getDevice( EnergyCPU.class );
                    return cpu.timeToCompute( null );
                } else {
                    return Time.ZERO;
                }
            }
            
            return _node.getTcalc();
        }
        
        @Override
        public double getNodeUtilization( Time time )
        {
            EnergyCPU cpu = getDevice( EnergyCPU.class );
            /*double utilization = Double.MAX_VALUE;
            for (Core core : cpu.getCores()) {
                // In case of nodes with the same lowest frequency, the size of the queue
                // determines who is the node with the lower number of queries.
                /*double frequency = core.getFrequency() + core.getQueue().size();
                if (frequency < utilization) {
                    utilization = frequency;
                }*/
                /*utilization = Math.min( utilization,
                                        ((PESOScore) core).getTotalQueryExecutionTime( time )
                              );
            }
            return utilization;*/
            return cpu.getUtilization( time );
        }
        
        @Override
        public void shutdown() throws IOException
        {
            EnergyCPU cpu = getDevice( EnergyCPU.class );
            cpu.computeIdleEnergy( getEventScheduler().getTimeDuration() );
            super.shutdown();
        }
    }
    
    
    
    private static class SwitchTimeSlotGenerator extends CBRGenerator
    {
        public static final Time TIME_SLOT = new Time( 15, TimeUnit.MINUTES );
        
        public SwitchTimeSlotGenerator( Time duration ) {
            super( Time.ZERO, duration.clone().subTime( TIME_SLOT ), TIME_SLOT, PACKET, PACKET );
        }
        
        @Override
        public Packet makePacket( Event e, long destination )
        {
            Packet packet = getRequestPacket();
            packet.addContent( Global.SWITCH_TIME_SLOT, "" );
            return packet;
        }
    }
    
    private static class SwitchGenerator extends EventGenerator
    {
        private int nextReplica = -1;
        private Map<Long,Long> tieSelected;
        
        
        public SwitchGenerator( Time duration )
        {
            super( duration, Time.ZERO, PACKET, PACKET );
            setDelayedResponse( true );
            setMaximumFlyingPackets( 1 );
            
            tieSelected = new HashMap<>();
        }
        
        @Override
        public EventGenerator connect( Agent to )
        {
            tieSelected.put( to.getId(), 0L );
            return super.connect( to );
        }
        
        @Override
        public Packet makePacket( Event e, long destination )
        {
            if (e instanceof RequestEvent) {
                // Response message from a replica node.
                return super.makePacket( e, destination );
            } else {
                // New request from client: clone the packet.
                return e.getPacket().clone();
            }
        }
        
        @Override
        protected int selectDestination( Time time )
        {
            // Bounded Round-Robin.
            SwitchAgent switchAgent = (SwitchAgent) getAgent();
            int maxReplicas = switchAgent.getCurrentReplicas();
            //System.out.println( "REPLICAS: " + maxReplicas );
            //nextReplica = (nextReplica + 1) % maxReplicas;
            
            /*if (maxReplicas == 2) {
                Agent dest = _destinations.get( 0 );
                CPU cpu = dest.getDevice( EnergyCPU.class );
                for (Core core : cpu.getCores()) {
                    System.out.println( "F: " + core.getFrequency() + ", Q: " + core.getQueue().size() );
                }
                System.exit( 0 );
            }*/
            
            // Minimum utilization.
            nextReplica = -1;
            double minUtilization = Double.MAX_VALUE;
            long tiedSelection = Long.MAX_VALUE;
            Agent selected = null;
            boolean tieSituation = false;
            //String value = "";
            for (int index = 0; index < maxReplicas; index++) {
                Agent dest = _destinations.get( index );
                double utilization = dest.getNodeUtilization( time );
                //value += "NODE: " + (index) + ": " + utilization + ", ";
                if (utilization < minUtilization) {
                    nextReplica = index;
                    minUtilization = utilization;
                    tieSituation = false;
                } else if (utilization == minUtilization) {
                    if (tieSelected.get( dest.getId() ) < tiedSelection) {
                        nextReplica = index;
                        minUtilization = utilization;
                        tiedSelection = tieSelected.get( dest.getId() );
                        selected = dest;
                    }
                    tieSituation = true;
                }
            }
            
            if (tieSituation) {
                tieSelected.put( selected.getId(), tiedSelection + 1 );
            }
            
            //System.out.println( value );
            
            return nextReplica;
        }
    }
    
    public static class SwitchAgent extends Agent implements EventHandler
    {
        protected List<Agent> destinations;
        
        private int queries;
        
        protected int currentReplicas = 1;
        
        private List<Double> meanArrivalQueries;
        private List<Double> meanCompletionTime;
        
        private int timeSlots;
        private int[] allReplicas;
        private ReplicatedGraph graph;
        
        private int slotIndex = -1;
        
        public static final int SEASONAL_ESTIMATOR            = 0;
        public static final int SEASONAL_ESTIMATOR_WITH_DRIFT = 1;
        private int estimatorType;
        
        // Used in seasonal estimator with drift.
        private Integer arrivals;
        private List<Integer> currentArrivals;
        
        // Watt dissipated by the associated CPU.
        private static final double Pstandby = 0.9;
        private static final double Pon      = 84;
        
        // The Lambda value used to balance the equation (lower for latency, higher for power).
        private final double LAMBDA;
        
        // Parameters used to normalize the latency.
        private static double ALPHA = -0.01;
        private int latency_normalization;
        
        //private static final Time WAKE_UP_TIME = new Time( 200, TimeUnit.SECONDS );
        
        
        
        public SwitchAgent( long id, int estimatorType, int latencyNormalization,
                            double lambda, EventGenerator evGenerator ) throws IOException
        {
            super( NetworkAgent.FULL_DUPLEX, NetworkLayer.APPLICATION, id );
            addEventGenerator( evGenerator );
            addEventHandler( this );
            
            destinations = evGenerator.getDestinations();
            
            LAMBDA = lambda;
            this.estimatorType = estimatorType;
            this.latency_normalization = latencyNormalization;
            
            loadMeanArrivalTime();
            loadQueryPerSlot();
            
            timeSlots = meanCompletionTime.size();
            
            if (estimatorType == SEASONAL_ESTIMATOR) {
                createGraph();
                //System.exit( 0 );
            } else {
                arrivals = 0;
                currentArrivals = new ArrayList<>( 2 );
            }
        }
        
        private void loadMeanArrivalTime() throws IOException
        {
            meanCompletionTime = new ArrayList<>( 128 );
            InputStream loader = ResourceLoader.getResourceAsStream( "Results/MeanCompletionTime.log" );
            BufferedReader reader = new BufferedReader( new InputStreamReader( loader ) );
            String line = null;
            while ((line = reader.readLine()) != null) {
                double time = Double.parseDouble( line.split( " " )[1] );
                meanCompletionTime.add( time );
            }
            reader.close();
        }
        
        private void loadQueryPerSlot() throws IOException
        {
            meanArrivalQueries = new ArrayList<>( 128 );
            InputStream loader = ResourceLoader.getResourceAsStream( "Results/QueryPerTimeSlot.log" );
            BufferedReader reader = new BufferedReader( new InputStreamReader( loader ) );
            String line = null;
            while ((line = reader.readLine()) != null) {
                double time = Double.parseDouble( line.split( " " )[1] );
                meanArrivalQueries.add( time );
            }
            reader.close();
        }
        
        @Override
        public void addEventOnQueue( Event e )
        {
            if (estimatorType == SEASONAL_ESTIMATOR_WITH_DRIFT) {
                long sourceId = e.getSource().getId();
                if (sourceId != getId()) {
                    if (sourceId != 1) {
                        // From replica.
                        queries--;
                    } else {
                        // From client.
                        arrivals++;
                        queries++;
                    }
                }
            }
        }
        
        public int getCurrentReplicas() {
            return currentReplicas;
        }
        
        /**
         * Sets the state of the associated nodes.
         * 
         * @param time    time of evaluation.
        */
        protected void setNodesState( Time time )
        {
            /*// Turn-on the sleeping nodes.
            for (int i = 0; i < currentReplicas; i++) {
                destinations.get( i ).getDevice( EnergyCPU.class ).setState( time, State.RUNNING );
            }
            
            if (slotIndex < timeSlots - 1) {
                if (estimatorType == SEASONAL_ESTIMATOR) {
                    if (allReplicas[slotIndex + 1] > currentReplicas) {
                        // Turn-on the subsequent needed replicas.
                        Time powerOn = time.clone().addTime( SwitchTimeSlotGenerator.TIME_SLOT )/*.subTime( WAKE_UP_TIME )*/;
                        /*for (int i = currentReplicas; i < allReplicas[slotIndex+1]; i++) {
                            destinations.get( i ).getDevice( EnergyCPU.class ).setState( powerOn, State.RUNNING );
                        }
                    } else {
                        if (SWITCH_OFF_MACHINES) {
                            for (int i = currentReplicas; i < REPLICAS_PER_NODE; i++) {
                                destinations.get( i ).getDevice( EnergyCPU.class ).setState( time, State.POWER_OFF );
                            }
                        }
                    }
                } else {
                    // Turn-on the current needed replicas.
                    for (int i = 0; i < currentReplicas; i++) {
                        destinations.get( i ).getDevice( EnergyCPU.class ).setState( time, State.RUNNING );
                    }
                    
                    if (SWITCH_OFF_MACHINES) {
                        for (int i = currentReplicas; i < REPLICAS_PER_NODE; i++) {
                            destinations.get( i ).getDevice( EnergyCPU.class ).setState( time, State.POWER_OFF );
                        }
                    }
                }
            } else {
                // Last slot: just turn-off the unused nodes.
                if (SWITCH_OFF_MACHINES) {
                    for (int i = currentReplicas; i < REPLICAS_PER_NODE; i++) {
                        destinations.get( i ).getDevice( EnergyCPU.class ).setState( time, State.POWER_OFF );
                    }
                }
            }*/
            
            // Turn-on the current needed replicas.
            for (int i = 0; i < currentReplicas; i++) {
                destinations.get( i ).getDevice( EnergyCPU.class ).setState( time, State.RUNNING );
            }
            
            if (SWITCH_OFF_MACHINES) {
                for (int i = currentReplicas; i < REPLICAS_PER_NODE; i++) {
                    destinations.get( i ).getDevice( EnergyCPU.class ).setState( time, State.POWER_OFF );
                }
            }
        }
        
        @Override
        public Time handle( Event e, EventType type )
        {
            Packet p = e.getPacket();
            if (p.hasContent( Global.SWITCH_TIME_SLOT )) {
                if (estimatorType == SEASONAL_ESTIMATOR) {
                    currentReplicas = allReplicas[++slotIndex];
                } else {
                    // Get the number of replicas using also the current informations.
                    if (slotIndex >= 0) {
                        currentArrivals.add( arrivals );
                        if (currentArrivals.size() > 2) {
                            currentArrivals.remove( 0 );
                        }
                        arrivals = 0;
                    }
                    
                    currentReplicas = getReplicas( ++slotIndex );
                }
                setNodesState( e.getTime() );
            }
            return _node.getTcalc();
        }
        
        /**
         * Returns the number of replicas, based only on the current values.
         * 
         * @param slotIndex    the time slot to evaluate.
         * 
         * @return the number of needed replicas.
        */
        private int getReplicas( int slotIndex )
        {
            // Select the results from a previous day.
            double completionExtimation = meanCompletionTime.get( slotIndex );
            double arrivalExtimation    = meanArrivalQueries.get( slotIndex );
            if (currentArrivals.size() == 2) {
                arrivalExtimation += (currentArrivals.get( 1 ) - currentArrivals.get( 0 ));
            }
            
            double minWeight = Double.MAX_VALUE;
            int replicas = 0;
            for (int nodes = 1; nodes <= REPLICAS_PER_NODE; nodes++) {
                double power   = getPowerCost( nodes );
                double latency = getLatencyCost( queries, arrivalExtimation, completionExtimation, nodes );
                double weight  = LAMBDA * power + (1 - LAMBDA) * latency;
                //System.out.println( "NODE: " + nodes + ", WEIGHT: " + weight + ", MIN_WEIGHT: " + minWeight );
                if (weight < minWeight) {
                    minWeight = weight;
                    replicas = nodes;
                }
            }
            
            //System.out.println( "SELECTED REPLICAS: " + replicas );
            
            return replicas;
        }
        
        private void createGraph()
        {
            graph = new ReplicatedGraph( timeSlots + 1, REPLICAS_PER_NODE );
            
            graph.addNode( 0 );
            for (int i = 0; i < timeSlots; i++) {
                for (int nodes = 1; nodes <= REPLICAS_PER_NODE; nodes++) {
                    final int index = i * REPLICAS_PER_NODE + nodes;
                    graph.addNode( index );
                    for (int j = 0; j < REPLICAS_PER_NODE; j++) {
                        final int fromNode = index - nodes - j;
                        if (fromNode < 0) {
                            break;
                        } else {
                            graph.connectNodes( fromNode, index );
                        }
                    }
                }
            }
            // Last node.
            final int index = timeSlots * REPLICAS_PER_NODE + 1;
            graph.addNode( index, 0 );
            for (int i = 1; i <= REPLICAS_PER_NODE; i++) {
                graph.connectNodes( index - i, index );
            }
            
            //System.out.println( "Graph: \n" + graph.toString() );
            allReplicas = new int[timeSlots];
            graph.computeMinimumPath( this, allReplicas );
        }
        
        public double getWeight( double queries, double nodes, int slotIndex )
        {
            final double completionExtimation = meanCompletionTime.get( slotIndex );
            final double arrivalExtimation    = meanArrivalQueries.get( slotIndex );
            
            double power   = getPowerCost( nodes );
            double latency = getLatencyCost( queries, arrivalExtimation, completionExtimation, nodes );
            //System.out.println( "POWER: " + power + ", LATENCY: " + latency );
            double weight  = LAMBDA * power + (1 - LAMBDA) * latency;
            
            return weight;
        }
        
        public int getNextQueries( double queries, double nodes, int slotIndex )
        {
            final double completionExtimation = meanCompletionTime.get( slotIndex );
            final double arrivalExtimation    = meanArrivalQueries.get( slotIndex );
            
            final long Ts = SwitchTimeSlotGenerator.TIME_SLOT.getTimeMicros();
            int nextQueries = Math.max( 0, (int) (queries - ((nodes * Ts) / completionExtimation) + arrivalExtimation) );
            return nextQueries;
        }
        
        private double getPowerCost( double nodes ) {
            return (Pon * nodes + Pstandby * (REPLICAS_PER_NODE - nodes)) / (Pon * REPLICAS_PER_NODE);
        }
        
        private double getLatencyCost( double queuedQueries, double newQueries, double meanCompletiontime, double nodes )
        {
            //System.out.println( "QUERIES: " + queuedQueries + ", NEW_QUERIES: " + newQueries + ", MEAN_COMPLETION_TIME: " + meanCompletiontime + ", NODES: " + nodes );
            final double Tk = (((queuedQueries + newQueries) * meanCompletiontime) / nodes) / Utils.MILLION;
            final double Ts = SwitchTimeSlotGenerator.TIME_SLOT.getTimeMicros() / Utils.MILLION;
            //System.out.println( "Tk: " + Tk + ", Ts: " + Ts + ", DIFFERENCE: " + (Tk - Ts) );
            switch (latency_normalization) {
                case( 1 ) : return 1 - Math.exp( ALPHA * Tk );
                case( 2 ) : return (Tk <= Ts) ? 0 : 1;
                case( 3 ) : return (Tk <= Ts) ? 0 : 1 - Math.exp( ALPHA * (Tk - Ts) );
            }
            return 0;
        }

        public String getMode( boolean compressed )
        {
            if (compressed) {
                return LAMBDA + "_" +
                        ((estimatorType == SEASONAL_ESTIMATOR) ? "LongTerm" : "ShortTerm") +
                         "_L" + latency_normalization;
            } else {
                return "Lambda=" + LAMBDA + ",Type=" +
                        ((estimatorType == SEASONAL_ESTIMATOR) ? "LongTerm" : "ShortTerm") +
                         ",L=" + latency_normalization;
            }
        }
    }
    
    private static class ReplicaSwitch extends SwitchAgent implements EventHandler
    {
        private double targetConsumption;
        
        private double targetRo;
        private int index = -1;
        private List<Double> nodeUtilization;
        
        private static int orderedIndex = -1;
        private List<Double> orderedNodeUtilization;
        
        
        
        public ReplicaSwitch( long id, double targetRo, double targetConsumption,
                              String model, EventGenerator generator ) throws Exception
        {
            super( id, 1, 1, 0.25, generator ); // Dummy parameters.
            addEventHandler( this );
            
            this.targetRo = targetRo;
            
            nodeUtilization = new ArrayList<>();
            InputStream loader = ResourceLoader.getResourceAsStream( "Results/QueueSize_" + model + ".log" );
            BufferedReader reader = new BufferedReader( new InputStreamReader( loader ) );
            String line = null;
            while ((line = reader.readLine()) != null) {
                double ro = Double.parseDouble( line.split( " " )[1] );
                nodeUtilization.add( ro );
            }
            
            orderedNodeUtilization = new ArrayList<>( nodeUtilization );
            Collections.sort( orderedNodeUtilization );
            
            orderedIndex = (orderedIndex + 1) % nodeUtilization.size();
            this.targetRo = orderedNodeUtilization.get( orderedIndex );
        }
        
        @Override
        public Time handle( Event e, EventType type )
        {
            Packet p = e.getPacket();
            if (p.hasContent( Global.SWITCH_TIME_SLOT )) {
                if (index == nodeUtilization.size() - 1) {
                    double energy = getSampler( Global.ENERGY_SAMPLING ).getTotalResult();
                    if (energy > targetConsumption) {
                        // Increment the target utilization factor.
                        //targetRo += TARGET_INCREMENT;
                        //Sampler samper = getSampler( Global.ENERGY_SAMPLING );
                        //samper.reset();
                    }
                }
                index = (index + 1) % nodeUtilization.size();
                if (nodeUtilization.get( index ) >= targetRo) {
                    // Increase the number of replicas.
                    currentReplicas = 2;
                } else {
                    // Reduce the number of replicas.
                    //currentReplicas = Math.max( currentReplicas - 1, 1 );
                    currentReplicas = 1;
                }
                System.out.println( "RO: " + nodeUtilization.get( index ) + ", TARGET: " + targetRo + ", REPLICAS: " + currentReplicas );
                setNodesState( e.getTime() );
            }
            
            return getNode().getTcalc();
        }
        
        public String getMode( boolean compressed ) {
            return "T=" + targetRo;
        }
    }
    
    
    
    
    
    
    
    public static void main( String[] args ) throws Exception
    {
        Utils.VERBOSE = false;
        REPLICA_SWITCH = true;
        
        //System.setProperty( "showGUI", "false" );
        if (System.getProperty( "showGUI" ) != null) {
            Global.showGUI = System.getProperty( "showGUI" ).equalsIgnoreCase( "true" );
        }
        
        testResults = new LinkedHashMap<>();
        
        SWITCH_OFF_MACHINES  = true;
        
        arrivalEstimator     = SwitchAgent.SEASONAL_ESTIMATOR;
        latencyNormalization = 2;
        lambda               = 0.5;
        
        // 485156
        // 471354 (0.159) =>  -2.8%
        
        // 313337
        // 252640 (0.250) => -19.3%
        
        testNetwork( Type.PESOS, Mode.TIME_CONSERVATIVE,  500 );
        //testNetwork( Type.PESOS, Mode.TIME_CONSERVATIVE, 1000 );
        //testNetwork( Type.PESOS, Mode.ENERGY_CONSERVATIVE,  500 );
        //testNetwork( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 1000 );
        
        /*arrivalEstimator = SwitchAgent.SEASONAL_ESTIMATOR;
        for (int i = 1; i <= 3; i++) {
            for (latencyNormalization = 1; latencyNormalization <= 3; latencyNormalization++) {
                lambda = 0.25d * i;
                testNetwork( Type.PESOS, Mode.TIME_CONSERVATIVE, 500 );
            }
        }*/
        
        /*arrivalEstimator = SwitchAgent.SEASONAL_ESTIMATOR_WITH_DRIFT;
        for (latencyNormalization = 1; latencyNormalization <= 3; latencyNormalization++) {
            for (int i = 1; i <= 3; i++) {
                lambda = 0.25d * i;
                testNetwork( Type.PESOS, Mode.TIME_CONSERVATIVE, 500 );
            }
        }*/
        
        for (Entry<String,Double> entry : testResults.entrySet()) {
            Utils.LOGGER.info( entry.getKey() + ", " + entry.getValue() );
        }
        
        //testNetwork( Type.PERF, null, 0 );
        //testNetwork( Type.CONS, null, 0 );
        
        //testNetwork( Type.PEGASUS, null,  500 );
        //testNetwork( Type.PEGASUS, null, 1000 );
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
        
        NetworkTopology net = new NetworkTopology();
        Simulator sim = new Simulator( net );
        
        // Create client.
        EventGenerator generator = new ClientGenerator( PACKET, PACKET );
        Agent client = new ClientAgent( 0, generator );
        net.addNode( 0, "Client", 0 );
        net.addAgent( client );
        
        // Create query broker.
        EventGenerator brokerGen = new BrokerGenerator( duration );
        net.addNode( 1, "Broker", 0 );
        
        CPUModel model = getModel( type, mode, timeBudget, 1 );
        final String modelType = model.getModelType( true );
        Plotter plotter = null;
        if (Global.showGUI) {
            plotter = new Plotter( "", 800, 600 );
        }
        
        controller = null;
        cpus.clear();
        
        String compressedReplicaMode = null;
        String extendedReplicaMode   = null;
        final Time samplingTime = new Time( 5, TimeUnit.MINUTES );
        for (int i = 0; i < NODES; i++) {
            CPUModel p_model = loadModel( type, mode, timeBudget, i+1 );
            p_model.loadModel();
            
            // Create the switch associated to the REPLICA nodes.
            EventGenerator switchGen = new SwitchGenerator( duration );
            final long switchId = 2 + i * REPLICAS_PER_NODE + i;
            SwitchAgent switchNode;
            if (REPLICA_SWITCH) {
                switchNode = new ReplicaSwitch( switchId, 0.177, 313337, model.getModelType( false ), switchGen );
            } else {
                switchNode = new SwitchAgent( switchId, arrivalEstimator, latencyNormalization, lambda, switchGen );
            }
            net.addNode( switchId, "Switch_" + (i+1), 0 );
            // From broker (1) to switch.
            net.addLink( 1, switchId, 1000, 0, NetworkLink.BIDIRECTIONAL );
            net.addAgent( switchNode );
            brokerGen.connect( switchNode );
            
            // Get the type of simulation.
            if (compressedReplicaMode == null) {
                compressedReplicaMode = switchNode.getMode( true );
                extendedReplicaMode   = switchNode.getMode( false );
            }
            
            // Add the time slot generator.
            EventGenerator timeSlotGenerator = new SwitchTimeSlotGenerator( duration );
            switchNode.addEventGenerator( timeSlotGenerator );
            timeSlotGenerator.connect( switchNode );
            
            for (int j = 0; j < REPLICAS_PER_NODE; j++) {
                final long nodeId = (i * REPLICAS_PER_NODE + j + 1);
                CPU cpu = new EnergyCPU( "Models/cpu_spec.json", getCoreClass( type ) );
                cpus.add( cpu );
                
                // Add the model to the corresponding CPU.
                cpu.setModel( p_model );
                
                EventGenerator sink = new MulticoreGenerator( duration );
                final long id = i * (REPLICAS_PER_NODE + 1) + 3 + j;
                Agent agentCore = new MulticoreAgent( id, sink );
                agentCore.addDevice( cpu );
                net.addNode( id, "node" + (i/REPLICAS_PER_NODE+1) + "_" + (i%REPLICAS_PER_NODE), 0 );
                // From switch to core (id).
                net.addLink( switchId, id, 1000, 0, NetworkLink.BIDIRECTIONAL );
                net.addAgent( agentCore );
                
                agentCore.addSampler( Global.ENERGY_SAMPLING, new Sampler( samplingTime, "Log/Distributed_Replica_" + modelType + "_" + compressedReplicaMode + "_Node" + nodeId + "_Energy.log", Sampling.CUMULATIVE ) );
                agentCore.addSampler( Global.IDLE_ENERGY_SAMPLING, new Sampler( samplingTime, null, Sampling.CUMULATIVE ) );
                
                if (type == Type.CONS) {
                    EventGenerator evtGen = new TimeSlotGenerator( duration );
                    evtGen.connect( agentCore );
                    agentCore.addEventGenerator( evtGen );
                }
                
                if (Global.showGUI) {
                    plotter.addPlot( agentCore.getSampler( Global.ENERGY_SAMPLING ).getValues(), "Node " + nodeId );
                }
                
                switchGen.connect( agentCore );
                // Create PESOS controller.
                if (type == Type.PESOS && PESOS_CONTROLLER && controller == null) {
                    if (controller == null) {
                        controller = new PESOScontroller( timeBudget * 1000, mode, cpus, NODES, cpu.getCPUcores() );
                    }
                    // Connect the agent.
                    controller.connect( agentCore );
                }
            }
        }
        
        //System.out.println( "TEST_MODE: " + testMode );
        Agent brokerAgent = new BrokerAgent( 1, timeBudget * 1000, brokerGen, modelType + "_" + compressedReplicaMode );
        // From client (0) to broker (1).
        net.addLink( 0, 1, 1000, 0, NetworkLink.BIDIRECTIONAL );
        net.addAgent( brokerAgent );
        client.getEventGenerator( 0 ).connect( brokerAgent );
        
        if (Global.showGUI) {
            plotter.setTitle( "DISTRIBUTED REPLICA (" + extendedReplicaMode + ") - " + modelType );
            plotter.setAxisName( "Time (h)", "Energy (J)" );
            plotter.setTicks( Axis.Y, 10 );
            plotter.setTicks( Axis.X, 23, 2 );
            plotter.setRange( Axis.Y, 0, 4300 );
            plotter.setScaleX( 60d * 60d * 1000d * 1000d );
            plotter.setVisible( true );
        }
        
        sim.start( duration, false );
        sim.close();
        
        double totalEnergy = 0;
        for (int i = 0; i < NODES * REPLICAS_PER_NODE; i++) {
            CPU cpu = cpus.get( i );
            double energy = cpu.getAgent().getSampler( Global.ENERGY_SAMPLING ).getTotalResult();
            double energyIdle = cpu.getAgent().getSampler( Global.IDLE_ENERGY_SAMPLING ).getTotalResult();
            totalEnergy += energy;
            Utils.LOGGER.info( extendedReplicaMode + " " + model.getModelType( false ) +
                               " - CPU (" + (i/REPLICAS_PER_NODE + 1) + "-" + (i%REPLICAS_PER_NODE) + ") => Energy: " + energy +
                               ", Idle: " + energyIdle + ", Queries: " + cpu.getExecutedQueries() );
        }
        Utils.LOGGER.info( extendedReplicaMode + " " + model.getModelType( false ) + " - Total energy: " + totalEnergy );
        
        // Show the animation.
        if (Global.showGUI) {
            /*AnimationNetwork an = new AnimationNetwork( 800, 600, modelType );
            an.loadSimulation( "Topology/Animation/Topology_distributed_multiCore.json",
                               "./Results/distr_multi_core.txt" );
            an.setTargetFrameRate( 90 );
            an.setForceExit( false );
            an.start();*/
            
            plotTailLatency( type, mode, timeBudget, compressedReplicaMode, extendedReplicaMode );
        }
        
        String result = modelType + "_" + compressedReplicaMode + "_" + (SWITCH_OFF_MACHINES ? "off" : "on");
        testResults.put( result, totalEnergy );
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
    
    public static void plotEnergyConsumption( int node, Type type, long time_budget, String mode,
                                              String compressedReplicaMode, String extendedReplicaMode ) throws IOException
    {
        Plotter plotter = new Plotter( "DISTRIBUTED REPLICA Energy Consumption", 800, 600 );
        plotter.setAxisName( "Time (h)", "Energy (J)" );
        double yRange = time_budget * 1000d + 200000d;
        plotter.setRange( Axis.Y, 0, yRange );
        plotter.setTicks( Axis.Y, (int) (yRange / 100000) );
        plotter.setScaleY( 1000d );
        
        plotter.setRange( Axis.X, 0, TimeUnit.HOURS.toMicros( 24 ) );
        plotter.setTicks( Axis.X, 23, 2 );
        plotter.setScaleX( TimeUnit.HOURS.toMicros( 1 ) );
        
        switch ( type ) {
            case PESOS :
                plotter.addPlot( "Log/Distributed_Replica_PESOS_" + mode + "_" + time_budget + "ms_" + compressedReplicaMode + "_Node" + node + "_Energy.log", "PESOS (" + mode + ", t=" + time_budget + "ms, " + extendedReplicaMode + ")" );
                break;
            case PERF :
                plotter.addPlot( "Log/Distributed_Replica_PERF_" + compressedReplicaMode + "_Node" + node + "_Energy.log", "PERF (" + extendedReplicaMode + ")" );
                break;
            case CONS :
                plotter.addPlot( "Log/Distributed_Replica_CONS_" + compressedReplicaMode + "_Node" + node + "_Energy.log", "CONS (" + extendedReplicaMode + ")" );
                break;
            case PEGASUS :
                plotter.addPlot( "Log/Distributed_Replica_PEGASUS_" + time_budget + "ms_" + compressedReplicaMode + "_Node" + node + "_Energy.log", "PEGASUS (t=" + time_budget + "ms, " + extendedReplicaMode + ")" );
                break;
            default : break;
        }
        plotter.setVisible( true );
    }
    
    public static void plotTailLatency( Type type, Mode mode, long time_budget, String compressedReplicaMode, String extendedReplicaMode ) throws IOException
    {
        final int percentile = 95;
        final double interval = TimeUnit.MINUTES.toMicros( 5 );
        
        Plotter plotter = new Plotter( "DISTRIBUTED REPLICA Tail Latency " + percentile + "-th Percentile", 800, 600 );
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
        final String folder = "Results/Latency/DistributedReplica/";
        List<Pair<Double,Double>> percentiles = null;
        switch ( type ) {
            case PESOS :
                percentiles = Utils.getPercentiles( percentile, interval,
                                                    "Log/Distributed_Replica_PESOS_" + mode + "_" + time_budget + "ms_" + compressedReplicaMode + "_Tail_Latency.log",
                                                    folder + "PESOS_" + mode + "_" + time_budget + "ms_" + compressedReplicaMode + "_Tail_Latency_" + percentile + "th_Percentile.txt" );
                plotter.addPlot( percentiles, "PESOS (" + mode + ", " + tau + "=" + time_budget + "ms, " + extendedReplicaMode + ")" );
                break;
            case PERF :
                percentiles = Utils.getPercentiles( percentile, interval,
                                                    "Log/Distributed_Replica_PERF_" + compressedReplicaMode + "_Tail_Latency.log",
                                                    folder + "PERF_" + compressedReplicaMode + "_Tail_Latency_" + percentile + "th_Percentile.txt" );
                plotter.addPlot( percentiles, "PERF (" + extendedReplicaMode + ")" );
                break;
            case CONS :
                percentiles = Utils.getPercentiles( percentile, interval,
                                                    "Log/Distributed_Replica_CONS_" + compressedReplicaMode + "_Tail_Latency.log",
                                                    folder + "CONS_" + compressedReplicaMode + "_Tail_Latency_" + percentile + "th_Percentile.txt" );
                plotter.addPlot( percentiles, "CONS (" + extendedReplicaMode + ")" );
                break;
            case PEGASUS :
                percentiles = Utils.getPercentiles( percentile, interval,
                                                    "Log/Distributed_Replica_PEGASUS_" + time_budget + "ms_" + compressedReplicaMode + "_Tail_Latency.log",
                                                    folder + "PEGASUS_" + time_budget + "ms_" + compressedReplicaMode + "_Tail_Latency_" + percentile + "th_Percentile.txt" );
                plotter.addPlot( percentiles, "PEGASUS (" + tau + "=" + time_budget + "ms, " + extendedReplicaMode + ")" );
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
}