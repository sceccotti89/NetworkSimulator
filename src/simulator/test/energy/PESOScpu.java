
package simulator.test.energy;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import simulator.core.Model;
import simulator.core.Task;
import simulator.test.energy.CPUModel.PESOSmodel;
import simulator.test.energy.CPUModel.QueryInfo;
import simulator.test.energy.EnergyModel.QueryEnergyModel;
import simulator.utils.Time;

public class PESOScpu extends CPU
{
    private static final long QUEUE_CHECK = TimeUnit.SECONDS.toMicros( 1 );
    
    private Map<Long,List<QueryInfo>> coreQueue;
    private LinkedList<QueryReference> queries;
    
    
    
    
    /**
     * Constructor used to retrieve this device.
    */
    public PESOScpu() {
        super( "", Collections.singletonList( 0L ) );
    }
    
    public PESOScpu( final String machine, final int cores, final int contexts,
                     final String frequencies_file ) throws IOException
    {
        this( machine, cores, contexts, readFrequencies( frequencies_file ) );
    }
    
    public PESOScpu( final String machine, final int cores, final int contexts,
                     final List<Long> frequencies ) throws IOException
    {
        super( machine, frequencies );
        
        _cores = Math.max( 1, cores );
        _contexts = contexts;
        
        setFrequency( getMaxFrequency() );
        coresMap = new HashMap<>( (int) _cores );
        
        coreQueue = new HashMap<>();
        queries = new LinkedList<>();
        
        setEnergyModel( new QueryEnergyModel() );
        //setEnergyModel( new CoefficientEnergyModel() );
        //setEnergyModel( new NormalizedEnergyModel() );
        //setEnergyModel( new ParameterEnergyModel() );
    }
    
    @Override
    public void setModel( final Model<Long,QueryInfo> model )
    {
        CPUModel cpuModel = (CPUModel) model;
        for (long i = 0; i < _cores; i++) {
            PESOScore core = new PESOScore( this, i, getMaxFrequency() );
            core.setBaseTimeBudget( getTime(), cpuModel.getTimeBudget().getTimeMicros() );
            coresMap.put( i, core );
        }
        
        super.setModel( model );
        model.setDevice( this );
    }
    
    @Override
    public void addQuery( final long coreId, final QueryInfo q )
    {
        Core core = getCore( coreId );
        if (core.getQueue().isEmpty()) {
            core.addQuery( q, false );
            removeQuery( q );
        }
        lastQuery = q;
    }
    
    private void removeQuery( final QueryInfo q )
    {
        for (int i = 0; i < queries.size(); i++) {
            QueryReference ref = queries.get( i );
            if (ref.getQuery().equals( q )) {
                queries.remove( i );
                break;
            }
        }
    }
    
    @Override
    public long selectCore( final Time time, final QueryInfo q )
    {
        System.out.println( "AGGIUNTA QUERY: " + q.getId() );
        QueryReference toAssign = new QueryReference( -1, q );
        queries.add( toAssign );
        analyzeFrequency( time, -1 );
        //System.out.println( "TIME: " + time + ", AGGIUNTA QUERY AL CORE: " + toAssign.getCoreId() + ", PROSSIMA: " + getCore( toAssign.getCoreId() ).getFirstQueryInQueue() );
        return lastSelectedCore = toAssign.getCoreId();
    }
    
