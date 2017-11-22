
package simulator.test.energy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import simulator.core.Device;
import simulator.events.Event;
import simulator.test.energy.CPUModel.QueryInfo;
import simulator.utils.Time;

public abstract class CPU extends Device<Long,QueryInfo>
{
    protected double _cores;
    protected Map<Long,Core> coresMap;
    protected EnergyModel energyModel;
    protected int _contexts;
    protected long currentCoreId = -1;
    protected long lastSelectedCore = -1;
    protected QueryInfo lastQuery;
    
    public CPU( String name, List<Long> frequencies ) {
        super( name, frequencies );
    }

    public void setEnergyModel( EnergyModel model ) {
        energyModel = model;
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
    
    public Core getCore( long index ) {
        return coresMap.get( index );
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
        
        // TODO implementare i context di ogni core della CPU (se proprio c'e' bisogno).
        
        
        public Core( CPU cpu, long coreId, long initFrequency )
        {
            this.cpu = cpu;
            this.coreId = coreId;
            time = new Time( 0, TimeUnit.MICROSECONDS );
            queryQueue = new ArrayList<>( 1024 );
            frequency = initFrequency;
        }
        
        public long getId() {
            return coreId;
        }
        
        public int getExecutedQueries() {
            return queriesExecuted;
        }
        
        protected void addQueryOnSampling( Time time, boolean updateFrequency )
        {
            Time startTime   = currentQuery.getStartTime();
            Time endTime     = currentQuery.getEndTime();
            Time tailLatency = endTime.clone().subTime( currentQuery.getArrivalTime() );
            cpu.addSampledValue( Global.TAIL_LATENCY_SAMPLING, endTime,
                                 endTime, tailLatency.getTimeMicros() );
            
            cpu.addSampledValue( Global.ENERGY_SAMPLING, startTime,
                                 endTime, currentQuery.getEnergyConsumption() );
            
            cpu.addSampledValue( Global.MEAN_COMPLETION_TIME, endTime,
                                 endTime, tailLatency.getTimeMicros() );
            
            // TODO writeResult( currentQuery.getFrequency(), currentQuery.getLastEnergy() );
            
            // Compute the current idle energy.
            addIdleEnergy( endTime, true );
            
            removeQuery( time, 0, updateFrequency );
            
            queriesExecuted++;
            currentQuery = null;
        }
        
        public void addIdleEnergy( Time time, boolean allCores )
        {
            double idleEnergy = 0;
            Time startTime = time.clone().subTime( getIdleTime(), TimeUnit.MICROSECONDS );
            if (!allCores) { // Only the current core.
                idleEnergy = getIdleEnergy();
            } else {
                for (Core core : cpu.getCores()) {
                    idleEnergy += core.getIdleEnergy();
                }
            }
            
            cpu.addSampledValue( Global.ENERGY_SAMPLING, startTime, time, idleEnergy );
            cpu.addSampledValue( Global.IDLE_ENERGY_SAMPLING, startTime, time, idleEnergy );
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
                    //TODO writeResult( frequency, currentQuery.getElapsedEnergy() );
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
                idleTime += elapsedTime;
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
        
        public abstract double getIdleEnergy();
        
        public double getUtilization( Time time )
        {
            double size = queryQueue.size();
            if (size == 0) return 0;
            if (queryQueue.get( 0 ).getEndTime().compareTo( time ) <= 0) size--;
            return size;
        }
    }
}
