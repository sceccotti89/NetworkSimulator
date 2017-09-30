/**
 * @author Stefano Ceccotti
*/

package simulator.test.energy;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import simulator.core.Device;
import simulator.core.Model;
import simulator.core.Task;
import simulator.events.Event;
import simulator.test.energy.CPUEnergyModel.CONSmodel;
import simulator.test.energy.CPUEnergyModel.PESOSmodel;
import simulator.test.energy.CPUEnergyModel.QueryInfo;
import simulator.test.energy.CPUEnergyModel.Type;
import simulator.test.energy.EnergyModel.QueryEnergyModel;
import simulator.utils.Time;
import simulator.utils.Utils;

public class EnergyCPU extends Device<Long,QueryInfo>
{
    protected double _cores;
    protected int _contexts;
    protected long currentCoreId = -1;
    protected long lastSelectedCore = -1;
    protected QueryInfo lastQuery;
    protected EnergyModel energyModel;
    protected Map<Long,Core> coresMap;
    
    private static final long QUEUE_CHECK = TimeUnit.SECONDS.toMicros( 1 );
    
    // TODO File usato per testing
    protected static PrintWriter coeffWriter;
    
    
    
    
    /**
     * Constructor used to retrieve this device.
    */
    public EnergyCPU() {
        super( "", Collections.singletonList( 0L ) );
    }
    
    public EnergyCPU( final String machine, final int cores, final int contexts,
                      final String frequencies_file ) throws IOException
    {
        this( machine, cores, contexts, readFrequencies( frequencies_file ) );
    }
    
    public EnergyCPU( final String machine, final int cores, final int contexts,
                      final List<Long> frequencies ) throws IOException
    {
        super( machine, frequencies );
        
        _cores = Math.max( 1, cores );
        _contexts = contexts;
        
        setFrequency( getMaxFrequency() );
        coresMap = new HashMap<>( (int) _cores );
        
        setEnergyModel( new QueryEnergyModel() );
        //setEnergyModel( new CoefficientEnergyModel() );
        //setEnergyModel( new NormalizedEnergyModel() );
        //setEnergyModel( new ParameterEnergyModel() );
    }
    
