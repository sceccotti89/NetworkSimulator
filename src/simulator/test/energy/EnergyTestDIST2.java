
package simulator.test.energy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.newdawn.slick.util.ResourceLoader;

import simulator.core.Agent;
import simulator.core.Device;
import simulator.core.Device.Sampler.Sampling;
import simulator.core.Simulator;
import simulator.core.Task;
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
import simulator.test.energy.EnergyCPU.PESOScore;
import simulator.topology.NetworkTopology;
import simulator.utils.Pair;
import simulator.utils.SizeUnit;
import simulator.utils.Time;
import simulator.utils.Utils;

public class EnergyTestDIST2
{
    private static final int NODES = 5;
    private static final int CPU_CORES = 4;
    
    private static final Packet PACKET = new Packet( 20, SizeUnit.BYTE );
    
    private static boolean PESOS_CONTROLLER = true;
    
    private static List<EnergyCPU> cpus;
    private static PesosController controller;
    
    
    
    
    
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
    
    
    
    
    private static class PesosController
    {
        private Map<Long,CpuInfo> cpuInfos;
        private final long timeBudget;
        private final Mode mode;
        
        private static final double WARNING_DELAY  = 100000d;
        private static final double CRITICAL_DELAY = 200000d;
        
        // Former value is number of arrived shards, the latter the completed ones.
        private Map<PesosQuery,Pair<Integer,Integer>> openQueries;
        
        
        public PesosController( final long timeBudget, final Mode mode )
        {
            cpuInfos = new HashMap<>( NODES );
            this.timeBudget = timeBudget;
            this.mode = mode;
            
            openQueries = new TreeMap<>();
        }
        
        public void connect( final Agent to )
        {
            try { cpuInfos.put( to.getId(), new CpuInfo( timeBudget, mode, to.getId(), to.getId() - 1 ) ); }
            catch ( IOException e ) {}
        }
        
        public void addQuery( final Time time, final long nodeID, final long coreID, final long queryID, final long versionId )
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
        
        public void completedQuery( final Time time, final long nodeID, final long coreID )
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
        
        private void analyzeSystem( final Time time )
        {
            // TODO potrei farlo in parallelo nel numero di nodi (se i core totali fossero troppi)!!!
            for (CpuInfo _cpu : cpuInfos.values()) {
                for (CoreInfo _core : _cpu.getCores()) {
                    Status status = evalTimeBudget( time.getTimeMicroseconds(), _cpu, _core );
                    //System.out.println( "STATUS: " + status );
                    long budget = timeBudget + status.getExtraTime();
                    if (_core.hasMoreQueries() && _core.checkTimeBudget( budget )) {
                        PESOScore core = (PESOScore) cpus.get( (int) _cpu.getId() - 2 ).getCore( _core.getCoreID() );
                        core.setTimeBudget( budget, _core.getFirstQuery().getId() );
                    }
                }
            }
        }
        
        private boolean lastToComplete( final long queryID ) {
            return openQueries.get( queryID ).getSecond() == cpuInfos.size() - 1;
        }
        
        private boolean allShardsArrived( final long queryID ) {
            return openQueries.get( queryID ).getFirst() == cpuInfos.size();
        }
        
        private boolean checkForEmptyCore( final long cpuId )
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
        