    private void analyzeFrequency( final Time time, final long currentCore )
    {
        int[] currentQueries = new int[(int) _cores];
        long[] frequencies = new long[(int) _cores];
        for (Core core : getCores()) {
            frequencies[(int) core.getId()] = core.getFrequency();
            List<QueryInfo> queue = core.getQueue();
            coreQueue.put( core.getId(), queue );
            currentQueries[(int) core.getId()] = queue.size();
        }
        
        // TODO Per adesso assegna le query in base al miglior core.
        // TODO In futuro, forse, eseguire tutte le possibili combinazioni.
        //System.out.println( "QUERIES: " + queries.size() );
        for (int i = queries.size() - 1; i >= 0; i--) {
            QueryReference ref = queries.get( i );
            long minFrequency = Long.MAX_VALUE;
            long coreId = -1;
            long tiedSelection = Long.MAX_VALUE;
            boolean tieSituation = false;
            
            for (Core core : getCores()) {
                List<QueryInfo> queue = coreQueue.get( core.getId() );
                queue.add( ref.getQuery() );
                long frequency = _model.eval( time, queue.toArray( new QueryInfo[0] ) );
                if (frequency < minFrequency) {
                    minFrequency = frequency;
                    coreId = core.getId();
                    tiedSelection = core.tieSelected;
                } else if (minFrequency == frequency) {
                    if (core.tieSelected < tiedSelection) {
                        coreId = core.getId();
                        minFrequency = frequency;
                        tiedSelection = core.tieSelected;
                    }
                    tieSituation = true;
                }
                queue.remove( queue.size() - 1 );
            }
            
            ref.coreId = coreId;
            frequencies[(int) coreId] = minFrequency;
            List<QueryInfo> queue = coreQueue.get( coreId );
            queue.add( ref.getQuery() );
            
            System.out.println( "QUERY: " + ref.getQuery().getId() + ", ASSEGNATA A: " + coreId + ", FREQ: " + frequencies[(int) coreId] );
            
            if (tieSituation) {
                getCore( coreId ).tieSelected++;
            }
        }
        
        for (Core core : getCores()) {
            int queries = currentQueries[(int) core.getId()];
            // Remove the added queries.
            for (int i = coreQueue.get( core.getId() ).size() - queries; i > 0; i--) {
                core.removeQuery( queries );
            }
            
            // Set the new frequency.
            long frequency = frequencies[(int) core.getId()];
            if (queries == 0) {
                core.setFrequency( frequency );
                if (currentCore != -1 && core.getId() != currentCore) {
                    // Add and execute immediately the next available query (if any).
                    core.addQuery( getNextQuery( time, core.getId() ), true );
                }
            } else {
                QueryInfo first = core.getFirstQueryInQueue();
                if (first.getStartTime().getTimeMicros() > 0) {
                    core.setFrequency( time, frequency );
                }
            }
        }
    }
    
    // TODO forse un miglior scheduler potrebbe essere quello che controlla il tempo di
    // TODO completamento delle varie query e le assegna in base ai tempi e non alla frequenza.
    
    /**
     * Returns the first query associated to the requesting core.
     * 
     * @param time      time of evaluation.
     * @param coreId    requesting core identifier.
    */
    protected QueryInfo getNextQuery( final Time time, final long coreId )
    {
        for (int i = 0; i < queries.size(); i++) {
            QueryReference ref = queries.get( i );
            if (ref.getCoreId() == coreId) {
                QueryInfo query = queries.remove( i ).getQuery();
                return query;
            }
        }
        return null;
    }
    
    @Override
    public void evalCONSparameters( final Time time ) {
        // Empty body.
    }
    
    @Override
    protected long evalFrequency( final Time time, final Core core )
    {
        Model<Long,QueryInfo> model = getModel();
        List<QueryInfo> queue = core.getQueue();
        
        // Evaluate the "best" frequency to use.
        return model.eval( time, queue.toArray( new QueryInfo[0] ) );
    }
    
    @Override
    public Time timeToCompute( final Task task )
    {
        Core core = coresMap.get( getLastSelectedCore() );
        QueryInfo query = lastQuery;
        return computeTime( query, core );
    }
    
    @Override
    protected void computeEnergyConsumption( final Core core, final QueryInfo query, final Time computeTime )
    {
        long frequency = core.getFrequency();
        double energy = query.getEnergy( frequency );
        energy = energyModel.computeEnergy( energy, frequency, computeTime, false );
        query.setEnergyConsumption( energy );
        core.addIdleEnergy( query.getStartTime(), true );
    }
    
