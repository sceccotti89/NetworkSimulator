
package simulator.test.energy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import simulator.core.Device;
import simulator.events.Event;
import simulator.test.energy.CPU.Core.State;
import simulator.test.energy.CPUModel.QueryInfo;
import simulator.utils.Time;
import simulator.utils.Utils;
import simulator.utils.resources.ResourceLoader;

public abstract class CPU extends Device<QueryInfo,Long>
{
    protected double _cores;
    protected Map<Long,Core> coresMap;
    protected EnergyModel energyModel;
    protected int _contexts;
    protected long currentCoreId = -1;
    protected long lastSelectedCore = -1;
    protected QueryInfo lastQuery;
    
    protected boolean centralizedQueue = false;
    protected Map<Long,List<QueryInfo>> coreQueue;
    protected LinkedList<QueryReference> queries;
    
    protected double minPower;
    protected double maxPower;
    protected double currentPower;
    
    
    
    public CPU( String cpuSpec, Class<? extends Core> coreClass ) throws Exception
    {
        super( cpuSpec );
        coresMap = new HashMap<>( (int) _cores );
        setFrequency( getMaxFrequency() );
        
        for (long i = 0; i < _cores; i++) {
            coresMap.put( i, coreClass.getConstructor( CPU.class, long.class, long.class )
                                      .newInstance( this, i, getMaxFrequency() ) );
        }
    }
    
    public CPU( String name, List<Long> frequencies )
    {
        super( name, frequencies );
        setFrequency( getMaxFrequency() );
    }
    
    @Override
    public void build( String cpuSpec ) throws IOException
    {
        InputStream fReader = ResourceLoader.getResourceAsStream( cpuSpec );
        BufferedReader br = new BufferedReader( new InputStreamReader( fReader ) );
        StringBuilder content = new StringBuilder( 64 );
        String nextLine = null;
        while((nextLine = br.readLine()) != null)
            content.append( nextLine.trim() );
        br.close();
        
        JSONObject settings = new JSONObject( content.toString() );
        this._name  = settings.getString( "Name" );
        this._cores = settings.getInt( "Cores" );
        this._contexts = settings.getInt( "Threads" );
        this.setMaxPower( settings.getDouble( "Max Power" ) );
        this.setMinPower( settings.getDouble( "Min Power" ) );
        //this._cache = settings.getInt( "Cache" );
        JSONArray freqs = settings.getJSONArray( "Frequencies" );
        this._frequencies = new ArrayList<>( freqs.length() );
        for (int i = 0; i < freqs.length(); i++) {
            this._frequencies.add( freqs.getLong( i ) );
        }
    }
    
    public void setEnergyModel( EnergyModel model ) {
        energyModel = model;
    }
    
    public double getPower() {
        return currentPower;
    }
    
    public void setPower( Time time, double newPower )
    {
        newPower = Math.min( newPower, maxPower );
        newPower = Math.max( newPower, minPower );
        if (newPower == currentPower) {
            return;
        }
        currentPower = newPower;
        setFrequencyOnPower( time );
    }
    
    protected void setFrequencyOnPower( Time time )
    {
        double power = currentPower - EnergyModel.getStaticPower();
        double activeCores = 0;
        for (Core core : getCores()) {
            if (core.isWorking( time )) {
                activeCores++;
                power -= EnergyModel.getCoreStaticPower();
            }
        }
        
        if (activeCores == 0) {
            return;
        }
        
        power /= activeCores * EnergyModel.getAlpha();
        double targetFrequency = Math.pow( power, 1/EnergyModel.getBeta() ) * Utils.MILLION;
        //System.out.println( "AC: " + activeCores + ", POWER: " + power + ", C_P: " + currentPower + ", TARGET: " + targetFrequency );
        // Find the highest frequency less than the target.
        List<Long> frequencies = getFrequencies();
        for (int i = frequencies.size() - 1; i >= 0; i--) {
            if (frequencies.get( i ) <= targetFrequency) {
                setFrequency( time, frequencies.get( i ) );
                break;
            }
        }
    }
    
