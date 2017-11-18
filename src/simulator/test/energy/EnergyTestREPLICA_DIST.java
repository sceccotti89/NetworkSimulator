
package simulator.test.energy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import simulator.core.Agent;
import simulator.core.Device;
import simulator.core.Device.Sampler.Sampling;
import simulator.core.Simulator;
import simulator.core.Task;
import simulator.events.Event;
import simulator.events.EventHandler;
import simulator.events.Packet;
import simulator.events.generator.CBRGenerator;
import simulator.events.generator.EventGenerator;
import simulator.events.impl.RequestEvent;
import simulator.events.impl.ResponseEvent;
import simulator.graphics.plotter.Plotter;
import simulator.graphics.plotter.Plotter.Axis;
import simulator.test.energy.CPUModel.CONSmodel;
import simulator.test.energy.CPUModel.Mode;
import simulator.test.energy.CPUModel.PEGASUSmodel;
import simulator.test.energy.CPUModel.PERFmodel;
import simulator.test.energy.CPUModel.PESOSmodel;
import simulator.test.energy.CPUModel.QueryInfo;
import simulator.test.energy.CPUModel.Type;
import simulator.test.energy.EnergyCPU.PESOScore;
import simulator.topology.NetworkTopology;
import simulator.utils.Pair;
import simulator.utils.SizeUnit;
import simulator.utils.Time;
import simulator.utils.Utils;

public class EnergyTestREPLICA_DIST
{
    public static final int NODES = 5;
    private static final int REPLICA_PER_NODE = 2;
    private static final int CPU_CORES = 4;
    
    private static final Packet PACKET = new Packet( 20, SizeUnit.BYTE );
    
    private static boolean PESOS_CONTROLLER = true;
    private static boolean PEGASUS_CONTROLLER = false;
    
    private static final EnergyCPU CPU = new EnergyCPU();
    
    private static List<EnergyCPU> cpus = new ArrayList<>( NODES );
    private static PesosController controller;
    
    
    
    
    