        private Status evalTimeBudget( final long time, final CpuInfo cpu, final CoreInfo core )
        {
            Status status = new Status( Status.FINE, 0L );
            long extraBudget = 0;
            
            //System.out.print( "ANALYZING " );
            //core.printQueue();
            for (PesosQuery query : core.getQueries()) {
                if (lastToComplete( query.getId() ) ||
                   (!allShardsArrived( query.getId() ) && checkForEmptyCore( cpu.getId() ))) {
                    return status;
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
                                if (query1.getId() == query.getId()) {
                                    if (index == 0) {
                                        return status;
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
                extraBudget = Math.min( extraBudget, queryDelay );
            }
            
            if (extraBudget >= CRITICAL_DELAY) {
                status.setStatus( Status.CRITICAL, extraBudget );
            } else if (extraBudget >= WARNING_DELAY) {
                status.setStatus( Status.WARNING, extraBudget );
            }
            
            return status;
        }
        
        private static class Status
        {
            private String _status;
            private long extraTime = 0;
            
            protected static final String FINE     = "FINE";
            protected static final String WARNING  = "WARNING";
            protected static final String CRITICAL = "CRITICAL";
            
            private static final double WARNING_COEFFICIENT  = 0.5;
            private static final double CRITICAL_COEFFICIENT = 0.7;
            
            
            public Status( final String status, final long time ) {
                setStatus( status, time );
            }
            
            public void setStatus( final String status, final long time )
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
        }
        
        private static class CpuInfo extends Device<Object,Object>
        {
            private long _id;
            private Map<Long,CoreInfo> coresMap;
            
            
            public CpuInfo( final long timeBudget, final Mode mode, final long id, final long index ) throws IOException
            {
                super( "", "Models/cpu_frequencies.txt" );
                
                PESOSmodel model = new PESOSmodel( timeBudget, mode, "Models/Distributed/Node_" + index + "/PESOS/MaxScore/" );
                model.setDevice( this );
                model.loadModel();
                
                _id = id;
                coresMap = new HashMap<>();
                for (long i = 0; i < CPU_CORES; i++) {
                    coresMap.put( i, new CoreInfo( _id, i, model ) );
                }
            }
            
            public void addQuery( final Time time, final long coreID, final PesosQuery query ) {
                coresMap.get( coreID ).addQuery( time, query );
            }
            
            public void completedQuery( final Time time, final long coreID ) {
                coresMap.get( coreID ).completedQuery( time );
            }
            
            public CoreInfo getCore( final long coreID ) {
                return coresMap.get( coreID );
            }
            
            public Collection<CoreInfo> getCores() {
                return coresMap.values();
            }

            public long getId() {
                return _id;
            }
            
            @Override
            public Time timeToCompute( final Task task ) {
                return null;
            }
            @Override
            public double getUtilization( final Time time ) {
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
            private long _cpuId;
            private long _coreId;
            
            private long _initTimeBudget;
            private long _timeBudget;
            
            private static final long DELTA_TIME_THRESHOLD = 50000;
            
            public CoreInfo( final long cpuId, final long coreId, final PESOSmodel model ) throws IOException
            {
                _cpuId = cpuId;
                _coreId = coreId;
                _initTimeBudget = model.getTimeBudget().getTimeMicroseconds();
                _timeBudget = model.getTimeBudget().getTimeMicroseconds();
                
                queue = new ArrayList<>( 64 );
                _model = model;
            }
            
            public void addQuery( final Time time, final PesosQuery query )
            {
                QueryInfo q = _model.getQuery( query.getId() );
                //PesosQuery pQuery = new PesosQuery( query.getId(), query.getTerms(), query.getPostings() );
                query.setInfo( q.getTerms(), q.getPostings() );
                query.predictServiceTime( _model );
                if (!hasMoreQueries()) {
                    query.setStartTime( time );
                }
                queue.add( query );
            }
            
            public void completedQuery( final Time time )
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
            
            public void printQueue()
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
            }
            
            public PesosQuery getFirstQuery() {
                return queue.get( 0 );
            }
            
            public boolean hasMoreQueries() {
                return !queue.isEmpty();
            }
            
            public boolean checkTimeBudget( final long timeBudget )
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
            
            public PesosQuery( final long id, final long versionId )
            {
                _id = id;
                _versionId = versionId;
            }
            
            public void setInfo( final int terms, final int postings )
            {
                _terms = terms;
                _postings = postings;
            }
            
            public void predictServiceTime( final PESOSmodel model ) {
                _serviceTime = model.predictServiceTimeAtMaxFrequency( _terms, _postings );
                //System.out.println( "QUERY: " + _id + ", PREDICTED: " + _serviceTime + ", REAL: " + model.getQuery( _id ).getTime( 3500000 )  );
            }
            
            /**
             * Returns the residual predicted service time of the query.
             * 
             * @param time    time of evaluation.
            */
            public long getResidualServiceTime( final long time )
            {
                if (_startTime == -1) {
                    return _serviceTime;
                } else {
                    long elapsedTime = time - _startTime;
                    return Math.max( 0, _serviceTime - elapsedTime );
                }
            }
            
            public void setStartTime( final Time time ) {
                _startTime = time.getTimeMicroseconds();
            }
            
            private Long getVersionId() {
                return _versionId;
            }
            
            public Long getId() {
                return _id;
            }

            @Override
            public int compareTo( final PesosQuery query )
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
    
    private static class PESOSmessage
    {
        private long _coreID;
        private long _queryID;
        private long _timeBudget;
        
        public PESOSmessage( final long coreID, final long queryID, final long timeBudget )
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
    }
    
    /*private static class PesosOracleAgent extends Agent implements EventHandler
    {
        private PesosPredictorGenerator evt_gen;
        
        public PesosOracleAgent( final long id, final PesosPredictorGenerator generator )
        {
            super( id );
            addEventGenerator( generator );
            addEventHandler( this );
        }
        
        private Status analyzeSystem()
        {
            // TODO completare.
            
            return Status.FINE;
        }
        
        @Override
        public Time handle( final Event e, final EventType type )
        {
            if (type == EventType.RECEIVED) {
                System.out.println( "RICEVUTO: " + e );
                //TODO CPUmessage msg = e.getPacket().getContent( Global.CPU_MESSAGE );
                long node = e.getSource().getId();
                evt_gen.cpuInfos.get( node )/*.addQuery( msg.getQueryID(), msg.getCoreID() )*//*;
                
                // Riceve 3 tipi di status: FINE, WARNING e CRITICAL.
                // Col primo non fa niente, col secondo invia un valore molto basso di delay, col terzo invia un valore piu' elevato.
                Status status = analyzeSystem();
                if (status == Status.CRITICAL) {
                    //model.predictServiceTimeAtMaxFrequency( terms, postings );
                }
            }
            
            return Time.ZERO;
        }
        
        private enum Status {
            FINE,
            WARNING,
            CRITICAL;
        }
    }*/
    
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
                packet = e.getPacket();
            }
            
            return packet;
        }
    }
    
    private static class SwitchAgent extends Agent implements EventHandler
    {
        public SwitchAgent( final long id, final EventGenerator evGenerator )
        {
            super( id );
            addEventGenerator( evGenerator );
            addEventHandler( this );
        }
        
        @Override
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
                if (type == EventType.GENERATED) {
                    // Event generated by a node as a response to the client.
                    computeIdleEnergy( e.getSource().getId(), e.getTime() );
                }
            //}
            
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
        private long _versionId = 0;
        
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
            
            //System.out.println( "RECEIVED QUERY: " + e.getPacket().getContents() );
            
            if (p.hasContent( Global.PESOS_TIME_BUDGET )) {
                PESOSmessage message = p.getContent( Global.PESOS_TIME_BUDGET );
                PESOScore core = (PESOScore) cpu.getCore( message.getCoreID() );
                core.setTimeBudget( message.getTimeBudget(), message.getQueryID() );
            }
            
            if (p.hasContent( Global.QUERY_ID )) {
                QueryInfo query = model.getQuery( p.getContent( Global.QUERY_ID ) );
                //System.out.println( "RECEIVED QUERY: " + p.getContent( Global.QUERY_ID ) );
                query.setEvent( e );
                query.setArrivalTime( e.getArrivalTime() );
                long coreID = cpu.selectCore( e.getArrivalTime() );
                cpu.addQuery( coreID, query );
                
                if (PESOS_CONTROLLER) {
                    long versionId = _versionId++ % Long.MAX_VALUE;
                    controller.addQuery( e.getTime(), getId(), coreID, query.getId(), versionId );
                }
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
                    //cpu.checkQueryCompletion( e.getTime() );
                    
                    // TODO testare se questo controllo funziona
                    for (long i = 0; i < CPU_CORES; i++) {
                        if (cpu.getCore( i ).checkQueryCompletion( e.getTime() )) {
                            if (PESOS_CONTROLLER) {
                                controller.completedQuery( e.getTime(), getId(), i );
                            }
                        }
                    }
                }
            } else {
                if (e.getPacket().hasContent( Global.QUERY_ID )) {
                    // Compute the time to complete the query.
                    EnergyCPU cpu = getDevice( new EnergyCPU() );
                    return cpu.timeToCompute( null );
                } else {
                    return Time.ZERO;
                }
            }
            
            return _node.getTcalc();
        }
        