    @Override
    public String getID() {
        return "PESOS_CPU";
    }
    
    private static class QueryReference
    {
        private long coreId;
        private QueryInfo query;
        
        public QueryReference( final long coreId, final QueryInfo query )
        {
            this.coreId = coreId;
            this.query = query;
        }
        
        public long getCoreId() {
            return coreId;
        }
        
        public QueryInfo getQuery() {
            return query;
        }
    }
    
    public static class PESOScore extends Core
    {
        private long baseTimeBudget;
        private long timeBudget;
        
        public PESOScore( final PESOScpu cpu, final long coreId, final long initFrequency ) {
            super( cpu, coreId, initFrequency );
        }
        
        @Override
        public void addQuery( final QueryInfo q, final boolean execute )
        {
            if (q != null) {
                q.setCoreId( coreId );
                queryQueue.add( q );
                if (execute) {
                    cpu.computeTime( q, this );
                }
            }
        }
        
        @Override
        public boolean checkQueryCompletion( final Time time )
        {
            //System.out.println( "TIME: " + time + ", CORE: " + getId() + ", ATTUALE: " + currentQuery );
            if (currentQuery != null && currentQuery.getEndTime().compareTo( time ) <= 0) {
                System.out.println( "TIME: " + time + ", CORE: " + getId() + ", COMPLETATA QUERY: " + currentQuery );
                addQueryOnSampling();
                if (timeBudget != baseTimeBudget) {
                    timeBudget = baseTimeBudget;
                    PESOSmodel model = (PESOSmodel) cpu.getModel();
                    model.setTimeBudget( timeBudget );
                }
                
                ((PESOScpu) cpu).analyzeFrequency( time, getId() );
                currentQuery = ((PESOScpu) cpu).getNextQuery( time, getId() );
                System.out.println( "PROSSIMA QUERY: " + currentQuery );
                if (currentQuery != null) {
                    addQuery( currentQuery, false );
                    cpu.computeTime( currentQuery, this );
                }
                return true;
            } else {
                return false;
            }
        }
        
        public void setBaseTimeBudget( final Time time, final long timeBudget )
        {
            baseTimeBudget = timeBudget;
            setTimeBudget( time, timeBudget, null );
        }
        
        public void setTimeBudget( final Time time, final long timeBudget, final Long queryID )
        {
            this.timeBudget = timeBudget;
            System.out.println( "CORE: " + getId() + ", NUOVO TIME BUDGET: " + timeBudget );
            if (queryID != null && currentQuery != null && currentQuery.getId() == queryID) {
                PESOSmodel model = (PESOSmodel) cpu.getModel();
                model.setTimeBudget( timeBudget );
                long frequency = cpu.evalFrequency( currentQuery.getStartTime(), this );
                //long frequency = cpu.evalFrequency( t, this );
                setFrequency( time, frequency );
            }
        }
        
        @Override
        public double getIdleEnergy()
        {
            double idleEnergy = 0;
            if (idleTime > 0) {
                if (idleTimeInterval + idleTime < QUEUE_CHECK) {
                    idleEnergy = cpu.energyModel.getIdleEnergy( frequency, idleTime );
                    idleTimeInterval += idleTime;
                } else {
                    // Set the minimum frequency if the time elapsed between two consecutive queries
                    // is at least QUEUE_CHECK microseconds.
                    long timeAtMaxFreq = QUEUE_CHECK - idleTimeInterval;
                    idleEnergy = cpu.energyModel.getIdleEnergy( frequency, timeAtMaxFreq );
                    
                    frequency = cpu.getMinFrequency();
                    double energy = cpu.energyModel.getIdleEnergy( frequency, idleTime - timeAtMaxFreq );
                    idleEnergy += energy;
                    
                    idleTimeInterval = 0;
                }
                
                idleTime = 0;
            }
            
            return idleEnergy;
        }
    }
}