
package simulator.test.energy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import simulator.core.Agent;
import simulator.core.Device;
import simulator.core.Task;
import simulator.test.energy.CPUModel.Mode;
import simulator.test.energy.CPUModel.PESOSmodel;
import simulator.test.energy.CPUModel.QueryInfo;
import simulator.test.energy.EnergyCPU.PESOScore;
import simulator.utils.Pair;
import simulator.utils.Time;

public class PESOScontroller
{
    private Map<Long,CpuInfo> cpuInfos;
    private final long timeBudget;
    private final Mode mode;
    
    private List<EnergyCPU> cpus;
    private static int cpu_cores;
    
    //private static final double WARNING_DELAY  = 100000d;
    //private static final double CRITICAL_DELAY = 200000d;
    
    // Former value is number of arrived shards, the latter the completed ones.
    private Map<PesosQuery,Pair<Integer,Integer>> openQueries;
    
    private static final double WEIGHT = 0.9;



    public PESOScontroller( long timeBudget, Mode mode, List<EnergyCPU> cpus, int nodes, int cores )
    {
        cpuInfos = new HashMap<>( nodes );
        this.timeBudget = timeBudget;
        this.mode = mode;
        
        this.cpus = cpus;
        cpu_cores = cores;
        
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
            for (long i = 0; i < cpu_cores; i++) {
                coresMap.put( i, new CoreInfo( i, model ) );
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
    
    public static class PESOSmessage
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
    }
}
