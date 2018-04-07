
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
import simulator.events.EventGenerator;
import simulator.events.EventHandler;
import simulator.events.Packet;
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
import simulator.test.energy.PEGASUS.PEGASUSmessage;
import simulator.test.energy.PESOScontroller.PESOSmessage;
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
    
    private static boolean PESOS_CONTROLLER = false;
    private static boolean PEGASUS_CONTROLLER = false;
    private static boolean SWITCH_OFF_MACHINES = false;
    
    private static Scheduler<Iterable<Core>, Long, QueryInfo> _scheduler;
    
    private static List<CPU> cpus = new ArrayList<>( NODES );
    
    private static int arrivalEstimator;
    private static int latencyNormalization;
    private static double lambda;
    
    private static boolean REPLICA_SWITCH = false;
    private static boolean JOB_STEALING = false;
    
    private static Map<String,Double> testResults;
    
    
    
    
    
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
        public void receivedMessage( Event e ) {
            // Empty body.
        }
        
        @Override
        public void notifyEvent( Event e )
        {
            // Send the packet to the broker.
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
            
            writer = new PrintWriter( "Log/Distributed_Replica_" + model + "_Tail_Latency.log", "UTF-8" );
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
                
                // Send the information of the incoming query to the switch.
                PESOSmessage message = new PESOSmessage( coreId, query.getId(), 0 );
                message.setVersionId( versionId );
                Packet packet = new Packet( 20, SizeUnit.BYTE );
                packet.addContent( Global.PESOS_CONTROLLER_ADD, message );
                Agent switchAgent = getConnectedAgents().get( 0 );
                sendMessage( switchAgent, packet, true );
            }
            
            Time time = cpu.timeToCompute( null );
            if (time != null) {
                // Prepare the message to the switch.
                Agent switchAgent = getConnectedAgents().get( 0 );
                sendMessage( cpu.getCore( coreId ).getTime(), switchAgent, p, false );
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
                            // Prepare the response message for the switch.
                            Agent switchAgent = getConnectedAgents().get( 0 );
                            sendMessage( core.getTime(), switchAgent, msg, false );
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
    
    
    
    private static class SwitchTimeSlotGenerator extends EventGenerator
    {
        public static final Time TIME_SLOT = new Time( 15, TimeUnit.MINUTES );
        
        public SwitchTimeSlotGenerator( Time duration ) {
            super( duration.clone().subTime( TIME_SLOT ), TIME_SLOT, EventGenerator.AFTER_CREATION );
        }
        
        @Override
        public Event createEvent()
        {
            Event e = super.createEvent();
            Packet packet = new Packet( 1, SizeUnit.BIT );
            packet.addContent( Global.SWITCH_TIME_SLOT, "" );
            e.setPacket( packet );
            return e;
        }
    }
    
    public static class SwitchAgent extends Agent
    {
        private int queries;
        
        protected int currentReplicas = 1;
        
        private List<Double> meanArrivalQueries;
        private List<Double> meanCompletionTime;
        
        private Map<Long,Long> tieSelected;
        
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
        private static final double Pstandby =  0.9;
        private static final double Pon      = 63.7;
        
        // The Lambda value used to balance the equation (lower for latency, higher for power).
        private final double LAMBDA;
        
        // Parameters used to normalize the latency.
        private static double ALPHA = -0.01;
        private int latency_normalization;
        
        //private static final Time WAKE_UP_TIME = new Time( 200, TimeUnit.SECONDS );
        
        
        
        public SwitchAgent( long id, int estimatorType,
                            int latencyNormalization, double lambda ) throws IOException
        {
            super( NetworkAgent.FULL_DUPLEX, NetworkLayer.APPLICATION, id );
            
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
            
            tieSelected = new HashMap<>();
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
        public void connect( Agent destination )
        {
            tieSelected.put( destination.getId(), 0L );
            super.connect( destination );
        }
        
        @Override
        public void receivedMessage( Event e )
        {
            long sourceId = e.getSource().getId();
            if (sourceId != getId()) {
                if (sourceId == 1) {
                    // From broker: send a message to the first available replica.
                    Agent destination = selectDestination( e.getTime() );
                    sendMessage( destination, e.getPacket(), true );
                } else {
                    // From replica: send-back a message to the broker.
                    sendMessage( getConnectedAgent( 1 ), e.getPacket(), false );
                }
            }
            
            if (estimatorType == SEASONAL_ESTIMATOR_WITH_DRIFT) {
                if (sourceId != getId()) {
                    if (sourceId != 1) {
                        // From replica.
                        queries--;
                    } else {
                        // From broker.
                        arrivals++;
                        queries++;
                    }
                }
            }
        }
        
        private Agent selectDestination( Time time )
        {
            // Bounded Round-Robin.
            int maxReplicas = getCurrentReplicas();
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
            Agent nextReplica = null;
            double minUtilization = Double.MAX_VALUE;
            long tiedSelection = Long.MAX_VALUE;
            boolean tieSituation = false;
            int replicaVisited = 0;
            //String value = "";
            for (Agent dest : getConnectedAgents()) {
                if (dest.getId() > 1) {
                    double utilization = dest.getNodeUtilization( time );
                    //value += "NODE: " + (index) + ": " + utilization + ", ";
                    if (utilization < minUtilization) {
                        nextReplica = dest;
                        minUtilization = utilization;
                        tieSituation = false;
                    } else if (utilization == minUtilization) {
                        if (tieSelected.get( dest.getId() ) < tiedSelection) {
                            nextReplica = dest;
                            minUtilization = utilization;
                            tiedSelection = tieSelected.get( dest.getId() );
                        }
                        tieSituation = true;
                    }
                    
                    replicaVisited++;
                }
                
                if (replicaVisited == maxReplicas) {
                    break;
                }
            }
            
            if (tieSituation) {
                tieSelected.put( nextReplica.getId(), tiedSelection + 1 );
            }
            
            //System.out.println( value );
            
            return nextReplica;
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
            // We add +1 to ignore the client node in the first position.
            
            // Turn-on the current needed replicas.
            for (int i = 0; i < currentReplicas; i++) {
                getConnectedAgents().get( i + 1 ).getDevice( EnergyCPU.class ).setState( time, State.RUNNING );
            }
            
            if (SWITCH_OFF_MACHINES) {
                for (int i = currentReplicas; i < REPLICAS_PER_NODE; i++) {
                    getConnectedAgents().get( i + 1 ).getDevice( EnergyCPU.class ).setState( time, State.POWER_OFF );
                }
            }
        }
        
        @Override
        public void notifyEvent( Event e )
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
    
    private static class ReplicaSwitch extends SwitchAgent
    {
        public static double targetConsumption;
        public static double targetRo;
        
        private int index = -1;
        private List<Double> nodeUtilization;
        
        //private static int orderedIndex = -1;
        //private List<Double> orderedNodeUtilization;
        
        
        
        public ReplicaSwitch( long id, /*double _targetRo, double targetConsumption,*/
                              String model ) throws Exception
        {
            super( id, 1, 1, 0.25 ); // Dummy parameters.
            
            //targetRo = _targetRo;
            
            nodeUtilization = new ArrayList<>();
            InputStream loader = ResourceLoader.getResourceAsStream( "Results/QueueSize_" + model + ".log" );
            BufferedReader reader = new BufferedReader( new InputStreamReader( loader ) );
            String line = null;
            while ((line = reader.readLine()) != null) {
                double ro = Double.parseDouble( line.split( " " )[1] );
                nodeUtilization.add( ro );
            }
            
            /*orderedNodeUtilization = new ArrayList<>( nodeUtilization );
            Collections.sort( orderedNodeUtilization );
            Collections.reverse( orderedNodeUtilization );
            
            orderedIndex = (orderedIndex + 1) % nodeUtilization.size();
            targetRo = orderedNodeUtilization.get( orderedIndex );*/
        }
        
        @Override
        public void notifyEvent( Event e )
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
                //System.out.println( "RO: " + nodeUtilization.get( index )/* + ", TARGET: " + targetRo*/ + ", REPLICAS: " + currentReplicas );
                setNodesState( e.getTime() );
            }
        }
        
        public String getMode( boolean compressed ) {
            return "T=" + targetRo;
        }
    }
    
    
    
    
    
    
    
    public static void main( String[] args ) throws Exception
    {
        Utils.VERBOSE = false;
        
        JOB_STEALING        = false;
        SWITCH_OFF_MACHINES = true;
        
        //System.setProperty( "showGUI", "false" );
        if (System.getProperty( "showGUI" ) != null) {
            Global.showGUI = System.getProperty( "showGUI" ).equalsIgnoreCase( "true" );
        }
        
        testResults = new LinkedHashMap<>();
        
        arrivalEstimator     = SwitchAgent.SEASONAL_ESTIMATOR;
        latencyNormalization = 2;
        lambda               = 0.5;
        
        _scheduler = new LowestPredictedFrequency();
        //_scheduler = new EarliestCompletionTime();
        
        /*testPaperSwitch( Type.PESOS, Mode.TIME_CONSERVATIVE,  500 );
        testPaperSwitch( Type.PESOS, Mode.TIME_CONSERVATIVE, 1000 );
        testPaperSwitch( Type.PESOS, Mode.ENERGY_CONSERVATIVE,  500 );
        testPaperSwitch( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 1000 );*/
        
        testNetwork( Type.PESOS, Mode.TIME_CONSERVATIVE,  500 );
        //testNetwork( Type.PESOS, Mode.TIME_CONSERVATIVE, 1000 );
        //testNetwork( Type.PESOS, Mode.ENERGY_CONSERVATIVE,  500 );
        //testNetwork( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 1000 );
        
        //testMySwitch( Type.PESOS, Mode.TIME_CONSERVATIVE, 500, 570013, new EarliestCompletionTime() );
        //testMySwitch( Type.PESOS, Mode.TIME_CONSERVATIVE, 1000, 420947, new LowestPredictedFrequency() );
        //testMySwitch( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 500, 491949, new LowestPredictedFrequency() );
        //testMySwitch( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 1000, 381325, new LowestPredictedFrequency() );
        
        /*arrivalEstimator = SwitchAgent.SEASONAL_ESTIMATOR;
        for (int i = 1; i <= 3; i++) {
            lambda = 0.25d * i;
            for (latencyNormalization = 1; latencyNormalization <= 3; latencyNormalization++) {
                testNetwork( Type.PESOS, Mode.TIME_CONSERVATIVE, 500 );
            }
        }
        
        arrivalEstimator = SwitchAgent.SEASONAL_ESTIMATOR_WITH_DRIFT;
        for (int i = 1; i <= 3; i++) {
            lambda = 0.25d * i;
            for (latencyNormalization = 1; latencyNormalization <= 3; latencyNormalization++) {
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
    
    protected static void testMySwitch( Type type, Mode mode, long timeBudget, double targetEnergy,
                                        Scheduler<Iterable<Core>, Long, QueryInfo> scheduler ) throws Exception
    {
        REPLICA_SWITCH      = true;
        JOB_STEALING        = true;
        SWITCH_OFF_MACHINES = true;
        
        _scheduler = scheduler;
        
        ReplicaSwitch.targetConsumption = targetEnergy;
        
        List<Double> nodeUtilization = new ArrayList<>();
        CPUModel model = getModel( type, mode, timeBudget, 1 );
        
        InputStream loader = ResourceLoader.getResourceAsStream( "Results/QueueSize_" + model.getModelType( false ) + ".log" );
        BufferedReader reader = new BufferedReader( new InputStreamReader( loader ) );
        String line = null;
        while ((line = reader.readLine()) != null) {
            double ro = Double.parseDouble( line.split( " " )[1] );
            nodeUtilization.add( ro );
        }
        Collections.sort( nodeUtilization );
        Collections.reverse( nodeUtilization );
        
        double minEnergy = targetEnergy;
        for(int i = 0; i < nodeUtilization.size(); i++) {
            ReplicaSwitch.targetRo = nodeUtilization.get( i );
            System.out.println( "TARGET_RO: " + nodeUtilization.get( i ) );
            double energy = testNetwork( type, mode, timeBudget );
            System.out.println( "MIN_TARGET: " + minEnergy + ", CURRENT: " + energy );
            if (energy >= minEnergy) {
                break;
            } else {
                minEnergy = energy;
            }
        }
        
        Utils.LOGGER.info( model.getModelType( false ) + ":: TARGET_RO: " + ReplicaSwitch.targetRo + " => " + minEnergy );
    }
    
    protected static void testPaperSwitch( Type type, Mode mode, long timeBudget ) throws Exception
    {
        arrivalEstimator = SwitchAgent.SEASONAL_ESTIMATOR;
        for (int i = 1; i <= 3; i++) {
            lambda = 0.25d * i;
            for (latencyNormalization = 1; latencyNormalization <= 3; latencyNormalization++) {
                testNetwork( type, mode, timeBudget );
            }
        }
        
        arrivalEstimator = SwitchAgent.SEASONAL_ESTIMATOR_WITH_DRIFT;
        for (int i = 1; i <= 3; i++) {
            lambda = 0.25d * i;
            for (latencyNormalization = 1; latencyNormalization <= 3; latencyNormalization++) {
                testNetwork( type, mode, timeBudget );
            }
        }
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
    
    public static double testNetwork( Type type, Mode mode, long timeBudget ) throws Exception
    {
        final Time duration = new Time( 24, TimeUnit.HOURS );
        PEGASUS_CONTROLLER = (type == Type.PEGASUS);
        
        NetworkTopology net = new NetworkTopology();
        Simulator sim = new Simulator( net );
        
        // Create client.
        EventGenerator generator = new ClientGenerator();
        Agent client = new ClientAgent( 0, generator );
        net.addNode( 0, "Client", 0 );
        net.addAgent( client );
        
        // Create broker.
        net.addNode( 1, "Broker", 0 );
        
        CPUModel model = getModel( type, mode, timeBudget, 1 );
        final String modelType = model.getModelType( true );
        Plotter plotter = null;
        if (Global.showGUI) {
            plotter = new Plotter( "", 800, 600 );
        }
        
        cpus.clear();
        
        String compressedReplicaMode = null;
        String extendedReplicaMode   = null;
        
        Agent broker = null;
        
        final Time samplingTime = new Time( 5, TimeUnit.MINUTES );
        for (int i = 0; i < NODES; i++) {
            CPUModel p_model = loadModel( type, mode, timeBudget, i+1 );
            p_model.loadModel();
            
            // Create the switch associated to the REPLICA nodes.
            final long switchId = 2 + i * REPLICAS_PER_NODE + i;
            SwitchAgent switchNode;
            if (REPLICA_SWITCH) {
                switchNode = new ReplicaSwitch( switchId, model.getModelType( false ) );
            } else {
                switchNode = new SwitchAgent( switchId, arrivalEstimator, latencyNormalization, lambda );
            }
            net.addNode( switchId, "Switch_" + (i+1), 0 );
            // From broker (1) to switch.
            net.addLink( 1, switchId, 1000, 0, NetworkLink.BIDIRECTIONAL );
            net.addAgent( switchNode );
            
            
            // Get the type of simulation.
            if (broker == null) {
                compressedReplicaMode = switchNode.getMode( true );
                extendedReplicaMode   = switchNode.getMode( false );
                broker = new BrokerAgent( 1, timeBudget * 1000, type, mode, modelType + "_" + compressedReplicaMode );
            }
            broker.connect( switchNode );
            
            // Add the time slot generator.
            EventGenerator timeSlotGenerator = new SwitchTimeSlotGenerator( duration );
            switchNode.addEventGenerator( timeSlotGenerator );
            
            for (int j = 0; j < REPLICAS_PER_NODE; j++) {
                final long nodeId = (i * REPLICAS_PER_NODE + j + 1);
                CPU cpu = new EnergyCPU( "Models/cpu_spec.json", getCoreClass( type ) );
                cpu.enableJobStealing( JOB_STEALING );
                if (_scheduler != null) {
                    cpu.setScheduler( _scheduler );
                }
                cpus.add( cpu );
                
                // Add the model to the corresponding CPU.
                cpu.setModel( p_model );
                
                final long id = i * (REPLICAS_PER_NODE + 1) + 3 + j;
                Agent agentCore = new MulticoreAgent( id );
                agentCore.addDevice( cpu );
                net.addNode( id, "node" + (i/REPLICAS_PER_NODE+1) + "_" + (i%REPLICAS_PER_NODE), 0 );
                // From switch to core (id).
                net.addLink( switchId, id, 1000, 0, NetworkLink.BIDIRECTIONAL );
                net.addAgent( agentCore );
                switchNode.connect( agentCore );
                
                agentCore.addSampler( Global.ENERGY_SAMPLING, new Sampler( samplingTime, "Log/Distributed_Replica_" + modelType + "_" + compressedReplicaMode + "_Node" + nodeId + "_Energy.log", Sampling.CUMULATIVE ) );
                agentCore.addSampler( Global.IDLE_ENERGY_SAMPLING, new Sampler( samplingTime, null, Sampling.CUMULATIVE ) );
                
                if (Global.showGUI) {
                    plotter.addPlot( agentCore.getSampler( Global.ENERGY_SAMPLING ).getValues(), "Node " + nodeId );
                }
            }
        }
        
        // From client (0) to broker (1).
        net.addLink( 0, 1, 1000, 0, NetworkLink.BIDIRECTIONAL );
        net.addAgent( broker );
        client.connect( broker );
        
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
        
        return totalEnergy;
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