    public void setMaxPower( double power ) {
        currentPower = maxPower = power;
    }
    
    public double getMaxPower() {
        return maxPower;
    }
    
    public void setMinPower( double power ) {
        minPower = power;
    }
    
    public double getMinPower() {
        return minPower;
    }
    
    public void setFrequency( Time now, long frequency )
    {
        for (Core core : getCores()) {
            core.setFrequency( now, frequency );
        }
        setFrequency( frequency );
    }
    
    public void increaseFrequency( Time now, int steps )
    {
        increaseFrequency( steps );
        for (Core core : getCores()) {
            core.setFrequency( now, getFrequency() );
        }
    }
    
    public void decreaseFrequency( Time now, int steps )
    {
        decreaseFrequency( steps );
        for (Core core : getCores()) {
            core.setFrequency( now, getFrequency() );
        }
    }
    
    public void setCentralizedQueue( boolean centralized )
    {
        centralizedQueue = centralized;
        if (centralized) {
            coreQueue = new HashMap<>();
            queries = new LinkedList<>();
        }
    }
    
    public abstract void addQuery( long coreId, QueryInfo q );
    
    @Override
    public void setTime( Time time )
    {
        // Set the time of the cores.
        for (Core cpuCore : coresMap.values()) {
            cpuCore.setTime( time );
        }
    }
    
    @Override
    public Time getTime()
    {
        // Gets the minimum time among all the cores.
        Time coreTime = Time.INFINITE;
        for (Core core : coresMap.values()) {
            if (core.getTime().compareTo( coreTime ) < 0) {
                coreTime = core.getTime();
            }
        }
        return coreTime;
    }
    
    /**
     * Returns the "best" available core to assign the next task.
    */
    public abstract long selectCore( Time time, QueryInfo query );
    
    protected void analyzeFrequency( Time time, long currentCore )
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
                core.removeQuery( time, queries, false );
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
    
    /**
     * Returns the first query associated to the requesting core.
     * 
     * @param time      time of evaluation.
     * @param coreId    requesting core identifier.
    */
    protected QueryInfo getNextQuery( Time time, long coreId )
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
    
    public Collection<Core> getCores() {
        return coresMap.values();
    }
    
    public double getCPUcores() {
        return _cores;
    }
    
    public Long getCurrentCoreId() {
        return currentCoreId;
    }
    
    public long getLastSelectedCore() {
        return lastSelectedCore;
    }
    
    public void setState( Time time, State state )
    {
        for (Core core : coresMap.values()) {
            core.setState( time, state );
        }
    }
    
    public Core getCore( long coreId ) {
        return coresMap.get( coreId );
    }
    
    public void setCoreState( Time time, long coreId, State state ) {
        setCoreState( time, getCore( coreId ), state );
    }
    
    public void setCoreState( Time time, Core core, State state ) {
        core.setState( time, state );
    }
    
    protected Time computeTime( QueryInfo query, Core core )
    {
        lastQuery = query;
        
        // It's not the next query to compute: returns the maximum time (SIM_TIME - arrival).
        if (!core.isNextQuery( query )) {
            Time time = _evtScheduler.getTimeDuration();
            query.setTimeToComplete( Time.ZERO, Time.ZERO );
            //System.out.println( "QUERY: " + query.getId() + ", CORE: " + core.getId() + ", TIME: INFINITO (NON E' LA PROSSIMA IMMEDIATA)" );
            return time.subTime( query.getArrivalTime() );
        }
        
        long frequency = core.getFrequency();
        query.setFrequency( frequency );
        
        Time computeTime = query.getTime( frequency );
        Time startTime   = core.getTime();
        Time endTime     = startTime.clone().addTime( computeTime );
        query.setTimeToComplete( startTime, endTime );
        //System.out.println( "QUERY: " + query.getId() + ", CORE: " + core.getId() + ", START: " + startTime + ", END: " + endTime );
        
        computeEnergyConsumption( core, query, computeTime );
        core.setCompletedQuery( query );
        
        core.updateEventTime( query, query.getEndTime() );
        
        return computeTime;
    }
    