        @Override
        public double getNodeUtilization( final Time time ) {
            return getDevice( new EnergyCPU() ).getUtilization( time );
        }
    }
    
    
    
    
    
    
    
    public static void createDistributedIndex() throws Exception
    {
        // TODO provare a ottenere valori molto diversi partendo pero' dalle query originali
        // TODO senza cioe' dividere il valore originale per 5.
        final String dir = "Models/Monolithic/PESOS/MaxScore/";
        List<PrintWriter> writers = new ArrayList<>( NODES );
        final double RANDOM_RANGE = 20;
        
        List<Double> ranges = new ArrayList<>( NODES );
        for (int i = 0; i < NODES; i++) {
            ranges.add( -1 * Math.random() * RANDOM_RANGE );
        }
        
        String file = dir + "predictions.txt";
        InputStream loader = ResourceLoader.getResourceAsStream( file );
        BufferedReader reader = new BufferedReader( new InputStreamReader( loader ) );
        
        for (int i = 1; i <= NODES; i++) {
            writers.add( new PrintWriter( "Models/Distributed/Node_" + i + "/PESOS/MaxScore/predictions.txt", "UTF-8" ) );
        }
        
        String line = null;
        while ((line = reader.readLine()) != null) {
            String[] values = line.split( "\\t+" );
            long queryID    = Long.parseLong( values[0] );
            int terms       = Integer.parseInt( values[1] );
            int postings    = Integer.parseInt( values[2] ) / NODES;
            for (int i = 0; i < NODES; i++) {
                int difference = (int) (postings / 100d * ranges.get( i ));
                if (i % 2 == 0) {
                    difference *= -1;
                }
                postings += difference;
                writers.get( i ).println( queryID + "\t" + terms + "\t" + postings );
            }
        }
        
        for (int i = 0; i < NODES; i++) {
            writers.get( i ).close();
        }
        writers.clear();
        loader.close();
        
        // TODO qui non so proprio come mettere
        /*file = dir + "regressors_normse.txt";
        loader = ResourceLoader.getResourceAsStream( file );
        reader = new BufferedReader( new InputStreamReader( loader ) );
        
        for (int i = 1; i <= NODES; i++) {
            writers.add( new PrintWriter( "Models/Distributed/Node_" + i + "/PESOS/MaxScore/predictions.txt", "UTF-8" ) );
        }
        
        
        
        for (int i = 1; i <= NODES; i++) {
            writers.get( i ).close();
        }
        writers.clear();
        loader.close();
        
        file = dir + "regressors.txt";
        loader = ResourceLoader.getResourceAsStream( file );
        reader = new BufferedReader( new InputStreamReader( loader ) );
        
        loader.close();*/
        
        file = dir + "time_energy.txt";
        loader = ResourceLoader.getResourceAsStream( file );
        reader = new BufferedReader( new InputStreamReader( loader ) );
        
        for (int i = 1; i <= NODES; i++) {
            writers.add( new PrintWriter( "Models/Distributed/Node_" + i + "/PESOS/MaxScore/time_energy.txt", "UTF-8" ) );
        }
        
        while ((line = reader.readLine()) != null) {
            String[] values = line.split( "\\s+" );
            long queryID = Long.parseLong( values[0] );
            for (int j = 0; j < NODES; j++) {
                writers.get( j ).print( queryID + " " );
            }
            
            for (int i = 1; i < values.length; i+=2) {
                double qTime  = Double.parseDouble( values[i] ) / NODES;
                double energy = Double.parseDouble( values[i+1] ) / NODES;
                long time     = Utils.getTimeInMicroseconds( qTime, TimeUnit.MILLISECONDS );
                for (int j = 0; j < NODES; j++) {
                    int difference = (int) (energy / 100d * ranges.get( j ));
                    if (j % 2 == 0) {
                        difference *= -1;
                    }
                    energy += difference;
                    
                    difference = (int) (time / 100d * ranges.get( j ));
                    if (j % 2 == 0) {
                        difference *= -1;
                    }
                    time += difference;
                    
                    if (i < values.length - 2) {
                        writers.get( j ).print( time + " " + energy + " " );
                    } else {
                        writers.get( j ).print( time + " " + energy );
                    }
                }
            }
            
            for (int j = 0; j < NODES; j++) {
                writers.get( j ).println();
            }
        }
        
        for (int i = 0; i < NODES; i++) {
            writers.get( i ).close();
        }
        writers.clear();
        
        loader.close();
    }
    