    private static class ClientGenerator extends EventGenerator
    {
        //private static final String QUERY_TRACE = "Models/msn.day2.arrivals.txt";
        private static final String QUERY_TRACE = "Models/test_arrivals.txt";
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
            super( id );
            addEventGenerator( evGenerator );
        }
    }
    
    
    
    
    // TODO Testare in un testing piu' piccolo se tutto funziona a dovere.
    private static class PesosController
    {
        private Map<Long,CpuInfo> cpuInfos;
        private final long timeBudget;
        private final Mode mode;
        
        //private static final double WARNING_DELAY  = 100000d;
        //private static final double CRITICAL_DELAY = 200000d;
        
        // Former value is number of arrived shards, the latter the completed ones.
        private Map<PesosQuery,Pair<Integer,Integer>> openQueries;
        
        private static final double WEIGHT = 0.9;



        public PesosController( long timeBudget, Mode mode )
        {
            cpuInfos = new HashMap<>( NODES );
            this.timeBudget = timeBudget;
            this.mode = mode;
            
            openQueries = new TreeMap<>();
        }
        
        public void connect( Agent to )
        {
            final long index = to.getId() / 2 + to.getId() % 2;
            try { cpuInfos.put( to.getId(), new CpuInfo( timeBudget, mode, to.getId(), index ) ); }
            catch ( IOException e ) {}
        }
        
        public void addQuery( Time time, long nodeID, long coreID, long queryID, long versionId )
        {
            PesosQuery query = new PesosQuery( queryID, versionId );
            
            CpuInfo cpu = cpuInfos.get( nodeID );
            cpu.addQuery( time, coreID, query );
            
            if (!openQueries.containsKey( query )) {
                openQueries.put( query, new Pair<>( 1, 0 ) );
            } else {
                Pair<Integer,Integer> value = openQueries.get( query );
                value.setFirst( value.getFirst() + 1 );
            }
            
            //System.out.println( "\nAGGIUNTA QUERY: " + queryID + " - CPU: " + nodeID + ", CORE: " + coreID );
            
            analyzeSystem( time );
        }
                public void completedQuery( Time time, long nodeID, long coreID )
        {
            CpuInfo cpu = cpuInfos.get( nodeID );
            PesosQuery query = cpu.getCore( coreID ).getFirstQuery();
            //System.out.println( "\nRIMOSSA QUERY: " + query.getId() + " - CPU: " + nodeID + ", CORE: " + coreID );
            
            Pair<Integer,Integer> value = openQueries.get( query );
            value.setSecond( value.getSecond() + 1 );
            if (value.getSecond() == cpuInfos.size()) {
                openQueries.remove( query );
            }
            
            cpu.completedQuery( time, coreID );
            
            analyzeSystem( time );
        }
        
        private void analyzeSystem( Time time )
        {
            // TODO potrei farlo in parallelo nel numero di nodi (se i core totali fossero troppi)!!!
            for (CpuInfo _cpu : cpuInfos.values()) {
                for (CoreInfo _core : _cpu.getCores()) {
                    long extraTime = evalTimeBudget( time.getTimeMicros(), _cpu, _core );
                    long budget = timeBudget + extraTime;
                    if (_core.hasMoreQueries() && _core.checkTimeBudget( budget )) {
                        PESOScore core = (PESOScore) cpus.get( (int) _cpu.getId() - 2 ).getCore( _core.getCoreID() );
                        core.setTimeBudget( time, budget, _core.getFirstQuery().getId() );
                    }
                }
            }
        }
        
        private boolean lastToComplete( PesosQuery query ) {
            return openQueries.get( query ).getSecond() == cpuInfos.size() - 1;
        }
        
        private boolean allShardsArrived( PesosQuery query ) {
            return openQueries.get( query ).getFirst() == cpuInfos.size();
        }
        
        private boolean checkForEmptyCore( long cpuId )
        {
            for (CpuInfo cpu : cpuInfos.values()) {
                if (cpu.getId() != cpuId) {
                    for (CoreInfo core : cpu.getCores()) {
                        if (!core.hasMoreQueries()) {
                            return true;
                        }
                    }
                }
            }
            
            return false;
        }
        
        private long evalTimeBudget( long time, CpuInfo cpu, CoreInfo core )
        {
            long extraBudget = 0;
            final boolean emptyCore = checkForEmptyCore( cpu.getId() );
            
            //System.out.print( "ANALYZING " );
            //core.printQueue();
            for (PesosQuery query : core.getQueries()) {
                if (lastToComplete( query ) ||
                   (!allShardsArrived( query ) && emptyCore)) {
                    return 0;
                }
                
                long queryDelay = 0;
                for (CpuInfo cpu1 : cpuInfos.values()) {
                    if (cpu1.getId() != cpu.getId()) {
                        long extraTime = 0;
                        // Scan all the cores of the cpu.
                        for (CoreInfo core1 : cpu1.coresMap.values()) {
                            //core1.printQueue();
                            // Scan all the enqueued queries.
                            int index = 0;
                            for (PesosQuery query1 : core1.getQueries()) {
                                if (query1.compareTo( query ) == 0) {
                                    if (index == 0) {
                                        return 0;
                                    } else {
                                        break;
                                    }
                                } else {
                                    index++;
                                    extraTime += query1.getResidualServiceTime( time );
                                }
                            }
                        }
                        
                        // Get the maximum time to wait for the current query.
                        queryDelay = Math.max( queryDelay, extraTime );
                    }
                }
                
                // Get the minimum between queries of the same core.
                if (extraBudget == 0) {
                    extraBudget = queryDelay;
                } else {
                    extraBudget = Math.min( extraBudget, queryDelay );
                }
            }
            
            //System.out.println( "EXTRA BUDGET: " + extraBudget );
            
            /*if (extraBudget >= CRITICAL_DELAY) {
                status.setStatus( Status.CRITICAL, extraBudget );
            } else if (extraBudget >= WARNING_DELAY) {
                status.setStatus( Status.WARNING, extraBudget );
            }*/
            
            return (long) (extraBudget * WEIGHT);
        }
        
        /*private static class Status
        {
            private String _status;
            private long extraTime = 0;
            
            protected static final String FINE     = "FINE";
            protected static final String WARNING  = "WARNING";
            protected static final String CRITICAL = "CRITICAL";
            
            private static final double WARNING_COEFFICIENT  = 0.5;
            private static final double CRITICAL_COEFFICIENT = 0.7;
            
            
            public Status( String status, long time ) {
                setStatus( status, time );
            }
            
            public void setStatus( String status, long time )
            {
                _status = status;
                if (status == WARNING) {
                    extraTime = (long) (time * WARNING_COEFFICIENT);
                } else if (status == CRITICAL) {
                    extraTime = (long) (time * CRITICAL_COEFFICIENT);
                }
            }
            
            public String getStatus() {
                return _status;
            }
            
            public long getExtraTime() {
                return extraTime;
            }
            
            @Override
            public String toString() {
                return getStatus() + ", ExtraTime: " + extraTime;
            }
        }*/
        
        private static class CpuInfo extends Device<Object,Object>
        {
            private long _id;
            private Map<Long,CoreInfo> coresMap;
            
            
            public CpuInfo( long timeBudget, Mode mode, long id, long index ) throws IOException
            {
                super( "", "Models/cpu_frequencies.txt" );
                
                PESOSmodel model = new PESOSmodel( timeBudget, mode, "Models/Distributed/Node_" + ((index+1)/2) + "/PESOS/MaxScore/" );
                model.setDevice( this );
                model.loadModel();
                
                _id = id;
                coresMap = new HashMap<>();
                for (long i = 0; i < CPU_CORES; i++) {
                    coresMap.put( i, new CoreInfo( /*_id, */i, model ) );
                }
            }
            
            public void addQuery( Time time, long coreID, PesosQuery query ) {
                coresMap.get( coreID ).addQuery( time, query );
            }
            
            public void completedQuery( Time time, long coreID ) {
                coresMap.get( coreID ).completedQuery( time );
            }
            
            public CoreInfo getCore( long coreID ) {
                return coresMap.get( coreID );
            }
            
            public Collection<CoreInfo> getCores() {
                return coresMap.values();
            }

            public long getId() {
                return _id;
            }
            
            @Override
            public Time timeToCompute( Task task ) {
                return null;
            }
            @Override
            public double getUtilization( Time time ) {
                return 0;
            }
            @Override
            public String getID() {
                return null;
            }
        }
        
        private static class CoreInfo
        {
            private PESOSmodel _model;
            private List<PesosQuery> queue;
            //private long _cpuId;
            private long _coreId;
            
            private long _initTimeBudget;
            private long _timeBudget;
            
            private static final long DELTA_TIME_THRESHOLD = 1;//50000;
            
            public CoreInfo( /*long cpuId, */long coreId, PESOSmodel model ) throws IOException
            {
                //_cpuId = cpuId;
                _coreId = coreId;
                _initTimeBudget = model.getTimeBudget().getTimeMicros() / 1000;
                _timeBudget = model.getTimeBudget().getTimeMicros() / 1000;
                
                queue = new ArrayList<>( 64 );
                _model = model;
            }
            
            public void addQuery( Time time, PesosQuery query )
            {
                QueryInfo q = _model.getQuery( query.getId() );
                query.setInfo( q.getTerms(), q.getPostings() );
                query.predictServiceTime( _model );
                if (!hasMoreQueries()) {
                    query.setStartTime( time );
                }
                queue.add( query );
            }
            
            public void completedQuery( Time time )
            {
                queue.remove( 0 );
                if (hasMoreQueries()) {
                    queue.get( 0 ).setStartTime( time );
                }
                _timeBudget = _initTimeBudget;
            }
            
            public List<PesosQuery> getQueries() {
                return queue;
            }
            
            /*public void printQueue()
            {
                System.out.print( "CPU: " + _cpuId + ", CORE: " + _coreId + " = [" );
                int index = 0;
                for (PesosQuery query : queue) {
                    if (index < queue.size() - 1) {
                        System.out.print( "ID: " + query.getId() + ", " );
                    } else {
                        System.out.print( "ID: " + query.getId() );
                    }
                }
                System.out.println( "]" );
            }*/
            
            public PesosQuery getFirstQuery() {
                return queue.get( 0 );
            }
            
            public boolean hasMoreQueries() {
                return !queue.isEmpty();
            }
            
            public boolean checkTimeBudget( long timeBudget )
            {
                if (Math.abs( timeBudget - _timeBudget ) >= DELTA_TIME_THRESHOLD) {
                    _timeBudget = timeBudget;
                    return true;
                } else {
                    return false;
                }
            }
            
            public long getCoreID() {
                return _coreId;
            }
        }
        
        private static class PesosQuery implements Comparable<PesosQuery>
        {
            private long _id;
            private long _versionId;
            private int _terms;
            private int _postings;
            private long _serviceTime;
            private long _startTime = -1;
            
            public PesosQuery( long id, long versionId )
            {
                _id = id;
                _versionId = versionId;
            }
            
            public void setInfo( int terms, int postings )
            {
                _terms = terms;
                _postings = postings;
            }
            
            public void predictServiceTime( PESOSmodel model )
            {
                // TODO testare questa soluzione ora che ho sistemato i postings: testare per tutti e 4 i casi.
                int ppcRMSE = model.getRMSE( _terms );
                _serviceTime = model.predictServiceTimeAtMaxFrequency( _terms, _postings + ppcRMSE );
                //System.out.println( "QUERY: " + _id + ", PREDICTED: " + _serviceTime + ", REAL: " + model.getQuery( _id ).getTime( 3500000 )  );
            }
            
            /**
             * Returns the residual predicted service time of the query.
             * 
             * @param time    time of evaluation.
            */
            public long getResidualServiceTime( long time )
            {
                if (_startTime == -1) {
                    return _serviceTime;
                } else {
                    long elapsedTime = time - _startTime;
                    return Math.max( 0, _serviceTime - elapsedTime );
                }
            }
            
            public void setStartTime( Time time ) {
                _startTime = time.getTimeMicros();
            }
            
            private Long getVersionId() {
                return _versionId;
            }
            
            public Long getId() {
                return _id;
            }

            @Override
            public int compareTo( PesosQuery query )
            {
                int compare = query.getId().compareTo( getId() );
                if (compare == 0) {
                    return query.getVersionId().compareTo( getVersionId() );
                } else {
                    return compare;
                }
            }
        }
    }
    
    /*private static class PESOSmessage
    {
        private long _coreID;
        private long _queryID;
        private long _timeBudget;
        
        public PESOSmessage( long coreID, long queryID, long timeBudget )
        {
            _coreID = coreID;
            _queryID = queryID;
            _timeBudget = timeBudget;
        }
        
        public long getCoreID() {
            return _coreID;
        }
        
        public long getQueryID() {
            return _queryID;
        }
        
        public long getTimeBudget() {
            return _timeBudget;
        }
    }*/
    
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
            System.out.println( "DESTINATION: " + destination );
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
        
        public BrokerAgent( long id, long target, EventGenerator evGenerator ) throws IOException
        {
            super( id );
            addEventGenerator( evGenerator );
            addEventHandler( this );
            
            writer = new PrintWriter( "Results/Distributed_Latencies.txt", "UTF-8" );
            queries = new ArrayList<>( 1 << 10 );
        }
        
        @Override
        public Time handle( Event e, EventType type )
        {
            if (type == EventType.RECEIVED) {
                long queryId = e.getPacket().getContent( Global.QUERY_ID );
                if (e.getSource().getId() == 0) {
                    queries.add( new QueryLatency( queryId, e.getTime() ) );
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
            for (Agent agent : _evtGenerators.get( 0 ).getDestinations()) {
                utilization += agent.getNodeUtilization( time );
            }
            return utilization;
        }
        
        @Override
        public void shutdown()
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
            super( id );
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
        public void shutdown()
        {
            EnergyCPU cpu = getDevice( CPU );
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
        
        public SwitchGenerator( Time duration, Packet reqPacket, Packet resPacket )
        {
            super( duration, Time.ZERO, reqPacket, resPacket );
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
            SwitchAgent switchAgent = (SwitchAgent) getAgent();
            //System.out.println( "REPLICAS: " + switchAgent.getCurrentReplicas() );
            nextReplica = (nextReplica + 1) % switchAgent.getCurrentReplicas();
            return nextReplica;
        }
    }
    
    // TODO questo metodo prevede che le query vengano mantenute qui e a turno ogni replica le richiede.
    private static class SwitchAgent extends Agent implements EventHandler
    {
        private List<QueryInfo> queries;
        private CPUModel _model;
        
        private int currentReplicas;
        
        private List<Double> meanArrivalTime;
        private List<Double> meanCompletionTime;
        
        private List<Integer> allReplicas;
        private ReplicatedGraph graph;
        
        private int slotIndex;
        
        public static final int SEASONAL_ESTIMATOR            = 0;
        public static final int SEASONAL_ESTIMATOR_WITH_DRIFT = 1;
        private int estimatorType;
        
        private Integer arrivals;
        private List<Integer> currentArrivals;
        
        // Watt dissipated by the associated CPU.
        private static final double Pstandby =  2;
        private static final double Pon      = 84;
        
        // The Lambda value used to balance the equation.
        private static final double LAMBDA = 0.75;
        
        private static final double ALPHA = -0.01;
        private int latency_normalization;
        
        
        
        public SwitchAgent( long id, int estimatorType, int latencyNormalization,
                            CPUModel model, EventGenerator evGenerator ) throws IOException
        {
            super( id );
            addEventGenerator( evGenerator );
            addEventHandler( this );
            
            queries = new ArrayList<>();
            _model = model;
            
            this.estimatorType = estimatorType;
            this.latency_normalization = latencyNormalization;
            slotIndex = -1;
            loadMeanArrivalTime();
            loadQueryPerSlot();
            
            if (estimatorType == SEASONAL_ESTIMATOR) {
                // Compute the minimum path among all the possibles.
                graph = new ReplicatedGraph( meanCompletionTime.size() );
                graph.addNode( 0, 0 );
                allReplicas = new ArrayList<>( 128 );
                createGraph( 0, 0, 0 );
            } else {
                arrivals = 0;
                currentArrivals = new ArrayList<>( 2 );
            }
        }
        
        private void loadMeanArrivalTime() throws IOException
        {
            meanCompletionTime = new ArrayList<>( 128 );
            BufferedReader reader = new BufferedReader( new FileReader( "Results/MeanCompletionTime.log" ) );
            String line = null;
            while ((line = reader.readLine()) != null) {
                double time = Double.parseDouble( line.split( " " )[1] );
                meanCompletionTime.add( time );
            }
            reader.close();
        }
        
        private void loadQueryPerSlot() throws IOException
        {
            meanArrivalTime = new ArrayList<>( 128 );
            BufferedReader reader = new BufferedReader( new FileReader( "Results/QueryPerTimeSlot.log" ) );
            String line = null;
            while ((line = reader.readLine()) != null) {
                double time = Double.parseDouble( line.split( " " )[1] );
                meanArrivalTime.add( time );
            }
            reader.close();
        }
        
        @Override
        public void addEventOnQueue( Event e )
        {
            if (estimatorType == SEASONAL_ESTIMATOR_WITH_DRIFT) {
                arrivals++;
            }
            
            Packet p = e.getPacket();
            if (p.hasContent( Global.QUERY_ID )) {
                QueryInfo query = _model.getQuery( p.getContent( Global.QUERY_ID ));
                queries.add( query );
            }
            
            super.addEventOnQueue( e );
        }
        
        public int getCurrentReplicas() {
            return currentReplicas;
        }

        @Override
        public Time handle( Event e, EventType type )
        {
            Packet p = e.getPacket();
            if (p.hasContent( Global.SWITCH_TIME_SLOT )) {
                if (estimatorType == SEASONAL_ESTIMATOR_WITH_DRIFT) {
                    if (slotIndex >= 0) {
                        currentArrivals.add( arrivals );
                        if (currentArrivals.size() > 2) {
                            currentArrivals.remove( 0 );
                        }
                        arrivals = 0;
                    }
                    
                    // Compute the number of nodes used to compute all the current queries.
                    currentReplicas = graphSearch( ++slotIndex );
                }
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
        private int graphSearch( int slotIndex )
        {
            // Select the results from a previous day.
            double completionExtimation = meanCompletionTime.get( slotIndex );
            double arrivalExtimation    = meanArrivalTime.get( slotIndex );
            if (currentArrivals.size() == 2) {
                arrivalExtimation += (currentArrivals.get( 1 ) - currentArrivals.get( 0 ));
            }
            
            double minWeight = Double.MAX_VALUE;
            int replicas = 0;
            for (int nodes = 1; nodes <= REPLICA_PER_NODE; nodes++) {
                double power   = getPowerCost( nodes );
                double latency = getLatencyCost( queries.size(), arrivalExtimation, completionExtimation, nodes );
                double weight  = LAMBDA * power + (1 - LAMBDA) * latency;
                if (weight < minWeight) {
                    minWeight = weight;
                    replicas = nodes;
                }
            }
            
            return replicas;
        }
        
        private void createGraph( int nodeIndex, int queries, int slotIndex )
        {
            if (slotIndex == meanCompletionTime.size() - 2) {
                // Create the last node.
                final int index = REPLICA_PER_NODE * meanCompletionTime.size() + 2;
                graph.addNode( index, 0 );
                graph.addLink( nodeIndex, index, queries );
                return;
            }
            
            double completionExtimation = meanCompletionTime.get( slotIndex );
            double arrivalExtimation    = meanArrivalTime.get( slotIndex );
            
            // FIXME BUG: se creo cosi' i nodi rischio di creare duplicati: 
            //            il numero di query presenti nel time slot non posso salvarlo nel nodo
            final long Ts = SwitchTimeSlotGenerator.TIME_SLOT.getTimeMicros();
            for (int nodes = 1; nodes <= REPLICA_PER_NODE; nodes++) {
                // Predict the queries for the suqsequent node.
                int nextQueries = (int) (queries - ((nodes * Ts) / completionExtimation) + arrivalExtimation);
                final int index = slotIndex * REPLICA_PER_NODE + nodes;
                graph.addNode( index, nextQueries );
                double power   = getPowerCost( nodes );
                double latency = getLatencyCost( queries, arrivalExtimation, completionExtimation, nodes );
                double weight  = LAMBDA * power + (1 - LAMBDA) * latency;
                graph.addLink( nodeIndex, index, weight );
                
                createGraph( index, nextQueries, slotIndex + 1 );
            }
        }
        
        private double getPowerCost( final int nodes ) {
            return (Pon * nodes + Pstandby * (REPLICA_PER_NODE - nodes)) / (Pon * REPLICA_PER_NODE);
        }
        
        private double getLatencyCost( int queuedQueries, double newQueries, double meanCompletiontime, int nodes )
        {
            double Tk = ((queuedQueries + newQueries) * meanCompletiontime) / nodes;
            final long Ts = SwitchTimeSlotGenerator.TIME_SLOT.getTimeMicros();
            switch (latency_normalization) {
                case( 1 ) : return 1 - Math.exp( ALPHA * Tk );
                case( 2 ) : return (Tk <= Ts) ? 0 : 1;
                case( 3 ) : return (Tk <= Ts) ? 0 : 1 - Math.exp( ALPHA * (Tk - Ts) );
            }
            return 0;
        }
    }
    
    
    
    
    
    
    
    public static void main( String[] args ) throws Exception
    {
        Utils.VERBOSE      = false;
        PESOS_CONTROLLER   = false;
        PEGASUS_CONTROLLER = false;
        
        testNetwork( Type.PESOS, Mode.TIME_CONSERVATIVE,  500 );
        //testNetwork( Type.PESOS, Mode.TIME_CONSERVATIVE, 1000 );
        //testNetwork( Type.PESOS, Mode.ENERGY_CONSERVATIVE,  500 );
        //testNetwork( Type.PESOS, Mode.ENERGY_CONSERVATIVE, 1000 );
        
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
        
        //NetworkTopology net = new NetworkTopology( "Topology/Animation/Topology_distributed_multiCore.json" );
        NetworkTopology net = new NetworkTopology( "Topology/Topology_distributed_replica_multiCore.json" );
        //net.setTrackingEvent( "Results/distr_multi_core.txt" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        // Create client.
        EventGenerator generator = new ClientGenerator( PACKET, PACKET );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        // Create query broker.
        EventGenerator brokerGen = new BrokerGenerator( duration );
        Agent brokerAgent = new BrokerAgent( 1, timeBudget * 1000, brokerGen );
        net.addAgent( brokerAgent );
        client.getEventGenerator( 0 ).connect( brokerAgent );
        
        // Create PESOS controller.
        if (type == Type.PESOS && PESOS_CONTROLLER) {
            controller = new PesosController( timeBudget * 1000, mode );
        }
        
        CPUModel model = getModel( type, mode, timeBudget, 1 );
        final String modelType = model.getModelType( true );
        Plotter plotter = new Plotter( "DISTRIBUTED REPLICA MULTI_CORE - " + modelType, 800, 600 );
        
        final Time samplingTime = new Time( 5, TimeUnit.MINUTES );
        for (int i = 0; i < NODES; i++) {
            CPUModel p_model = loadModel( type, mode, timeBudget, i+1 );
            
            // Create the switch associated to the REPLICA nodes.
            EventGenerator switchGen = new SwitchGenerator( duration, PACKET, PACKET );
            Agent switchNode = new SwitchAgent( 2 + i * REPLICA_PER_NODE + i,
                                                SwitchAgent.SEASONAL_ESTIMATOR_WITH_DRIFT, 1,
                                                p_model, switchGen );
            net.addAgent( switchNode );
            brokerGen.connect( switchNode );
            
            // Add the time slot generator.
            EventGenerator timeSlotGenerator = new SwitchTimeSlotGenerator( duration );
            switchNode.addEventGenerator( timeSlotGenerator );
            timeSlotGenerator.connect( switchNode );
            
            for (int j = 0; j < REPLICA_PER_NODE; j++) {
                final long nodeId = (i * REPLICA_PER_NODE + j + 1);
                EnergyCPU cpu = new EnergyCPU( "Intel i7-4770K", CPU_CORES, 1, "Models/cpu_frequencies.txt" );
                cpu.addSampler( Global.ENERGY_SAMPLING, samplingTime, Sampling.CUMULATIVE, "Log/" + modelType + "_Energy.log" );
                cpu.addSampler( Global.IDLE_ENERGY_SAMPLING, samplingTime, Sampling.CUMULATIVE, null );
                cpu.addSampler( Global.TAIL_LATENCY_SAMPLING, null, null, "Log/" + modelType + "_Node" + nodeId + "_Tail_Latency.log" );
                cpus.add( cpu );
                
                // Add the model to the corresponding cpu.
                p_model.loadModel();
                cpu.setModel( p_model );
                
                EventGenerator sink = new MulticoreGenerator( duration );
                final long id = i * (REPLICA_PER_NODE + 1) + 3 + j;
                Agent agentCore = new MulticoreAgent( id, sink );
                agentCore.addDevice( cpu );
                net.addAgent( agentCore );
                
                if (type == Type.CONS) {
                    EventGenerator evtGen = new ServerConsGenerator( duration );
                    evtGen.connect( agentCore );
                    agentCore.addEventGenerator( evtGen );
                }
                
                plotter.addPlot( cpu.getSampledValues( Global.ENERGY_SAMPLING ), "Node " + nodeId + " " + p_model.getModelType( false ) );
                
                switchGen.connect( agentCore );
                if (PESOS_CONTROLLER) {
                    controller.connect( agentCore );
                }
            }
        }
        
        plotter.setAxisName( "Time (h)", "Energy (J)" );
        plotter.setTicks( Axis.Y, 10 );
        plotter.setTicks( Axis.X, 24, 2 );
        plotter.setRange( Axis.Y, 0, 4300 );
        plotter.setScaleX( 60d * 60d * 1000d * 1000d );
        plotter.setVisible( true );
        
        sim.start( duration, false );
        //sim.start( new Time( 53100, TimeUnit.MICROSECONDS ), false );
        sim.close();
        
        for (int i = 0; i < NODES * REPLICA_PER_NODE; i++) {
            EnergyCPU cpu = cpus.get( i );
            double energy = cpu.getResultSampled( Global.ENERGY_SAMPLING );
            System.out.println( "CPU: " + i + ", Energy: " + energy );
        }
        
        // Show the animation.
        /*AnimationNetwork an = new AnimationNetwork( 800, 600, modelType );
        an.loadSimulation( "Topology/Animation/Topology_distributed_multiCore.json", "./Results/distr_multi_core.txt" );
        an.setTargetFrameRate( 90 );
        an.setForceExit( false );
        an.start();*/
    }
}