    protected abstract void computeEnergyConsumption( Core core, QueryInfo query, Time computeTime );
    
    /**
     * Evaluates the CONS parameters.
     * 
     * @param time    time of evaluation.
    */
    public abstract void evalCONSparameters( Time time );
    
    /**
     * Evaluates which is the "best" frequency
     * to process the remaining tasks.
     * 
     * @param time    time of evaluation.
     * @param core    core to which assign the evaluated frequency.
    */
    protected abstract long evalFrequency( Time time, Core core );
    
    public QueryInfo getLastQuery() {
        return lastQuery;
    }
    
    /**
     * Checks if the last executed query, on each core, have been completely executed.
     * 
     * @param time    time to check the completness of the query
    */
    public void checkQueryCompletion( Time time )
    {
        for (Core core : coresMap.values()) {
            core.checkQueryCompletion( time );
        }
    }
    
    public void computeIdleEnergy( Time time )
    {
        //System.out.println( "INPUT_TIME: " + time );
        for (Core core : coresMap.values()) {
            core.setTime( time );
            core.addIdleEnergy( time, false );
        }
    }
    
    /**
     * Returns the number of queries executed by the CPU,
     * as the sum of queries executed by each core.
    */
    public int getExecutedQueries()
    {
        int qe = 0;
        for (Core core : coresMap.values()) {
            qe += core.getExecutedQueries();
        }
        return qe;
    }
    
    @Override
    public double getUtilization( Time time )
    {
        double utilization = 0;
        for (Core core : coresMap.values()) {
            utilization += core.getUtilization( time );
        }
        return utilization;
    }
    
    @Override
    public String toString()
    {
        return "Cpu: " + _name + " @ " + _frequencies + "KHz\n" +
               "     " + (int) _cores + " cores with " +
               _contexts + ((_contexts == 1) ? " thread per core." : " threads per core.");
    }
    
    protected static class QueryReference
    {
        private long coreId;
        private QueryInfo query;
        
        public QueryReference( long coreId, QueryInfo query )
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
    
    // Core of the CPU.
    public static abstract class Core
    {
        protected long coreId;
        protected Time time;
        protected List<QueryInfo> queryQueue;
        protected long frequency;
        protected QueryInfo currentQuery = null;
        protected long idleTime = 0;
        protected CPU cpu;
        protected long idleTimeInterval = 0;
        private int queriesExecuted = 0;
        // Number of times this core has been selected in a tied situation.
        protected long tieSelected = 0;
        protected State state;
        
        // Possible states of the CPU core.
        public enum State {
            RUNNING,
            POWER_OFF
        };
        
        // TODO implementare i context di ogni core della CPU (se proprio c'e' bisogno).
        
        
        public Core( CPU cpu, long coreId, long initFrequency )
        {
            this.cpu = cpu;
            this.coreId = coreId;
            time = new Time( 0, TimeUnit.MICROSECONDS );
            queryQueue = new ArrayList<>( 1024 );
            frequency = initFrequency;
            
            state = State.RUNNING;
        }
        
        public long getId() {
            return coreId;
        }
        
        public void setState( Time time, State state )
        {
            setTime( time );
            this.state = state;
        }
        
        public boolean isWorking( Time time ) {
            //return currentQuery != null && currentQuery.getEndTime().compareTo( time ) > 0;
            return !queryQueue.isEmpty();
        }
        
        public int getExecutedQueries() {
            return queriesExecuted;
        }
        