    public static void main( final String[] args ) throws Exception
    {
        //createDistributedIndex();
        
        Utils.VERBOSE = false;
        PESOS_CONTROLLER = false;
        
        testAnimationNetwork( Mode.PESOS_TIME_CONSERVATIVE,  500 );
        //testAnimationNetwork( Mode.PESOS_TIME_CONSERVATIVE, 1000 );
        //testAnimationNetwork( Mode.PESOS_ENERGY_CONSERVATIVE,  500 );
        //testAnimationNetwork( Mode.PESOS_ENERGY_CONSERVATIVE, 1000 );
        
        // PESOS Controller On
        //CPU: 0, Energy: 111.81421579220957
        //CPU: 1, Energy: 112.04269079417281
        //CPU: 2, Energy: 110.50349078120033
        //CPU: 3, Energy: 121.32924088682292
        //CPU: 4, Energy: 114.56436581673024
        
        // PESOS Controller Off
        //CPU: 0, Energy: 111.81421579220957
        //CPU: 1, Energy: 112.04269079417281
        //CPU: 2, Energy: 110.50349078120033
        //CPU: 3, Energy: 121.32924088682292
        //CPU: 4, Energy: 114.56436581673024
    }
    
    public static void testAnimationNetwork( final Mode mode, final long timeBudget ) throws Exception
    {
        NetworkTopology net = new NetworkTopology( "Topology/Animation/Topology_distributed_multiCore.json" );
        net.setTrackingEvent( "Results/distr_multi_core.txt" );
        System.out.println( net.toString() );
        
        Simulator sim = new Simulator( net );
        
        // Create client.
        EventGenerator generator = new ClientGenerator( PACKET, PACKET );
        Agent client = new ClientAgent( 0, generator );
        net.addAgent( client );
        
        // Create switch.
        EventGenerator switchGen = new SwitchGenerator( Time.INFINITE );
        Agent switchAgent = new SwitchAgent( 1, switchGen );
        net.addAgent( switchAgent );
        client.getEventGenerator( 0 ).connect( switchAgent );
        
        // Create PESOS oracle.
        controller = new PesosController( timeBudget * 1000, mode );
        
        final String modelType = "PESOS_" + mode + "_" + timeBudget + "ms";
        Plotter plotter = new Plotter( "DISTRIBUTED MULTI_CORE - " + modelType, 800, 600 );
        
        cpus = new ArrayList<>( NODES );
        for (int i = 0; i < NODES; i++) {
            EnergyCPU cpu = new EnergyCPU( "Intel i7-4770K", 4, 1, "Models/cpu_frequencies.txt" );
            cpu.addSampler( Global.ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, "Log/" + modelType + "_Energy.log" );
            cpu.addSampler( Global.IDLE_ENERGY_SAMPLING, new Time( 5, TimeUnit.MINUTES ), Sampling.CUMULATIVE, null );
            cpu.addSampler( Global.TAIL_LATENCY_SAMPLING, null, null, "Log/" + modelType + "_Tail_Latency.log" );
            cpus.add( cpu );
            
            // Add the PESOS model to the corresponding cpu.
            CPUEnergyModel model = new PESOSmodel( timeBudget, mode, "Models/Distributed/Node_" + (i+1) + "/PESOS/MaxScore/" );
            model.loadModel();
            cpu.setModel( model );
            
            EventGenerator sink = new MulticoreGenerator( Time.INFINITE );
            Agent agentCore = new MulticoreAgent( 2 + i, sink );
            agentCore.addDevice( cpu );
            net.addAgent( agentCore );
            
            plotter.addPlot( cpu.getSampledValues( Global.ENERGY_SAMPLING ), "Node " + (i+1) + " " + model.getModelType( true ) );
            
            switchGen.connect( agentCore );
            controller.connect( agentCore );
        }
        
        plotter.setAxisName( "Time (h)", "Energy (J)" );
        plotter.setTicks( Axis.Y, 10 );
        plotter.setTicks( Axis.X, 24, 2 );
        plotter.setRange( Axis.Y, 0, 1800 );
        plotter.setScaleX( 60d * 60d * 1000d * 1000d );
        plotter.setVisible( true );
        
        sim.start( new Time( 24, TimeUnit.HOURS ), false );
        //sim.start( new Time( 53100, TimeUnit.MICROSECONDS ), false );
        sim.close();
        
        for (int i = 0; i < NODES; i++) {
            EnergyCPU cpu = cpus.get( i );
            double energy = cpu.getResultSampled( Global.ENERGY_SAMPLING );
            System.out.println( "CPU: " + i + ", Energy: " + energy );
        }
        
        // Show the animation.
        AnimationNetwork an = new AnimationNetwork( 800, 600, modelType );
        an.loadSimulation( "Topology/Animation/Topology_distributed_multiCore.json", "./Results/distr_multi_core.txt" );
        an.setTargetFrameRate( 90 );
        an.setForceExit( false );
        an.start();
    }
}