
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
    
    public CPU( final String name, final List<Long> frequencies ) {
        super( name, frequencies );
    }

    public void setEnergyModel( final EnergyModel model ) {
        energyModel = model;
    }
    
    public abstract void addQuery( final long coreId, final QueryInfo q );
    
    @Override
    public void setTime( final Time time )
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
    public abstract long selectCore( final Time time, final QueryInfo query );
    
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
    
    public Core getCore( final long index ) {
        return coresMap.get( index );
    }
    
    protected Time computeTime( final QueryInfo query, final Core core )
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
    
    protected abstract void computeEnergyConsumption( final Core core, final QueryInfo query, final Time computeTime );
    
    /**
     * Evaluates the CONS parameters.
     * 
     * @param time    time of evaluation.
    */
    public abstract void evalCONSparameters( final Time time );
    
    /**
     * Evaluates which is the "best" frequency
     * to process the remaining tasks.
     * 
     * @param time    time of evaluation.
     * @param core    core to which assign the evaluated frequency.
    */
    protected abstract long evalFrequency( final Time time, final Core core );
    
    public QueryInfo getLastQuery() {
        return lastQuery;
    }
    
    /**
     * Checks if the last executed query, on each core, have been completely executed.
     * 
     * @param time    time to check the completness of the query
    */
    public void checkQueryCompletion( final Time time )
    {
        for (Core core : coresMap.values()) {
            core.checkQueryCompletion( time );
        }
    }
    
    public void computeIdleEnergy( final Time time )
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
    public double getUtilization( final Time time )
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
        
        
        public Core( final CPU cpu, final long coreId, final long initFrequency )
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
        
        protected void addQueryOnSampling()
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
            
            removeQuery( 0 );
            
            queriesExecuted++;
            currentQuery = null;
        }
        
        public void addIdleEnergy( final Time time, final boolean allCores )
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
        
        protected void setFrequency( final long frequency ) {
            this.frequency = frequency;
        }
        
        protected void setFrequency( final Time time, final long newFrequency )
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
        
        public void setTime( final Time time )
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

        public abstract boolean checkQueryCompletion( final Time time );
        
        public void updateEventTime( final QueryInfo query, final Time time )
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
        
        public void setCompletedQuery( final QueryInfo query )
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
        public abstract void addQuery( final QueryInfo query, final boolean updateFrequency );
        
        /**
         * Checks if the input query is the next one for this core.
        */
        public boolean isNextQuery( final QueryInfo query ) {
            return !queryQueue.isEmpty() && queryQueue.get( 0 ).equals( query );
        }
        
        public QueryInfo getFirstQueryInQueue() {
            return queryQueue.get( 0 );
        }
        
        public QueryInfo getLastQueryInQueue() {
            return queryQueue.get( queryQueue.size() - 1 );
        }
        
        public QueryInfo removeQuery( final int index ) {
            return queryQueue.remove( index );
        }
        
        public void removeQuery( final QueryInfo q ) {
            queryQueue.remove( q );
        }
        
        public long getIdleTime() {
            return idleTime;
        }
        
        public abstract double getIdleEnergy();
        
        public double getUtilization( final Time time )
        {
            double size = queryQueue.size();
            if (size == 0) return 0;
            if (queryQueue.get( 0 ).getEndTime().compareTo( time ) <= 0) size--;
            return size;
        }
    }
}