        protected void addQueryOnSampling( Time time, boolean updateFrequency )
        {
            Time startTime   = currentQuery.getStartTime();
            Time endTime     = currentQuery.getEndTime();
            Time tailLatency = endTime.clone().subTime( currentQuery.getArrivalTime() );
            cpu.getAgent().addSampledValue( Global.TAIL_LATENCY_SAMPLING, endTime,
                                            endTime, tailLatency.getTimeMicros() );
            
            // Add the static power consumed for the query.
            double energy = currentQuery.getEnergyConsumption() +
                            EnergyModel.getStaticPower( currentQuery.getCompletionTime() );
            cpu.getAgent().addSampledValue( Global.ENERGY_SAMPLING, startTime,
                                            endTime, energy );
            
            cpu.getAgent().addSampledValue( Global.MEAN_COMPLETION_TIME, endTime,
                                            endTime, tailLatency.getTimeMicros() );
            
            EnergyCPU.writeResult( currentQuery.getFrequency(), currentQuery.getLastEnergy() );
            
            // Compute the current idle energy.
            addIdleEnergy( endTime, true );
            
            removeQuery( time, 0, updateFrequency );
            
            queriesExecuted++;
            currentQuery = null;
        }
        
        public void addIdleEnergy( Time time, boolean allCores )
        {
            double idleEnergy = 0;
            Time startTime = time.clone().subTime( idleTime, TimeUnit.MICROSECONDS );
            if (!allCores) { // Only the current core.
                idleEnergy = getIdleEnergy();
            } else {
                for (Core core : cpu.getCores()) {
                    idleEnergy += core.getIdleEnergy();
                }
            }
            
            cpu.getAgent().addSampledValue( Global.ENERGY_SAMPLING, startTime, time, idleEnergy );
            cpu.getAgent().addSampledValue( Global.IDLE_ENERGY_SAMPLING, startTime, time, idleEnergy );
        }
        
        protected void setFrequency( long frequency ) {
            this.frequency = frequency;
        }
        
        protected void setFrequency( Time time, long newFrequency )
        {
            if (frequency != newFrequency) {
                //System.out.println( "TIME_BUDGET: " + ((PESOSmodel) cpu.getModel()).getTimeBudget() );
                //System.out.println( "TIME: " + time + ", NEW: " + newFrequency + ", OLD: " + frequency );
                if (currentQuery != null) {
                    /*if (newFrequency < frequency) {
                        // FIXME abbassamento frequenza (in genere con PESOS).
                        try {
                            int val = 100 / 0;
                            System.out.println( "VAL: " + val );
                        } catch ( Exception e ){
                            e.printStackTrace();
                            System.out.println( "TIME: " + time + ", " + frequency + " => " + newFrequency + ", QUEUE: " );
                            for (QueryInfo query : queryQueue) {
                                System.out.println( query.getId() +
                                                    ", ARRIVAL: " + query.getArrivalTime() +
                                                    ", START: " + query.getStartTime() );
                            }
                            System.exit( 0 );
                        }
                    }*/
                    
                    // Update the termination time and the energy consumption of the current query.
                    double energy = cpu.energyModel.computeEnergy( currentQuery.getEnergy( newFrequency ),
                                                                   newFrequency,
                                                                   currentQuery.getTime( newFrequency ),
                                                                   false );
                    currentQuery.updateTimeEnergy( time, newFrequency, energy );
                    EnergyCPU.writeResult( frequency, currentQuery.getElapsedEnergy() );
                    //System.out.println( "TIME: " + time + ", CORE: " + coreId + ", QUERY: " + currentQuery.getId() + ", NEW_END_TIME: " + currentQuery.getEndTime() );
                    this.time = currentQuery.getEndTime();
                    updateEventTime( currentQuery, this.time );
                    
                    //System.out.println( "CORE: " + getId() + ", AGGIORNATA QUERY: " + currentQuery );
                }
                
                frequency = newFrequency;
            }
        }
        
        public long getFrequency() {
            return frequency;
        }
        
        public void setTime( Time time )
        {
            if (this.time.compareTo( time ) <= 0) {
                // This is idle time.
                long elapsedTime = time.getTimeMicros() - this.time.getTimeMicros();
                if (state != State.POWER_OFF) {
                    idleTime += elapsedTime;
                }
                this.time.setTime( time );
            }
        }
        
