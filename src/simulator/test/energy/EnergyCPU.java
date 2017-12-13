/**
 * @author Stefano Ceccotti
*/

package simulator.test.energy;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import simulator.core.Model;
import simulator.core.Task;
import simulator.test.energy.CPUModel.CONSmodel;
import simulator.test.energy.CPUModel.LOAD_SENSITIVEmodel;
import simulator.test.energy.CPUModel.MY_model;
import simulator.test.energy.CPUModel.PESOSmodel;
import simulator.test.energy.CPUModel.QueryInfo;
import simulator.test.energy.CPUModel.Type;
import simulator.test.energy.EnergyModel.QueryEnergyModel;
import simulator.utils.Time;
import simulator.utils.Utils;

public class EnergyCPU extends CPU
{
    // TODO File usato per ottenere i coefficienti
    protected static PrintWriter coeffWriter;
    
    
    
    
    public EnergyCPU( String specFile ) throws IOException {
        super( specFile );
    }
    
    public EnergyCPU( String machine, int cores, int contexts,
                      String frequencies_file ) throws IOException {
        this( machine, cores, contexts, readFrequencies( frequencies_file ) );
    }
    
    public EnergyCPU( String machine, int cores, int contexts,
                      List<Long> frequencies ) throws IOException
    {
        super( machine, frequencies );
        
        _cores = Math.max( 1, cores );
        _contexts = contexts;
        
        setFrequency( getMaxFrequency() );
        coresMap = new HashMap<>( (int) _cores );
        
        setMaxPower( 84d );
        setMinPower(  2d );
        
        setEnergyModel( new QueryEnergyModel( cores ) );
        //setEnergyModel( new CoefficientEnergyModel( cores ) );
        //setEnergyModel( new NormalizedEnergyModel( cores ) );
        //setEnergyModel( new ParameterEnergyModel( cores ) );
    }
    