    @Override
    public void setModel( final Model<Long,QueryInfo> model )
    {
        // TODO RIMUOVERE IL WRITER DOPO I TEST
        if (coeffWriter != null)
            coeffWriter.close();
        
        CPUEnergyModel cpuModel = (CPUEnergyModel) model;
        try {
            if (cpuModel.getType() == Type.PERF) {
                coeffWriter = new PrintWriter( "Results/Coefficients/PERF_Freq_Energy.txt", "UTF-8" );
            } else {
                String file;
                if (cpuModel.getType() == Type.PESOS) {
                    long timeBudget = cpuModel.getTimeBudget().getTimeMicroseconds()/1000;
                    file = "PESOS_" + timeBudget + "_" + cpuModel.getMode();
                } else {
                    file = "CONS_" + cpuModel.getMode();
                }
                file += (_cores == 1) ? "_distr" : "_mono";
                Utils.checkDirectory( "Results/Coefficients" );
                coeffWriter = new PrintWriter( "Results/Coefficients/" + file + "_Freq_Energy.txt", "UTF-8" );
            }
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        
        for (long i = 0; i < _cores; i++) {
            switch (cpuModel.getType()) {
                case PESOS:
                    PESOScore core = new PESOScore( this, i, getMaxFrequency() );
                    core.setBaseTimeBudget( cpuModel.getTimeBudget().getTimeMicroseconds() );
                    coresMap.put( i, core );
                    break;
                case PERF:  coresMap.put( i, new PERFcore( this, i, getMaxFrequency() ) ); break;
                case CONS:  coresMap.put( i, new CONScore( this, i, getMaxFrequency() ) ); break;
            }
        }
        
        super.setModel( model );
        model.setDevice( this );
    }
    
    public void setEnergyModel( final EnergyModel model ) {
        energyModel = model;
    }
    
    public void addQuery( final long coreId, final QueryInfo q )
    {
        Core core = coresMap.get( coreId );
        core.addQuery( q );
    }
    
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
    public long selectCore( final Time time )
    {
        //System.out.println( "SELECTING CORE AT: " + time );
        long id = -1;
        double utilization = Integer.MAX_VALUE;
        int tiedSelection  = Integer.MAX_VALUE;
        boolean tieSituation = false;
        for (Core core : coresMap.values()) {
            double coreUtilization = core.getUtilization( time );
            if (coreUtilization < utilization) {
                id = core.getId();
                utilization = coreUtilization;
                tiedSelection = core.tieSelected;
                tieSituation = false;
            } else if (coreUtilization == utilization) {
                if (core.tieSelected < tiedSelection) {
                    id = core.getId();
                    utilization = coreUtilization;
                    tiedSelection = core.tieSelected;
                }
                tieSituation = true;
            }
        }
        
        if (tieSituation) {
            getCore( id ).tieSelected++;
        }
        
        return lastSelectedCore = id;
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
    
    /**
     * Evaluates the CONS parameters.
     * 
     * @param time    time of evaluation.
    */
    public void evalCONSparameters( final Time time )
    {
        //System.out.println( "Evaluating at: " + time );
        computeIdleEnergy( time );
        evalCONSfrequency( time );
    }
    
    /**
     * Evaluates which is the "best" frequency
     * to process the remaining tasks for the CONS model.
     * 
     * @param time    time of evaluation.
    */
    private void evalCONSfrequency( final Time time )
    {
        CPUEnergyModel model = (CPUEnergyModel) getModel();
        
        // Evaluate the "best" frequency to use.
        for (Core core : coresMap.values()) {
            currentCoreId = core.getId();
            long frequency = model.eval( null );
            core.setFrequency( time, frequency );
        }
    }
    
    /**
     * Evaluates which is the "best" frequency
     * to process the remaining tasks.
     * 
     * @param time    time of evaluation.
     * @param core    core to which assign the evaluated frequency.
    */
    protected long evalFrequency( final Time time, final Core core )
    {
        Model<Long,QueryInfo> model = getModel();
        List<QueryInfo> queue = core.getQueue();
        
        if (core.getFirstQueryInQueue().getStartTime().getTimeMicroseconds() == 44709952703L)
            System.out.println( "EVALUATING FREQUENCY AT: " + time );
        
        // Evaluate the "best" frequency to use.
        return model.eval( time, queue.toArray( new QueryInfo[0] ) );
    }
    
    @Override
    public Time timeToCompute( final Task task )
    {
        Core core = coresMap.get( getLastSelectedCore() );
        QueryInfo query = core.getLastQueryInQueue();
        return computeTime( query, core );
    }
    
    private Time computeTime( final QueryInfo query, final Core core )
    {
        lastQuery = query;
        
        // If this is not the next query to compute, returns the maximum time (SIM_TIME - arrival).
        if (!core.isNextQuery( query )) {
            //Time time = new Time( Long.MAX_VALUE, TimeUnit.MICROSECONDS );
            Time time = _evtScheduler.getTimeDuration();
            query.setTimeToComplete( Time.ZERO, Time.ZERO );
            return time.subTime( query.getArrivalTime() );
        }
        
        long frequency = core.getFrequency();
        query.setFrequency( frequency );
        
        Time computeTime = query.getTime( frequency );
        Time startTime   = core.getTime();
        Time endTime     = startTime.clone().addTime( computeTime );
        query.setTimeToComplete( startTime, endTime );
        
        if (query.getStartTime().getTimeMicroseconds() == 44709952703L)
            System.out.println( "CORE: " + core.getId() + ", EXECUTING QUERY: " + query.getId() + ", START_TIME: " + startTime + ", COMPUTE_TIME: " + computeTime + ", END_TIME: " + endTime + ", QUEUE: " + core.getQueue() );
        
        computeEnergyConsumption( core, query, computeTime );
        core.setCompletedQuery( query );
        
        core.updateEventTime( query, query.getEndTime() );
        
        return computeTime;
    }
    
    private void computeEnergyConsumption( final Core core, final QueryInfo query, final Time computeTime )
    {
        long frequency = core.getFrequency();
        double energy = query.getEnergy( frequency );
        CPUEnergyModel model = (CPUEnergyModel) getModel();
        if (model.getType() == Type.CONS) {
            long time = query.getTime( frequency ).getTimeMicroseconds();
            double energyUnit = energy/time;
            energy = energyUnit * computeTime.getTimeMicroseconds();
        }
        
        energy = energyModel.computeEnergy( energy, frequency, computeTime, false );
        query.setEnergyConsumption( energy );
        
        core.addIdleEnergy( query.getStartTime(), true );
    }
    
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
    
    public static void writeResult( final long frequency, final double energy ) {
        coeffWriter.println( frequency + " " + energy + " " );
    }
    
    @Override
    public Double getResultSampled( final String sampler ) {
        // TODO RIMUOVERE QUESTO METODO DOPO I TEST
        coeffWriter.close();
        return super.getResultSampled( sampler );
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
    public String getID() {
        return "EnergyCPU";
    }
    
    @Override
    public String toString()
    {
        return "Cpu: " + _name + " @ " + _frequencies + "KHz\n" +
               "     " + _cores + " cores with " +
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
        protected EnergyCPU cpu;
        protected long idleTimeInterval = 0;
        private int queriesExecuted = 0;
        // Number of times this core has been selected in a tied situation.
        private int tieSelected = 0;
        
        // TODO implementare i context di ogni core della CPU (se proprio c'e' bisogno).
        
        
        public Core( final EnergyCPU cpu, final long coreId, final long initFrequency )
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
                                 endTime, tailLatency.getTimeMicroseconds() );
            
            cpu.addSampledValue( Global.ENERGY_SAMPLING, startTime,
                                 endTime, currentQuery.getEnergyConsumption() );
            
            writeResult( currentQuery.getFrequency(), currentQuery.getLastEnergy() );
            
            // Compute the current idle energy.
            addIdleEnergy( endTime, true );
            
            removeQuery( 0 );
            
            queriesExecuted++;
            currentQuery = null;
        }
        
        private void addIdleEnergy( final Time time, final boolean allCores )
        {
            double idleEnergy = 0;
            Time startTime = time.clone().subTime( getIdleTime(), TimeUnit.MICROSECONDS );
            if (!allCores) { // Only the current core.
                idleEnergy = getIdleEnergy();
            } else {
                for (Core core : cpu.coresMap.values()) {
                    idleEnergy += core.getIdleEnergy();
                }
            }
            
            cpu.addSampledValue( Global.ENERGY_SAMPLING, startTime, time, idleEnergy );
            cpu.addSampledValue( Global.IDLE_ENERGY_SAMPLING, startTime, time, idleEnergy );
        }
        
        protected void setFrequency( final Time time, final long newFrequency )
        {
            if (frequency != newFrequency) {
                System.out.println( "TIME_BUDGET: " + ((PESOSmodel) cpu.getModel()).getTimeBudget() );
                System.out.println( "TIME: " + time + ", NEW: " + newFrequency + ", OLD: " + frequency );
                if (currentQuery != null) {
                    /*if (newFrequency < frequency) {
                        // FIXME abbassamento frequenza PESOS.
                        try {
                            int val = 100 / 0;
                            System.out.println( "VAL: " + val );
                        } catch ( Exception e ){
                            //e.printStackTrace();
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
                    writeResult( frequency, currentQuery.getElapsedEnergy() );
                    this.time = currentQuery.getEndTime();
                    updateEventTime( currentQuery, this.time );
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
                long elapsedTime = time.getTimeMicroseconds() - this.time.getTimeMicroseconds();
                idleTime += elapsedTime;
                this.time.setTime( time );
            }
        }
        
        public Time getTime() {
            return time.clone();
        }

        public abstract boolean checkQueryCompletion( final Time time );
        
        private void updateEventTime( final QueryInfo query, final Time time )
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
        
        public abstract void addQuery( final QueryInfo q );
        
        public boolean isNextQuery( final QueryInfo query ) {
            return queryQueue.get( 0 ).equals( query );
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
    
    public static class PESOScore extends Core
    {
        private long baseTimeBudget;
        private long timeBudget;
        
        public PESOScore( final EnergyCPU cpu, final long coreId, final long initFrequency ) {
            super( cpu, coreId, initFrequency );
        }
        
        @Override
        public void addQuery( final QueryInfo q )
        {
            q.setCoreId( coreId );
            queryQueue.add( q );
            long frequency = cpu.evalFrequency( q.getArrivalTime(), this );
            setFrequency( q.getArrivalTime(), frequency );
        }
        
        @Override
        public boolean checkQueryCompletion( final Time time )
        {
            if (currentQuery != null && currentQuery.getEndTime().compareTo( time ) <= 0) {
                addQueryOnSampling();
                if (timeBudget != baseTimeBudget) {
                    timeBudget = baseTimeBudget;
                    PESOSmodel model = (PESOSmodel) cpu.getModel();
                    model.setTimeBudget( timeBudget );
                }
                
                if (hasMoreQueries()) {
                    long frequency = cpu.evalFrequency( time, this );
                    setFrequency( time, frequency );
                    cpu.computeTime( getFirstQueryInQueue(), this );
                }
                return true;
            } else {
                return false;
            }
        }
        
        public void setBaseTimeBudget( final long time )
        {
            baseTimeBudget = time;
            setTimeBudget( time, null );
        }
        
        public void setTimeBudget( final long time, final Long queryID )
        {
            timeBudget = time;
            System.out.println( "CORE: " + getId() + ", NUOVO TIME BUDGET: " + timeBudget );
            if (queryID != null && currentQuery != null && currentQuery.getId() == queryID) {
                PESOSmodel model = (PESOSmodel) cpu.getModel();
                model.setTimeBudget( timeBudget );
                if (hasMoreQueries()) {
                    Time t = new Time( time, TimeUnit.MICROSECONDS );
                    // FIXME quale delle 2 versioni e' la piu' corretta?? forse la prima va bene lo stesso
                    // FIXME perche' sono interessato alla velocita' in base al nuovo time budget.
                    long frequency = cpu.evalFrequency( currentQuery.getStartTime(), this );
                    //long frequency = cpu.evalFrequency( t, this );
                    setFrequency( t, frequency );
                }
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
                    writeResult( frequency, idleEnergy );
                } else {
                    // Set the minimum frequency if the time elapsed between two consecutive queries
                    // is at least QUEUE_CHECK microseconds.
                    long timeAtMaxFreq = QUEUE_CHECK - idleTimeInterval;
                    idleEnergy = cpu.energyModel.getIdleEnergy( frequency, timeAtMaxFreq );
                    writeResult( frequency, idleEnergy );
                    
                    frequency = cpu.getMinFrequency();
                    double energy = cpu.energyModel.getIdleEnergy( frequency, idleTime - timeAtMaxFreq );
                    idleEnergy += energy;
                    writeResult( frequency, energy );
                    
                    idleTimeInterval = 0;
                }
                
                idleTime = 0;
            }
            
            return idleEnergy;
        }
    }
    
    private static class PERFcore extends Core
    {
        public PERFcore( final EnergyCPU cpu, final long coreId, final long initFrequency )
        {
            super( cpu, coreId, initFrequency );
        }
        
        @Override
        public void addQuery( final QueryInfo q )
        {
            q.setCoreId( coreId );
            queryQueue.add( q );
        }
        
        @Override
        public boolean checkQueryCompletion( final Time time )
        {
            if (currentQuery != null && currentQuery.getEndTime().compareTo( time ) <= 0) {
                addQueryOnSampling();
                if (hasMoreQueries()) {
                    cpu.computeTime( getFirstQueryInQueue(), this );
                }
                return true;
            } else {
                return false;
            }
        }
        
        @Override
        public double getIdleEnergy()
        {
            double idleEnergy = 0;
            if (idleTime > 0) {
                idleEnergy = cpu.energyModel.getIdleEnergy( frequency, idleTime );
                idleTimeInterval += idleTime;
                writeResult( frequency, idleEnergy );
                idleTime = 0;
            }
            return idleEnergy;
        }
    }
    
    protected static class CONScore extends Core
    {
        private double receivedQueries;
        private double processedQueries;
        private double cumulativeTime;
        
        public CONScore( final EnergyCPU cpu, final long coreId, final long initFrequency )
        {
            super( cpu, coreId, initFrequency );
        }
        
        @Override
        public void addQuery( final QueryInfo q )
        {
            q.setCoreId( coreId );
            queryQueue.add( q );
            receivedQueries++;
        }
        
        @Override
        public boolean checkQueryCompletion( final Time time )
        {
            if (currentQuery != null && currentQuery.getEndTime().compareTo( time ) <= 0) {
                processedQueries++;
                cumulativeTime += currentQuery.getCompletionTime()/1000;
                
                addQueryOnSampling();
                if (hasMoreQueries()) {
                    cpu.computeTime( getFirstQueryInQueue(), this );
                }
                return true;
            } else {
                return false;
            }
        }
        
        public double getArrivalRate() {
            return receivedQueries / CONSmodel.PERIOD; //in ms!
        }
        
        public double getServiceRate()
        {
            double serviceRate;
            if (cumulativeTime == 0) {
                if (processedQueries != 0) {
                    serviceRate = Double.MAX_VALUE;
                } else {
                    serviceRate = 0;
                }
            } else {
                serviceRate = processedQueries / cumulativeTime; //in ms!
            }
            
            return serviceRate;
        }
        
        /** Method used by the cons model. */
        public void reset()
        {
            receivedQueries = 0;
            processedQueries = 0;
            cumulativeTime = 0;
        }
        
        @Override
        public double getIdleEnergy()
        {
            double idleEnergy = 0;
            if (idleTime > 0) {
                idleEnergy = cpu.energyModel.getIdleEnergy( frequency, idleTime );
                idleTimeInterval += idleTime;
                writeResult( frequency, idleEnergy );
                idleTime = 0;
            }
            return idleEnergy;
        }
    }
}