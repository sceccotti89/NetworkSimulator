
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

import simulator.core.Agent;
import simulator.core.devices.Device;
import simulator.events.Event;
import simulator.test.energy.CPU.Core.State;
import simulator.test.energy.CPUModel.PESOSmodel;
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
    
    protected Scheduler<Iterable<Core>,Long,QueryInfo> _scheduler;
    
    protected boolean centralizedQueue = false;
    protected Map<Long,List<QueryInfo>> coreQueue;
    protected LinkedList<QueryReference> queries;
    protected Map<Long,Long> completionTime;
    
    protected double minPower;
    protected double maxPower;
    protected double currentPower;
    
    
    
    /*public CPU( String cpuSpec ) throws Exception
    {
        super( cpuSpec );
        coresMap = new HashMap<>( (int) _cores );
        setFrequency( getMinFrequency() );
        setScheduler( new TieLeastLoaded() );
    }*/
    
    public CPU( String cpuSpec, Class<? extends Core> coreClass ) throws Exception
    {
        super( cpuSpec );
        coresMap = new HashMap<>( (int) _cores );
        setFrequency( getMinFrequency() );
        
        for (long i = 0; i < _cores; i++) {
            coresMap.put( i, coreClass.getConstructor( CPU.class, long.class, long.class )
                                      .newInstance( this, i, getMaxFrequency() ) );
        }
        
        setScheduler( new TieLeastLoaded() );
    }
    
    public CPU( String name, List<Long> frequencies )
    {
        super( name, frequencies );
        setFrequency( getMinFrequency() );
        setScheduler( new TieLeastLoaded() );
    }
    
    @Override
    public void build( String cpuSpec ) throws IOException
    {
        InputStream fReader = ResourceLoader.getResourceAsStream( cpuSpec );
        BufferedReader br = new BufferedReader( new InputStreamReader( fReader ) );
        StringBuilder content = new StringBuilder( 64 );
        String nextLine = null;
        while((nextLine = br.readLine()) != null) {
            content.append( nextLine.trim() );
        }
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
    
    public void setScheduler( Scheduler<Iterable<Core>,Long,QueryInfo> scheduler ) {
        _scheduler = scheduler;
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
                //power -= EnergyModel.getCoreStaticPower();
            }
        }
        
        if (activeCores == 0) {
            return;
        }
        
        double corePower = power / activeCores;
        // Find the highest frequency less than the target.
        List<Long> frequencies = getFrequencies();
        for (int i = frequencies.size() - 1; i >= 0; i--) {
            long frequency = frequencies.get( i );
            double energy = energyModel.computeEnergy( 0, frequency, Utils.MILLION, false );
            if (energy <= corePower) {
                setFrequency( time, frequency );
                break;
            }
        }
        
        /*power /= activeCores * EnergyModel.getAlpha();
        double targetFrequency = Math.pow( power, 1/EnergyModel.getBeta() ) * Utils.MILLION;
        // Find the highest frequency less than the target.
        for (int i = frequencies.size() - 1; i >= 0; i--) {
            if (frequencies.get( i ) <= targetFrequency) {
                setFrequency( time, frequencies.get( i ) );
                break;
            }
        }*/
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
            completionTime = new HashMap<Long,Long>();
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
    
    /**
     * Analyzes the best frequency when the centralized queue is used.
     * 
     * @param time           time of evaluation
     * @param currentCore    core which called this method
    */
    protected void analyzeFrequency( Time time, long currentCore )
    {
        int[] currentQueries = new int[(int) _cores];
        long[] frequencies = new long[(int) _cores];
        for (Core core : getCores()) {
            frequencies[(int) core.getId()] = core.getFrequency();
            List<QueryInfo> queue = core.getQueue();
            // Queue can be empty or containing just one query: the one in execution by the core.
            coreQueue.put( core.getId(), queue );
            currentQueries[(int) core.getId()] = queue.size();
            
            long timeToComplete = 0;
            for (QueryInfo q : queue) {
                PESOSmodel model = (PESOSmodel) _model;
                final int postings = q.getPostings() + model.getRMSE( q.getTerms() );
                timeToComplete += model.predictServiceTimeAtMaxFrequency( q.getTerms(), postings );
            }
            completionTime.put( core.getId(), timeToComplete );
        }
        
        //System.out.println( "QUERIES: " + queries.size() );
        for (int i = queries.size() - 1; i >= 0; i--) {
            QueryReference ref = queries.get( i );
            long minTarget = Long.MAX_VALUE;
            long coreId = -1;
            long tiedSelection = Long.MAX_VALUE;
            boolean tieSituation = false;
            
            if (_scheduler instanceof TieLeastLoaded) {
                for (Core core : getCores()) {
                    long coreUtilization = (long) core.getUtilization( time );
                    if (coreUtilization < minTarget) {
                        minTarget = coreUtilization;
                        coreId = core.getId();
                        tiedSelection = core.tieSelected;
                        tieSituation = false;
                    } else if (coreUtilization == minTarget) {
                        if (core.tieSelected < tiedSelection) {
                            coreId = core.getId();
                            minTarget = coreUtilization;
                            tiedSelection = core.tieSelected;
                        }
                        tieSituation = true;
                    }
                }
            } else if (_scheduler instanceof LowestPredictedFrequency) {
                for (Core core : getCores()) {
                    List<QueryInfo> queue = coreQueue.get( core.getId() );
                    queue.add( ref.getQuery() );
                    long frequency = _model.eval( time, queue.toArray( new QueryInfo[0] ) );
                    if (frequency < minTarget) {
                        minTarget = frequency;
                        coreId = core.getId();
                        tiedSelection = core.tieSelected;
                        tieSituation = false;
                    } else if (frequency == minTarget) {
                        if (core.tieSelected < tiedSelection) {
                            coreId = core.getId();
                            minTarget = frequency;
                            tiedSelection = core.tieSelected;
                        }
                        tieSituation = true;
                    }
                    queue.remove( queue.size() - 1 );
                }
                //frequencies[(int) coreId] = minTarget;
            } else if (_scheduler instanceof EarliestCompletionTime) {
                for (Core core : getCores()) {
                    long executionTime = completionTime.get( core.getId() );
                    if (executionTime < minTarget) {
                        coreId = core.getId();
                        minTarget = executionTime;
                        tiedSelection = core.tieSelected;
                        tieSituation = false;
                    } else if (executionTime == minTarget) {
                        if (core.tieSelected < tiedSelection) {
                            coreId = core.getId();
                            minTarget = executionTime;
                            tiedSelection = core.tieSelected;
                        }
                        tieSituation = true;
                    }
                }
                
                QueryInfo q = ref.getQuery();
                PESOSmodel model = (PESOSmodel) _model;
                final int postings = q.getPostings() + model.getRMSE( q.getTerms() );
                minTarget += model.predictServiceTimeAtMaxFrequency( q.getTerms(), postings );
                completionTime.put( coreId, minTarget );
            }
            
            
            
            if (tieSituation) {
                getCore( coreId ).tieSelected++;
            }
            
            ref.coreId = coreId;
            List<QueryInfo> queue = coreQueue.get( coreId );
            queue.add( ref.getQuery() );
            frequencies[(int) coreId] = _model.eval( time, queue.toArray( new QueryInfo[0] ) );
        }
        
        for (Core core : getCores()) {
            /*long frequency;
            List<QueryInfo> queue = coreQueue.get( core.getId() );
            if (queue.isEmpty()) {
                frequency = frequencies[(int) core.getId()];
            } else {
                frequency = _model.eval( time, queue.toArray( new QueryInfo[0] ) );
            }*/
            
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
    
    public int getCPUcores() {
        return (int) _cores;
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
            //Time time = _evtScheduler.getTimeDuration();
            query.setTimeToComplete( Time.ZERO, Time.ZERO );
            //System.out.println( "QUERY: " + query.getId() + ", CORE: " + core.getId() + ", TIME: INFINITO (NON E' LA PROSSIMA IMMEDIATA)" );
            //return time.subTime( query.getArrivalTime() );
            return null;
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
     * Enables or disables the job stealing technique.
     * 
     * @param enable    {@code true} to enable the job stealing,
     *                  {@code false} otherwise.
    */
    public void enableJobStealing( boolean enable )
    {
        for (Core core : getCores()) {
            core.enableJobStealing( enable );
        }
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
        protected QueryInfo currentTask = null;
        protected long idleTime = 0;
        protected CPU cpu;
        protected long idleTimeInterval = 0;
        private int queriesExecuted = 0;
        // Number of times this core has been selected in a tied situation.
        protected long tieSelected = 0;
        protected State state;
        protected boolean enableJS = false;
        
        protected double receivedQueries;
        protected double processedQueries;
        protected double cumulativeTime;
        
        // Possible states of the CPU core.
        public enum State {
            RUNNING,
            POWER_OFF
        };
        
        
        public Core( CPU cpu, long coreId, long initFrequency )
        {
            this.cpu = cpu;
            this.coreId = coreId;
            time = new Time( 0, TimeUnit.MICROSECONDS );
            queryQueue = new ArrayList<>( 128 );
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
        
        public void enableJobStealing( boolean enable ) {
            enableJS = enable;
        }
        
        public boolean isWorking( Time time ) {
            //return currentTask != null && currentTask.getEndTime().compareTo( time ) > 0;
            return !queryQueue.isEmpty();
        }
        
        public int getExecutedQueries() {
            return queriesExecuted;
        }
        
        protected void addQueryOnSampling( Time time, boolean updateFrequency )
        {
            Time startTime   = currentTask.getStartTime();
            Time endTime     = currentTask.getEndTime();
            Time tailLatency = endTime.clone().subTime( currentTask.getArrivalTime() );
            Agent agent = cpu.getAgent();
            if (agent.getSampler( Global.TAIL_LATENCY_SAMPLING ) != null) {
                agent.getSampler( Global.TAIL_LATENCY_SAMPLING )
                     .addSampledValue( endTime, endTime, tailLatency.getTimeMicros() );
            }
            
            // Add the static power consumed for the query.
            double energy = currentTask.getEnergyConsumption() +
                            EnergyModel.getStaticPower( currentTask.getCompletionTime() );
            if (agent.getSampler( Global.ENERGY_SAMPLING ) != null) {
                agent.getSampler( Global.ENERGY_SAMPLING )
                     .addSampledValue( startTime, endTime, energy );
            }
            
            if (agent.getSampler( Global.MEAN_COMPLETION_TIME ) != null) {
                agent.getSampler( Global.MEAN_COMPLETION_TIME )
                     .addSampledValue( endTime, endTime, tailLatency.getTimeMicros() );
            }
            
            EnergyCPU.writeResult( currentTask.getFrequency(), currentTask.getLastEnergy() );
            
            // Compute the current idle energy.
            addIdleEnergy( endTime, true );
            
            removeQuery( time, 0, updateFrequency );
            
            queriesExecuted++;
            currentTask = null;
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
            
            cpu.getAgent().getSampler( Global.ENERGY_SAMPLING )
                          .addSampledValue( startTime, time, idleEnergy );
            cpu.getAgent().getSampler( Global.IDLE_ENERGY_SAMPLING )
                          .addSampledValue( startTime, time, idleEnergy );
        }
        
        protected void setFrequency( long frequency ) {
            this.frequency = frequency;
        }
        
        public void delayTask( Time time, Time delay )
        {
            if (currentTask == null) {
                setTime( time.clone().addTime( delay ) );
            } else {
                System.out.println( getTime().addTime( delay ) + ": SONO QUI (" + getId() + ")" );
                currentTask.setDelay( delay );
                setTime( currentTask.getEndTime() );
                updateEventTime( currentTask, getTime() );
            }
        }
        
        protected void setFrequency( Time time, long newFrequency )
        {
            if (frequency != newFrequency) {
                //System.out.println( "TIME_BUDGET: " + ((PESOSmodel) cpu.getModel()).getTimeBudget() );
                //System.out.println( "TIME: " + time + ", NEW: " + newFrequency + ", OLD: " + frequency );
                if (currentTask != null) {
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
                    double energy = cpu.energyModel.computeEnergy( currentTask.getEnergy( newFrequency ),
                                                                   newFrequency,
                                                                   currentTask.getTime( newFrequency ),
                                                                   false );
                    currentTask.updateTimeEnergy( time, newFrequency, energy );
                    EnergyCPU.writeResult( frequency, currentTask.getElapsedEnergy() );
                    //System.out.println( "TIME: " + time + ", CORE: " + coreId + ", QUERY: " + currentTask.getId() + ", NEW_END_TIME: " + currentTask.getEndTime() );
                    this.time.setTime( currentTask.getEndTime() );
                    updateEventTime( currentTask, getTime() );
                    
                    //System.out.println( "CORE: " + getId() + ", AGGIORNATA QUERY: " + currentTask );
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
                if (state != State.POWER_OFF && currentTask == null) {
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
         * 
         * @param time    time when the operation starts.
         * 
         * @return the query stolen from another queue,
         *         {@code null} otherwise.
        */
        protected QueryInfo startJobStealing( Time time )
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
                return last;
            }
            
            return null;
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
            currentTask = query;
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
            return !queryQueue.isEmpty() && getFirstQueryInQueue().equals( query );
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