    @Override
    public void setModel( Model<QueryInfo,Long> model )
    {
        // TODO RIMUOVERE IL WRITER DOPO I TEST
        if (coeffWriter != null)
            coeffWriter.close();
        
        CPUModel cpuModel = (CPUModel) model;
        try {
            String file = null;
            long timeBudget;
            switch (cpuModel.getType()) {
                case PERF    : file = "PERF"; break;
                case PESOS   :
                    timeBudget = cpuModel.getTimeBudget().getTimeMillis();
                    file = "PESOS_" + timeBudget + "_" + cpuModel.getMode();
                    break;
                case CONS           : file = "CONS"; break;
                case LOAD_SENSITIVE :
                    timeBudget = cpuModel.getTimeBudget().getTimeMillis();
                    file = "LOAD_SENSITIVE_" + timeBudget; break;
                case MY_MODEL :
                    timeBudget = cpuModel.getTimeBudget().getTimeMillis();
                    file = "MY_MODEL_" + timeBudget; break;
                case PEGASUS        : file = "PEGASUS"; break;
            }
            Utils.checkDirectory( "Results/Coefficients" );
            file += (_cores == 1) ? "_distr" : "_mono";
            coeffWriter = new PrintWriter( "Results/Coefficients/" + file + "_Freq_Energy.txt", "UTF-8" );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
        
        for (long i = 0; i < _cores; i++) {
            switch (cpuModel.getType()) {
                case PESOS :
                    PESOScore core = new PESOScore( this, i, getMaxFrequency() );
                    core.setBaseTimeBudget( getTime(), cpuModel.getTimeBudget().getTimeMicros() );
                    coresMap.put( i, core );
                    break;
                case PERF           : coresMap.put( i, new PERFcore( this, i, getMaxFrequency() ) ); break;
                case CONS           : coresMap.put( i, new CONScore( this, i, getMaxFrequency() ) ); break;
                case LOAD_SENSITIVE : coresMap.put( i, new LOAD_SENSITIVEcore( this, i, getMaxFrequency() ) ); break;
                case MY_MODEL       : coresMap.put( i, new MY_MODELcore( this, i, getMaxFrequency() ) ); break;
                case PEGASUS        : coresMap.put( i, new PEGASUScore( this, i, getMaxFrequency() ) ); break;
            }
        }
        
        super.setModel( model );
        model.setDevice( this );
    }
    
    @Override
    public void addQuery( long coreId, QueryInfo q )
    {
        Core core = coresMap.get( coreId );
        if (centralizedQueue) {
            if (core.getQueue().isEmpty()) {
                core.addQuery( q, false );
                removeQuery( q );
            }
            lastQuery = q;
        } else {
            //System.out.println( "SELEZIONATO CORE: " + coreId + ", TIME: " + core.getTime() );
            core.addQuery( q, true );
        }
    }
    
    private void removeQuery( QueryInfo q )
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
    public long selectCore( Time time, QueryInfo query )
    {
        if (centralizedQueue) {
            //System.out.println( "AGGIUNTA QUERY: " + query.getId() );
            QueryReference toAssign = new QueryReference( -1, query );
            queries.add( toAssign );
            analyzeFrequency( time, -1 );
            //System.out.println( "TIME: " + time + ", AGGIUNTA QUERY AL CORE: " + toAssign.getCoreId() + ", PROSSIMA: " + getCore( toAssign.getCoreId() ).getFirstQueryInQueue() );
            return lastSelectedCore = toAssign.getCoreId();
        } else {
            getAgent().addSampledValue( Global.QUERY_PER_TIME_SLOT, time, time, 1 );
            return ((CPUModel) _model).selectCore( time, this, query );
        }
    }
    
    @Override
    public void evalCONSparameters( Time time )
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
    private void evalCONSfrequency( Time time )
    {
        CPUModel model = (CPUModel) getModel();
        
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
    protected long evalFrequency( Time time, Core core )
    {
        Model<QueryInfo,Long> model = getModel();
        List<QueryInfo> queue = core.getQueue();
        
        // Evaluate the "best" frequency to use.
        return model.eval( time, queue.toArray( new QueryInfo[0] ) );
    }
    
    @Override
    public Time timeToCompute( Task task )
    {
        Core core = coresMap.get( getLastSelectedCore() );
        QueryInfo query;
        if (centralizedQueue) {
            query = lastQuery;
        } else {
            query = core.getLastQueryInQueue();
        }
        return computeTime( query, core );
    }
    
    @Override
    protected void computeEnergyConsumption( Core core, QueryInfo query, Time computeTime )
    {
        long frequency = core.getFrequency();
        double energy = query.getEnergy( frequency );
        CPUModel model = (CPUModel) getModel();
        if (model.getType() == Type.CONS) {
            long time = query.getTime( frequency ).getTimeMicros();
            double energyUnit = energy/time;
            energy = energyUnit * computeTime.getTimeMicros();
        }
        
        energy = energyModel.computeEnergy( energy, frequency, computeTime, false );
        query.setEnergyConsumption( energy );
        
        core.addIdleEnergy( query.getStartTime(), true );
    }
    
    // TODO rimuovere dopo i test
    public static void writeResult( long frequency, double energy ) {
        coeffWriter.println( frequency + " " + energy + " " );
    }
    
    @Override
    public void shutdown() throws IOException
    {
        coeffWriter.close();
        super.shutdown();
    }
    
    
    
    public static class PESOScore extends Core
    {
        private long baseTimeBudget;
        private long timeBudget;
        
        private long queryExecutionTime = 0;
        
        private static final long QUEUE_CHECK = TimeUnit.SECONDS.toMicros( 1 );
        
        
        public PESOScore( EnergyCPU cpu, long coreId, long initFrequency ) {
            super( cpu, coreId, initFrequency );
        }
        
        @Override
        public void addQuery( QueryInfo q, boolean updateFrequency )
        {
            if (cpu.centralizedQueue) {
                if (q != null) {
                    q.setCoreId( coreId );
                    queryQueue.add( q );
                    // Here has the meaning of executing the query.
                    if (updateFrequency) {
                        cpu.computeTime( q, this );
                    }
                }
            } else {
                q.setCoreId( coreId );
                queryQueue.add( q );
                long frequency = cpu.evalFrequency( q.getArrivalTime(), this );
                if (updateFrequency) {
                    PESOSmodel model = (PESOSmodel) cpu.getModel();
                    final int postings = q.getPostings() + model.getRMSE( q.getTerms() );
                    queryExecutionTime += model.predictServiceTimeAtMaxFrequency( q.getTerms(), postings );
                    
                    setFrequency( q.getArrivalTime(), frequency );
                } else {
                    this.frequency = frequency;
                }
            }
        }
        
        @Override
        public boolean checkQueryCompletion( Time time )
        {
            if (currentQuery != null && currentQuery.isComplete( time )) {
                //long queryId = currentQuery.getId();
                //System.out.println( "TIME: " + time + ", CORE: " + getId() + ", COMPLETATA QUERY: " + currentQuery );
                PESOSmodel model = (PESOSmodel) cpu.getModel();
                final int postings = currentQuery.getPostings() + model.getRMSE( currentQuery.getTerms() );
                queryExecutionTime -= model.predictServiceTimeAtMaxFrequency( currentQuery.getTerms(), postings );
                
                if (timeBudget != baseTimeBudget) {
                    timeBudget = baseTimeBudget;
                    //PESOSmodel model = (PESOSmodel) cpu.getModel();
                    model.setTimeBudget( timeBudget );
                }
                
                addQueryOnSampling( time, true );
                
                if (cpu.centralizedQueue) {
                    cpu.analyzeFrequency( time, getId() );
                    currentQuery = cpu.getNextQuery( time, getId() );
                    //System.out.println( "PROSSIMA QUERY: " + currentQuery );
                    if (currentQuery != null) {
                        addQuery( currentQuery, false );
                        cpu.computeTime( currentQuery, this );
                    }
                } else {
                    if (hasMoreQueries()) {
                        cpu.computeTime( getFirstQueryInQueue(), this );
                    }/* else {
                        startJobStealing( time );
                    }*/
                }
                
                return true;
            } else {
                return false;
            }
        }
        
        public void setBaseTimeBudget( Time time, long timeBudget )
        {
            baseTimeBudget = timeBudget;
            setTimeBudget( time, timeBudget, null );
        }
        
        public void setTimeBudget( Time time, long timeBudget, Long queryID )
        {
            this.timeBudget = timeBudget;
            //System.out.println( "CORE: " + getId() + ", NUOVO TIME BUDGET: " + timeBudget );
            if (queryID != null && currentQuery != null && currentQuery.getId() == queryID) {
                PESOSmodel model = (PESOSmodel) cpu.getModel();
                model.setTimeBudget( timeBudget );
                long frequency = cpu.evalFrequency( currentQuery.getStartTime(), this );
                //long frequency = cpu.evalFrequency( t, this );
                setFrequency( time, frequency );
            }
        }
        
        public long getQueryExecutionTime( Time time )
        {
            if (currentQuery != null && currentQuery.getEndTime().compareTo( time ) <= 0) {
                PESOSmodel model = (PESOSmodel) cpu.getModel();
                final int postings = currentQuery.getPostings() + model.getRMSE( currentQuery.getTerms() );
                long predictedTime = model.predictServiceTimeAtMaxFrequency( currentQuery.getTerms(), postings );
                return queryExecutionTime - predictedTime;
            }
            
            return queryExecutionTime;
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
        public PERFcore( EnergyCPU cpu, long coreId, long initFrequency ) {
            super( cpu, coreId, initFrequency );
        }
        
        @Override
        public void addQuery( QueryInfo q, boolean execute )
        {
            if (cpu.centralizedQueue) {
                if (q != null) {
                    q.setCoreId( coreId );
                    queryQueue.add( q );
                    if (execute) {
                        cpu.computeTime( q, this );
                    }
                }
            } else {
                q.setCoreId( coreId );
                queryQueue.add( q );
            }
        }
        
        @Override
        public boolean checkQueryCompletion( Time time )
        {
            if (currentQuery != null && currentQuery.isComplete( time )) {
                //long queryId = currentQuery.getId();
                addQueryOnSampling( time, false );
                if (cpu.centralizedQueue) {
                    currentQuery = cpu.getNextQuery( time, getId() );
                    //System.out.println( "PROSSIMA QUERY: " + currentQuery );
                    if (currentQuery != null) {
                        addQuery( currentQuery, false );
                        cpu.computeTime( currentQuery, this );
                    }
                } else {
                    if (hasMoreQueries()) {
                        cpu.computeTime( getFirstQueryInQueue(), this );
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }
    
    protected static class CONScore extends Core
    {
        private double receivedQueries;
        private double processedQueries;
        private double cumulativeTime;
        
        public CONScore( EnergyCPU cpu, long coreId, long initFrequency ) {
            super( cpu, coreId, initFrequency );
        }
        
        @Override
        public void addQuery( QueryInfo q, boolean updateFrequency )
        {
            if (cpu.centralizedQueue) {
                if (q != null) {
                    q.setCoreId( coreId );
                    queryQueue.add( q );
                    // Here has the meaning of execute the query.
                    if (updateFrequency) {
                        cpu.computeTime( q, this );
                    }
                }
            } else {
                q.setCoreId( coreId );
                queryQueue.add( q );
            }
            receivedQueries++;
        }
        
        @Override
        public boolean checkQueryCompletion( Time time )
        {
            if (currentQuery != null && currentQuery.isComplete( time )) {
                //long queryId = currentQuery.getId();
                processedQueries++;
                cumulativeTime += currentQuery.getCompletionTime()/1000;
                
                addQueryOnSampling( time, false );
                
                if (cpu.centralizedQueue) {
                    currentQuery = cpu.getNextQuery( time, getId() );
                    //System.out.println( "PROSSIMA QUERY: " + currentQuery );
                    if (currentQuery != null) {
                        addQuery( currentQuery, false );
                        cpu.computeTime( currentQuery, this );
                    }
                } else {
                    if (hasMoreQueries()) {
                        cpu.computeTime( getFirstQueryInQueue(), this );
                    }
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
        
        public void reset()
        {
            receivedQueries = 0;
            processedQueries = 0;
            cumulativeTime = 0;
        }
    }
    
    public static class LOAD_SENSITIVEcore extends Core
    {
        private long queryExecutionTime = 0;
        
        public LOAD_SENSITIVEcore( CPU cpu, long coreId, long initFrequency ) {
            super( cpu, coreId, initFrequency );
        }
        
        @Override
        public void addQuery( QueryInfo q, boolean updateFrequency )
        {
            if (cpu.centralizedQueue) {
                if (q != null) {
                    q.setCoreId( coreId );
                    queryQueue.add( q );
                    // Here has the meaning of execute the query.
                    if (updateFrequency) {
                        cpu.computeTime( q, this );
                    }
                }
            } else {
                q.setCoreId( coreId );
                queryQueue.add( q );
                long frequency = cpu.evalFrequency( q.getArrivalTime(), this );
                if (updateFrequency) {
                    LOAD_SENSITIVEmodel model = (LOAD_SENSITIVEmodel) cpu.getModel();
                    final int postings = q.getPostings() + model.getRMSE( q.getTerms() );
                    queryExecutionTime += model.predictServiceTimeAtMaxFrequency( q.getTerms(), postings );
                    
                    setFrequency( q.getArrivalTime(), frequency );
                } else {
                    this.frequency = frequency;
                }
            }
        }

        @Override
        public boolean checkQueryCompletion( Time time )
        {
            if (currentQuery != null && currentQuery.isComplete( time )) {
                LOAD_SENSITIVEmodel model = (LOAD_SENSITIVEmodel) cpu.getModel();
                final int postings = currentQuery.getPostings() + model.getRMSE( currentQuery.getTerms() );
                queryExecutionTime -= model.predictServiceTimeAtMaxFrequency( currentQuery.getTerms(), postings );
                
                addQueryOnSampling( time, true );
                
                if (cpu.centralizedQueue) {
                    cpu.analyzeFrequency( time, getId() );
                    currentQuery = cpu.getNextQuery( time, getId() );
                    //System.out.println( "PROSSIMA QUERY: " + currentQuery );
                    if (currentQuery != null) {
                        addQuery( currentQuery, false );
                        cpu.computeTime( currentQuery, this );
                    }
                } else {
                    if (hasMoreQueries()) {
                        cpu.computeTime( getFirstQueryInQueue(), this );
                    }/* else {
                        startJobStealing( time );
                    }*/
                }
                return true;
            } else {
                return false;
            }
        }
        
        public long getQueryExecutionTime( Time time )
        {
            if (currentQuery != null && currentQuery.getEndTime().compareTo( time ) <= 0) {
                LOAD_SENSITIVEmodel model = (LOAD_SENSITIVEmodel) cpu.getModel();
                final int postings = currentQuery.getPostings() + model.getRMSE( currentQuery.getTerms() );
                long predictedTime = model.predictServiceTimeAtMaxFrequency( currentQuery.getTerms(), postings );
                return queryExecutionTime - predictedTime;
            }
            
            return queryExecutionTime;
        }
    }
    
    public static class MY_MODELcore extends Core
    {
        private long queryExecutionTime = 0;
        
        public MY_MODELcore( EnergyCPU cpu, long coreId, long initFrequency ) {
            super( cpu, coreId, initFrequency );
        }
        
        @Override
        public void addQuery( QueryInfo q, boolean updateFrequency )
        {
            if (cpu.centralizedQueue) {
                if (q != null) {
                    q.setCoreId( coreId );
                    queryQueue.add( q );
                    // Here has the meaning of execute the query.
                    if (updateFrequency) {
                        cpu.computeTime( q, this );
                    }
                }
            } else {
                q.setCoreId( coreId );
                queryQueue.add( q );
                long frequency = cpu.evalFrequency( q.getArrivalTime(), this );
                if (updateFrequency) {
                    MY_model model = (MY_model) cpu.getModel();
                    final int postings = q.getPostings() + model.getRMSE( q.getTerms() );
                    queryExecutionTime += model.predictServiceTimeAtMaxFrequency( q.getTerms(), postings );
                    
                    setFrequency( q.getArrivalTime(), frequency );
                } else {
                    this.frequency = frequency;
                }
            }
        }
        
        @Override
        public boolean checkQueryCompletion( Time time )
        {
            if (currentQuery != null && currentQuery.isComplete( time )) {
                //long queryId = currentQuery.getId();
                //System.out.println( "TIME: " + time + ", CORE: " + getId() + ", COMPLETATA QUERY: " + currentQuery );
                MY_model model = (MY_model) cpu.getModel();
                final int postings = currentQuery.getPostings() + model.getRMSE( currentQuery.getTerms() );
                queryExecutionTime -= model.predictServiceTimeAtMaxFrequency( currentQuery.getTerms(), postings );
                
                addQueryOnSampling( time, true );
                
                if (cpu.centralizedQueue) {
                    cpu.analyzeFrequency( time, getId() );
                    currentQuery = cpu.getNextQuery( time, getId() );
                    //System.out.println( "PROSSIMA QUERY: " + currentQuery );
                    if (currentQuery != null) {
                        addQuery( currentQuery, false );
                        cpu.computeTime( currentQuery, this );
                    }
                } else {
                    if (hasMoreQueries()) {
                        cpu.computeTime( getFirstQueryInQueue(), this );
                    }/* else {
                        startJobStealing( time );
                    }*/
                }
                return true;
            } else {
                return false;
            }
        }
        
        public long getQueryExecutionTime( Time time )
        {
            if (currentQuery != null && currentQuery.getEndTime().compareTo( time ) <= 0) {
                MY_model model = (MY_model) cpu.getModel();
                final int postings = currentQuery.getPostings() + model.getRMSE( currentQuery.getTerms() );
                long predictedTime = model.predictServiceTimeAtMaxFrequency( currentQuery.getTerms(), postings );
                return queryExecutionTime - predictedTime;
            }
            
            return queryExecutionTime;
        }
    }

    private static class PEGASUScore extends Core
    {

        public PEGASUScore( CPU cpu, long coreId, long initFrequency ) {
            super( cpu, coreId, initFrequency );
        }
        
        @Override
        public void addQuery( QueryInfo q, boolean execute )
        {
            if (cpu.centralizedQueue) {
                if (q != null) {
                    q.setCoreId( coreId );
                    queryQueue.add( q );
                    if (execute) {
                        cpu.computeTime( q, this );
                    }
                }
            } else {
                q.setCoreId( coreId );
                queryQueue.add( q );
            }
        }
        
        @Override
        public boolean checkQueryCompletion( Time time )
        {
            if (currentQuery != null && currentQuery.isComplete( time )) {
                addQueryOnSampling( time, false );
                
                if (cpu.centralizedQueue) {
                    currentQuery = cpu.getNextQuery( time, getId() );
                    //System.out.println( "PROSSIMA QUERY: " + currentQuery );
                    if (currentQuery != null) {
                        addQuery( currentQuery, false );
                        cpu.computeTime( currentQuery, this );
                    }
                } else {
                    if (hasMoreQueries()) {
                        cpu.computeTime( getFirstQueryInQueue(), this );
                    }
                }
                return true;
            } else {
                return false;
            }
        }
    }
}
