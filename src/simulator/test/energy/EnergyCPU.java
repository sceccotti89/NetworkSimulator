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
import simulator.test.energy.CPUEnergyModel.Mode;
import simulator.test.energy.CPUEnergyModel.QueryInfo;
import simulator.test.energy.EnergyModel.*;
import simulator.utils.Time;
import simulator.utils.Utils;

public class EnergyCPU extends Device<Long,QueryInfo>
{
    protected double _cores;
    protected int _contexts;
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
        
        coresMap = new HashMap<>( cores );
        for (long i = 0; i < cores; i++) {
            coresMap.put( i, new Core( this, i, getMaxFrequency() ) );
        }
        
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
        
        try {
            CPUEnergyModel cpuModel = (CPUEnergyModel) model;
            cpuModel.setCPU( this );
            if (cpuModel.getMode() == null) {
                coeffWriter = new PrintWriter( "Results/Coefficients/PERF_Freq_Energy.txt", "UTF-8" );
            } else {
                String file;
                if (cpuModel.getMode() == Mode.PESOS_ENERGY_CONSERVATIVE || cpuModel.getMode() == Mode.PESOS_TIME_CONSERVATIVE) {
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
        
        super.setModel( model );
    }
    
    public void setEnergyModel( final EnergyModel model ) {
        energyModel = model;
    }
    
    public void addQuery( final long coreId, final QueryInfo q )
    {
        Core core = coresMap.get( coreId );
        core.addQuery( q );
        
        //System.out.println( "AGGIUNTA QUERY " + q.getId() + " TO CORE: " + coreId );
        
        // Evaluate the new core frequency.
        evalFrequency( q.getArrivalTime(), core );
    }
    
    @Override
    public void setTime( final Time time )
    {
        // Set the time of the CPU cores.
        for (Core cpuCore : coresMap.values()) {
            cpuCore.setTime( time.clone() );
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
        double utilization = Double.MAX_VALUE;
        for (Core core : coresMap.values()) {
            //System.out.println( "CORE: " + core.getId() + ", UTILIZATION: " + core.getUtilization( time ) );
            double coreUtilization = core.getUtilization( time );
            if (coreUtilization < utilization) {
                id = core.getId();
                utilization = coreUtilization;
            }
        }
        //System.out.println( "SELECTED: " + id );
        return lastSelectedCore = id;
    }
    
    public long getLastSelectedCore() {
        return lastSelectedCore;
    }
    
    public Core getCore( final long index ) {
        return coresMap.get( index );
    }
    
    /**
     * Evaluate which is the "best" frequency to use
     * to process the remaining tasks.
     * 
     * @param time    time of evaluation.
     * @param core    core to which assign the evaluated frequency.
    */
    private void evalFrequency( final Time time, final Core core )
    {
        Model<Long,QueryInfo> model = getModel();
        List<QueryInfo> queue = core.getQueue();
        
        // Set the time of the cores as (at least) the last query arrival time.
        QueryInfo query = queue.get( queue.size() - 1 );
        setTime( query.getArrivalTime() );
        
        if (core.getFirstQueryInQueue().getStartTime().getTimeMicroseconds() == 44709952703L)
            System.out.println( "EVALUATING FREQUENCY AT: " + time );
        
        // Evaluate the "best" frequency to use.
        long frequency = model.eval( time, queue.toArray( new QueryInfo[0] ) );
        //System.out.println( "QUERY: " + core.lastQuery + ", OLD_FREQ: " + core.getFrequency() + ", NEW FREQ: " + frequency );
        core.setFrequency( time, frequency );
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
        
        // If this is not the next query to compute, returns the maximum time (MAX - arrival).
        if (!core.isNextQuery( query )) {
            Time time = new Time( Long.MAX_VALUE, TimeUnit.MICROSECONDS );
            query.setTimeToComplete( Time.ZERO, time.clone() );
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
        
        computeEnergyConsumption( core, query );
        core.setCompletedQuery( query );
        
        core.updateEventTime( query, query.getEndTime() );
        
        return computeTime;
    }
    
    private void computeEnergyConsumption( final Core core, final QueryInfo query )
    {
        long frequency = core.getFrequency();
        double energy = query.getEnergy( frequency );
        
        // Subtract the idle energy spent by the other cores during the execution of the query.
        Time computeTime = query.getTime( frequency );
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
    
    public void computeIdleEnergy( final Time time ) {
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
        // TODO RIMUOVERE LA PROSSIMA RIGA DOPO I TEST
        coeffWriter.close();
        return super.getResultSampled( sampler );
    }
    
    public int getExecutedQueries() {
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
    public static class Core
    {
        private long coreId;
        private Time time;
        private List<QueryInfo> queryQueue;
        private long frequency;
        private QueryInfo currentQuery = null;
        private long idleTime = 0;
        private EnergyCPU cpu;
        private long idleTimeInterval = 0;
        private int queriesExecuted = 0;
        
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
        
        private void addQueryOnSampling()
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
        
        public void setFrequency( final Time time, final long newFrequency )
        {
            if (frequency != newFrequency) {
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
                this.time = time;
            }
        }
        
        public void checkQueryCompletion( final Time time )
        {
            if (currentQuery != null && currentQuery.getEndTime().compareTo( time ) <= 0) {
                addQueryOnSampling();
                if (hasMoreQueries()) {
                    cpu.evalFrequency( time, this );
                    cpu.computeTime( getFirstQueryInQueue(), this );
                }
            }
        }
        
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
        
        public Time getTime() {
            return time.clone();
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
        
        public void addQuery( final QueryInfo q ) {
            q.setCoreId( coreId );
            queryQueue.add( q );
        }
        
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
        
        public double getIdleEnergy()
        {
            double idleEnergy = 0;
            if (idleTime > 0) {
                if (((CPUEnergyModel) cpu.getModel()).getMode() == null || idleTimeInterval + idleTime < QUEUE_CHECK) {
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
        
        public double getUtilization( final Time time )
        {
            double size = queryQueue.size();
            if (size == 0) return 0;
            if (queryQueue.get( 0 ).getEndTime().compareTo( time ) <= 0) size--;
            return size;
        }
    }
}