        public Time getTime() {
            return time.clone();
        }

        public abstract boolean checkQueryCompletion( Time time );
        
        /**
         * Starts the job stealing phase.
        */
        protected void startJobStealing( Time time )
        {
            // Get the query from the core with the highest frequency.
            long coreSelected = -1;
            long maxFrequency = Long.MIN_VALUE;
            for (Core core : cpu.getCores()) {
                List<QueryInfo> queue = core.getQueue();
                if (queue.size() > 1) {
                    long coreFrequency = core.getFrequency();
                    if (coreFrequency > maxFrequency) {
                        maxFrequency = coreFrequency;
                        coreSelected = core.getId();
                    }
                }
            }
            
            if (coreSelected >= 0) {
                // Remove the last query of the selected core.
                Core core = cpu.getCore( coreSelected );
                QueryInfo last = core.removeLastQueryInQueue( time, true );
                last.setCoreId( getId() );
                addQuery( last, true );
                cpu.computeTime( last, this );
            }
        }
        
        public void updateEventTime( QueryInfo query, Time time )
        {
            Event event = query.getEvent();
            if (event.getTime().compareTo( time ) != 0) {
                if (cpu.getEventScheduler().remove( event )) {
                    event.setTime( time.clone() );
                    cpu.getEventScheduler().schedule( event );
                }
            }
        }
        
        public boolean hasMoreQueries() {
            return !queryQueue.isEmpty();
        }
        
        public List<QueryInfo> getQueue() {
            return queryQueue;
        }
        
        public void setCompletedQuery( QueryInfo query )
        {
            time = query.getEndTime();
            currentQuery = query;
            idleTimeInterval = 0;
            idleTime = 0;
        }
        
        /**
         * Adds a query into the core.
         *
         * @param query              the query to add.
         * @param updateFrequency    check if the frequency have to be updated.
        */
        public abstract void addQuery( QueryInfo query, boolean updateFrequency );
        
        /**
         * Checks if the input query is the next one for this core.
        */
        public boolean isNextQuery( QueryInfo query ) {
            return !queryQueue.isEmpty() && queryQueue.get( 0 ).equals( query );
        }
        
        public QueryInfo getFirstQueryInQueue() {
            return queryQueue.get( 0 );
        }
        
        public QueryInfo getLastQueryInQueue() {
            return queryQueue.get( queryQueue.size() - 1 );
        }
        
        public QueryInfo removeLastQueryInQueue( Time time, boolean updateFrequency ) {
            return removeQuery( time, queryQueue.size() - 1, updateFrequency );
        }
        
        public QueryInfo removeQuery( Time time, int index, boolean updateFrequency )
        {
            QueryInfo query = queryQueue.remove( index );
            if (updateFrequency && hasMoreQueries()) {
                long frequency = cpu.evalFrequency( time, this );
                setFrequency( time, frequency );
            }
            
            return query;
        }
        
        public void removeQuery( Time time, QueryInfo q, boolean updateFrequency )
        {
            queryQueue.remove( q );
            if (updateFrequency && hasMoreQueries()) {
                long frequency = cpu.evalFrequency( time, this );
                setFrequency( time, frequency );
            }
        }
        
        public long getIdleTime() {
            return idleTime;
        }
        
        /**
         * Returns the energy consumption during the idle period.
        */
        public double getIdleEnergy()
        {
            double idleEnergy = 0;
            if (idleTime > 0) {
                idleEnergy = cpu.energyModel.getIdleEnergy( frequency, idleTime );
                idleTimeInterval += idleTime;
                EnergyCPU.writeResult( frequency, idleEnergy );
                idleTime = 0;
            }
            return idleEnergy;
        }
        
        public double getUtilization( Time time )
        {
            double size = queryQueue.size();
            if (size == 0) return 0;
            if (queryQueue.get( 0 ).getEndTime().compareTo( time ) <= 0) size--;
            return size;
        }
    }
}
