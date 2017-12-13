/**
 * @author Stefano Ceccotti
*/

package simulator.test.energy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import simulator.core.Model;
import simulator.events.Event;
import simulator.test.energy.CPU.Core;
import simulator.test.energy.CPUModel.QueryInfo;
import simulator.test.energy.EnergyCPU.CONScore;
import simulator.test.energy.EnergyCPU.LOAD_SENSITIVEcore;
import simulator.test.energy.EnergyCPU.MY_MODELcore;
import simulator.utils.Pair;
import simulator.utils.Time;
import simulator.utils.Utils;
import simulator.utils.resources.ResourceLoader;

public abstract class CPUModel extends Model<QueryInfo,Long> implements Cloneable
{
    private static final String POSTINGS_PREDICTORS   = "predictions.txt";
    //private static final String EFFECTIVE_TIME_ENERGY = "time_energy.txt";
    private static final String EFFECTIVE_TIME_ENERGY = "time_energy_fit.txt";
    
    private static final String SEPARATOR = "=";
    
    protected String _directory;
    protected String _postings;
    protected String _effective_time_energy;
    protected String _regressors;
    
    // Time limit to complete a query.
    protected Time timeBudget;
    
    // List of query infos.
    private Map<Long,QueryInfo> queries;
    
    // Map used to store the predictors for the evaluation of the "best" frequency.
    protected Map<String,Double> regressors;
    
    private static final Long[] FREQUENCIES = new Long[] { 3500000L, 3300000L, 3100000L, 2900000L,
                                                           2700000L, 2500000L, 2300000L, 2100000L,
                                                           2000000L, 1800000L, 1600000L, 1400000L,
                                                           1200000L, 1000000L,  800000L };
    
    // Energy evaluation mode.
    protected Type type;
    
    
    /**
     * Types of energy model:
     * <p><lu>
     * <li>PESOS
     * <li>PERF
     * <li>CONS
     * </lu>
    */
    public enum Type
    {
        PESOS( "PESOS" ),
        PERF( "PERF" ),
        CONS( "CONS" ),
        LOAD_SENSITIVE( "LOAD_SENSITIVE" ),
        PEGASUS( "PEGASUS" ),
        MY_MODEL( "MY_MODEL" );
        
        private Mode mode;
        private String name;
        
        private Type( String name ) {
            this.name = name;
        }
        
        public void setMode( Mode mode ) {
            this.mode = mode;
        }
        
        public Mode getMode() {
            return mode;
        }
        
        @Override
        public String toString() {
            return name;
        }
    }
    
    /**
     * Model modality:
     * <p><lu>
     * <li>TIME_CONSERVATIVE:   consumes more energy, reducing the tail latency.
     * <li>ENERGY_CONSERVATIVE: consumes less energy at a higher tail latency.
     * </lu>
    */
    public enum Mode
    {
        TIME_CONSERVATIVE( "TC" ),
        ENERGY_CONSERVATIVE( "EC" );
        
        private final String name;
        private Mode( String name ) {
            this.name = name;
        }
        
        @Override
        public String toString() {
            return this.name;
        }
    };
    
    
    
    
    /**
     * Creates a new energy model.
     * 
     * @param type     type of model (see {@linkplain CPUModel.Type Type}).
     * @param dir      directory used to load the fiels.
     * @param files    list of files used to load the model.
     * 
     * @throws IOException if a file doesn't exists or is malformed.
    */
    public CPUModel( Type type, String dir, String... files )
    {
        this.type = type;
        
        _directory = dir;
        _postings = _directory + files[0];
        _effective_time_energy = _directory + files[1];
        if (files.length == 3) {
            _regressors = _directory + files[2];
        }
    }
    
    @Override
    public void loadModel() throws IOException
    {
        if (_regressors != null) {
            loadRegressors();
        }
        loadPostingsPredictors();
        loadEffectiveTimeEnergy();
    }
    
    public abstract String getModelType( boolean delimeters );
    
    public Type getType() {
        return type;
    }
    
    public Mode getMode() {
        return type.getMode();
    }

    public Time getTimeBudget() {
        return timeBudget;
    }
    
    public void setTimeBudget( long time ) {
        timeBudget = new Time( time, TimeUnit.MICROSECONDS );
    }

    private void loadPostingsPredictors() throws IOException
    {
        queries = new HashMap<>( 1 << 14 );
        
        InputStream fReader = ResourceLoader.getResourceAsStream( _postings );
        BufferedReader predictorReader = new BufferedReader( new InputStreamReader( fReader ) );
        
        String line = null;
        while ((line = predictorReader.readLine()) != null) {
            String[] values = line.split( "\\t+" );
            long queryID    = Long.parseLong( values[0] );
            int terms       = Integer.parseInt( values[1] );
            int postings    = Integer.parseInt( values[2] );
            QueryInfo query = new QueryInfo( queryID, terms, postings );
            queries.put( queryID, query );
        }
        
        predictorReader.close();
        fReader.close();
    }
    
    private void loadRegressors() throws IOException
    {
        InputStream fReader = ResourceLoader.getResourceAsStream( _regressors );
        BufferedReader regressorReader = new BufferedReader( new InputStreamReader( fReader ) );
        
        regressors = new HashMap<>();
        
        String line;
        while ((line = regressorReader.readLine()) != null) {
            String[] values = line.split( SEPARATOR );
            regressors.put( values[0], Double.parseDouble( values[1] ) );
        }
        
        regressorReader.close();
        fReader.close();
    }
    
    private void loadEffectiveTimeEnergy() throws IOException
    {
        InputStream fReader = ResourceLoader.getResourceAsStream( _effective_time_energy );
        BufferedReader reader = new BufferedReader( new InputStreamReader( fReader ) );
        
        String line;
        while ((line = reader.readLine()) != null) {
            String[] values = line.split( "\\s+" );
            
            long queryID = (long) Double.parseDouble( values[0] );
            QueryInfo query = queries.get( queryID );
            int index = 0;
            for (int i = 1; i < values.length; i+=2) {
                double qTime  = Double.parseDouble( values[i] ); // in ms.
                double energy = Double.parseDouble( values[i+1] );
                //double watt = energy / (qTime / 1000);
                double Ps = (EnergyModel.Ps / 1000) * qTime;
                if (energy <= Ps) {
                    System.out.println( "ID: " + queryID + ", FREQ: " + (i/2) + ", J: " + energy + ", Ps: " + Ps );
                }
                energy -= Ps;
                long time = Utils.getTimeInMicroseconds( qTime, TimeUnit.MILLISECONDS );
                query.setTimeAndEnergy( FREQUENCIES[index++], time, energy );
            }
        }
        
        reader.close();
        fReader.close();
    }

    public QueryInfo getQuery( long queryID ) {
        return queries.get( queryID ).clone();
    }
    
    /**
     * Returns the "best" available core to assign the next task.</br>
     * By default the core with the minor number of queries in the queue is chosen.
     * 
     * @param time    time of evaluation.
     * @param cpu     the associated cpu.
     * @param q       the query to add.
    */
    public long selectCore( Time time, EnergyCPU cpu, QueryInfo q )
    {
        long id = -1;
        double utilization = Integer.MAX_VALUE;
        long tiedSelection = Long.MAX_VALUE;
        boolean tieSituation = false;
        for (Core core : cpu.getCores()) {
            double coreUtilization = core.getUtilization( time );
            if (coreUtilization < utilization) {
                id = core.getId();
                utilization = coreUtilization;
                tiedSelection = core.tieSelected;
                tieSituation = false;
            } else if (coreUtilization == utilization) { // TODO per testare l'abbassamento di frequenze di PESOS dovrei commentare questa parte
                if (core.tieSelected < tiedSelection) {
                    id = core.getId();
                    utilization = coreUtilization;
                    tiedSelection = core.tieSelected;
                }
                tieSituation = true;
            }
        }
        
        if (tieSituation) {
            cpu.getCore( id ).tieSelected++;
        }
        
        return cpu.lastSelectedCore = id;
    }
    
    /**
     * Clones this model.
    */
    protected abstract CPUModel cloneModel();
    
    @Override
    protected CPUModel clone() {
        return cloneModel();
    }
    
    public static class MY_model extends CPUModel
    {
        private static final String REGRESSORS_TIME_CONSERVATIVE   = "regressors.txt";
        private static final String REGRESSORS_ENERGY_CONSERVATIVE = "regressors_normse.txt";
        
        
        /**
         * Creates MY PERSONAL model.
         * 
         * @param time_budget    time limit to complete a query (in us).
         * @param mode           PESOS modality (see {@linkplain CPUModel.Mode Mode}).
         * @param directory      directory used to load the files.
         * 
         * @throws IOException if a file doesn't exist or is malformed.
        */
        public MY_model( long time_budget, Mode mode, String directory )
        {
            this( new Time( time_budget, TimeUnit.MILLISECONDS ), mode, directory,
                  POSTINGS_PREDICTORS, EFFECTIVE_TIME_ENERGY,
                  (mode == Mode.ENERGY_CONSERVATIVE) ? REGRESSORS_ENERGY_CONSERVATIVE :
                                                       REGRESSORS_TIME_CONSERVATIVE );
        }
        
        /**
         * Creates MY PERSONAL model.
         * 
         * @param time_budget    time limit to complete a query (in us).
         * @param mode           PESOS modality (see {@linkplain CPUModel.Mode Mode}).
         * @param directory      directory used to load the files.
         * @param files          list of file used to load the model.
         * 
         * @throws IOException if a file doesn't exist or is malformed.
        */
        public MY_model( long time_budget, Mode mode, String directory, String... files ) {
            this( new Time( time_budget, TimeUnit.MILLISECONDS ), mode, directory, files );
        }
        
        public MY_model( Time time_budget, Mode mode, String directory, String... files ) {
            super( getType( mode ), directory, files );
            timeBudget = time_budget;
        }
        
        private static Type getType( Mode mode )
        {
            Type type = Type.MY_MODEL;
            type.setMode( mode );
            return type;
        }
        
        /*@Override
        public Long eval( Time now, QueryInfo... queries )
        {
            QueryInfo query = queries[0];
            Time currentDeadline = query.getArrivalTime().addTime( timeBudget );
            if (currentDeadline.compareTo( now ) <= 0) {
                // Time to complete the query is already over.
                return _device.getMaxFrequency();
            }
            
            // Get the frequency to resolve the query in the remaining time budget.
            int ppcRMSE = regressors.get( "class." + query.getTerms() + ".rmse" ).intValue();
            long pcost = query.getPostings() + ppcRMSE;
            Time budget = timeBudget.clone();
            budget.subTime( now.clone().subTime( query.getArrivalTime() ) );
            long target = getTargetFrequency( query.getTerms(), pcost, budget.getTimeMicros() );
            
            int maxCount = 1;
            for (Long frequency : _device.getFrequencies()) {
                if (frequency >= target) {
                    // Evaluate the residual completion time at the current frequency of the first query.
                    ppcRMSE = regressors.get( "class." + query.getTerms() + ".rmse" ).intValue();
                    pcost = query.getPostings() + ppcRMSE;
                    Time service = predictServiceTime( query.getTerms(), pcost, frequency );
                    Time startTime = query.getStartTime();
                    if (startTime.getTimeMicros() == 0) {
                        startTime.setTime( now );
                    }
                    service.subTime( now.clone().subTime( startTime ) );
                    Time endTime = now.clone().addTime( service );
                    
                    int count = 1;
                    for (int i = 1; i < queries.length; i++) {
                        QueryInfo q = queries[i];
                        ppcRMSE = regressors.get( "class." + q.getTerms() + ".rmse" ).intValue();
                        pcost   = q.getPostings() + ppcRMSE;
                        service = predictServiceTime( q.getTerms(), pcost, frequency );
                        Time timeBudget4q = (endTime.clone().subTime( q.getArrivalTime() )).addTime( service );
                        if (timeBudget4q.compareTo( timeBudget ) <= 0) {
                            count++;
                        } else {
                            break;
                        }
                        endTime.addTime( service );
                    }
                    
                    if (count > maxCount) {
                        maxCount = count;
                        target = frequency;
                    }
                    
                    if (count == queries.length) {
                        break;
                    }
                }
            }
            
            if (maxCount < queries.length) {
                // Can't meet the time budget for all the queries: return the maximum frequency.
                return _device.getMaxFrequency();
            }
            
            return target;
        }
        
        private Time predictServiceTime( int terms, long postings, long frequency )
        {
            String base  = frequency + "." + terms;
            double alpha = regressors.get( base + ".alpha" );
            double beta  = regressors.get( base + ".beta" );
            double rmse  = regressors.get( base + ".rmse" );
            long time = Utils.getTimeInMicroseconds( alpha * postings + beta + rmse, TimeUnit.MILLISECONDS );
            return new Time( time, TimeUnit.MICROSECONDS );
        }
        
        private long getTargetFrequency( int terms, long postings, double targetTime )
        {
            for (Long frequency : _device.getFrequencies()) {
                final String base = frequency + "." + terms;
                double alpha = regressors.get( base + ".alpha" );
                double beta  = regressors.get( base + ".beta" );
                double rmse  = regressors.get( base + ".rmse" );
                
                long extimatedTime = Utils.getTimeInMicroseconds( alpha * postings + beta + rmse, TimeUnit.MILLISECONDS );
                if (extimatedTime <= targetTime) {
                    return frequency;
                }
            }
            
            return _device.getMaxFrequency(); 
        }*/
        
        @Override
        public long selectCore( Time time, EnergyCPU cpu, QueryInfo q )
        {
            long id = -1;
            long minExecutionTime = Long.MAX_VALUE;
            long tiedSelection    = Long.MAX_VALUE;
            boolean tieSituation = false;
            for (Core core : cpu.getCores()) {
                long executionTime = ((MY_MODELcore) core).getQueryExecutionTime( time );
                if (executionTime < minExecutionTime) {
                    id = core.getId();
                    minExecutionTime = executionTime;
                    tiedSelection = core.tieSelected;
                    tieSituation = false;
                } else if (executionTime == minExecutionTime) {
                    if (core.tieSelected < tiedSelection) {
                        id = core.getId();
                        minExecutionTime = executionTime;
                        tiedSelection = core.tieSelected;
                    }
                    tieSituation = true;
                }
            }
            
            if (tieSituation) {
                cpu.getCore( id ).tieSelected++;
            }
            
            return cpu.lastSelectedCore = id;
        }
        
        @Override
        public Long eval( Time now, QueryInfo... queries )
        {
            QueryInfo query = queries[0];
            Time currentDeadline = query.getArrivalTime().addTime( timeBudget );
            if (currentDeadline.compareTo( now ) <= 0) {
                // Time to complete the query is already over.
                return _device.getMaxFrequency();
            }
            
            // Get the maximum time budget evaluated by the 2 algorithms.
            long LOAD_SENSITIVEbudget = evalLOAD_SENSITIVEtimeBudget( now, queries );
            long PESOSbudget = evalPESOStimeBudget( now, queries );
            final long targetTime = Math.max( LOAD_SENSITIVEbudget, PESOSbudget );
            
            int ppcRMSE = regressors.get( "class." + query.getTerms() + ".rmse" ).intValue();
            final long pcost = query.getPostings() + ppcRMSE;
            return identifyTargetFrequency( query.getTerms(), pcost, targetTime );
        }
        
        private long evalLOAD_SENSITIVEtimeBudget( Time now, QueryInfo... queries )
        {
            QueryInfo currentQuery = queries[0];
            QueryInfo last = queries[queries.length-1];
            Time deltaN = (last.getArrivalTime().addTime( timeBudget )).subTime( now );
            
            // Get the predicted time, at max speed, for the remaining queries.
            Time predictedTimeToCompute = new Time( 0, TimeUnit.MICROSECONDS );
            for (int i = 0; i < queries.length - 1; i++) {
                QueryInfo query = queries[i];
                int ppcRMSE = regressors.get( "class." + query.getTerms() + ".rmse" ).intValue();
                long pcost = query.getPostings() + ppcRMSE;
                long time = predictServiceTimeAtMaxFrequency( query.getTerms(), pcost );
                predictedTimeToCompute.addTime( new Time( time, TimeUnit.MICROSECONDS ) );
            }
            
            deltaN.subTime( predictedTimeToCompute );
            if (deltaN.getTimeMicros() == 0) {
                return 0;
            } else {
                // Get the slack time.
                int ppcRMSE = regressors.get( "class." + currentQuery.getTerms() + ".rmse" ).intValue();
                final long pcost = currentQuery.getPostings() + ppcRMSE;
                Time extimatedTime = new Time( predictServiceTimeAtMaxFrequency( currentQuery.getTerms(), pcost ) +
                                               deltaN.getTimeMicros() / queries.length,
                                               TimeUnit.MICROSECONDS );
                Time delta1 = (currentQuery.getArrivalTime().addTime( timeBudget )).subTime( now );
                Time targetTime = extimatedTime.min( delta1 );
                return targetTime.getTimeMicros();
            }
        }
        
        private long evalPESOStimeBudget( Time now, QueryInfo... queries )
        {
            QueryInfo currentQuery = queries[0];
            Time currentDeadline = currentQuery.getArrivalTime().addTime( timeBudget );
            long lateness = getLateness( now, queries );
            currentDeadline.subTime( lateness, TimeUnit.MICROSECONDS );
            if (currentDeadline.compareTo( now ) <= 0) {
                return 0;
            }
            
            int ppcRMSE = regressors.get( "class." + currentQuery.getTerms() + ".rmse" ).intValue();
            long pcost = currentQuery.getPostings() + ppcRMSE;
            long volume = pcost;
            double maxDensity = volume / currentDeadline.subTime( now ).getTimeMicros();
            
            for (int i = 1; i < queries.length; i++) {
                QueryInfo q = queries[i];
                
                ppcRMSE = regressors.get( "class." + q.getTerms() + ".rmse" ).intValue();
                volume += q.getPostings() + ppcRMSE;
                
                Time qArrivalTime = q.getArrivalTime();
                Time deadline = qArrivalTime.addTime( timeBudget.clone().subTime( lateness, TimeUnit.MICROSECONDS ) );
                if (deadline.compareTo( now ) <= 0) {
                    return 0;
                } else {
                    double density = volume / (deadline.subTime( now ).getTimeMicros());
                    if (density > maxDensity) {
                        maxDensity = density;
                    }
                }
            }
            
            return (long) (pcost/maxDensity);
        }
        
        private long getLateness( Time now, QueryInfo[] queries )
        {
            double lateness = 0;
            int cnt = 0;
            
            for (int i = 0; i < queries.length; i++) {
                QueryInfo q = queries[i];
                int ppcRMSE = regressors.get( "class." + q.getTerms() + ".rmse" ).intValue();
                long pcost = q.getPostings() + ppcRMSE;
                long predictedTimeMaxFreq = predictServiceTimeAtMaxFrequency( q.getTerms(), pcost );
                Time qArrivalTime = q.getArrivalTime();
                long budget = timeBudget.clone().subTime( now.clone().subTime( qArrivalTime ) ).getTimeMicros();
                
                if (predictedTimeMaxFreq > budget) {
                    lateness += predictedTimeMaxFreq - budget;
                } else {
                    cnt++;
                }
            }
            
            double result = lateness / cnt;
            return Utils.getTimeInMicroseconds( result, TimeUnit.MICROSECONDS );
        }
        
        public long predictServiceTimeAtMaxFrequency( int terms, long postings )
        {
            String base  = _device.getMaxFrequency() + "." + terms;
            double alpha = regressors.get( base + ".alpha" );
            double beta  = regressors.get( base + ".beta" );
            double rmse  = regressors.get( base + ".rmse" );
            return Utils.getTimeInMicroseconds( alpha * postings + beta + rmse, TimeUnit.MILLISECONDS );
        }
        
        private long identifyTargetFrequency( int terms, long postings, double targetTime )
        {
            for (Long frequency : _device.getFrequencies()) {
                final String base = frequency + "." + terms;
                double alpha = regressors.get( base + ".alpha" );
                double beta  = regressors.get( base + ".beta" );
                double rmse  = regressors.get( base + ".rmse" );
                
                long extimatedTime = Utils.getTimeInMicroseconds( alpha * postings + beta + rmse, TimeUnit.MILLISECONDS );
                if (extimatedTime <= targetTime) {
                    return frequency;
                }
            }
            
            return _device.getMaxFrequency(); 
        }
        
        public int getRMSE( int terms ) {
            return regressors.get( "class." + terms + ".rmse" ).intValue();
        }
        
        @Override
        public String getModelType( boolean delimeters )
        {
            if (delimeters) {
                return "MY_Model_" + getMode() + "_" + getTimeBudget().getTimeMillis() + "ms";
            } else {
                return "MY_Model (" + getMode() + ", t = " + getTimeBudget().getTimeMillis() + "ms)";
            }
        }
        
        @Override
        protected CPUModel cloneModel()
        {
            MY_model model = new MY_model( timeBudget.clone(), getMode(), "", _postings, _effective_time_energy, _regressors );
            try { model.loadModel(); }
            catch ( IOException e ) { e.printStackTrace(); }
            return model;
        }
        
        @Override
        public void close() {}
    }
    
    public static class LOAD_SENSITIVEmodel extends CPUModel
    {
        private static final String REGRESSORS_TIME_CONSERVATIVE   = "regressors.txt";
        private static final String REGRESSORS_ENERGY_CONSERVATIVE = "regressors_normse.txt";
        
        /**
         * Creates a new LOAD SENSITIVE model.
         * 
         * @param time_budget    time limit to complete a query (in ms).
         * @param mode           the model regressors type.
         * @param directory      directory used to load the files.
         * 
         * @throws IOException if a file doesn't exist or is malformed.
        */
        public LOAD_SENSITIVEmodel( long time_budget, Mode mode, String directory ) {
            this( new Time( time_budget, TimeUnit.MILLISECONDS ), mode, directory,
                            POSTINGS_PREDICTORS, EFFECTIVE_TIME_ENERGY,
                            (mode == Mode.ENERGY_CONSERVATIVE) ? REGRESSORS_ENERGY_CONSERVATIVE :
                                                                 REGRESSORS_TIME_CONSERVATIVE );
        }
        
        public LOAD_SENSITIVEmodel( long time_budget, Mode mode, String dir, String... files ){
            this( new Time( time_budget, TimeUnit.MILLISECONDS ), mode, dir, files );
        }
        
        public LOAD_SENSITIVEmodel( Time time_budget, Mode mode, String dir, String... files )
        {
            super( getType( mode ), dir, files );
            timeBudget = time_budget;
        }
        
        private static Type getType( Mode mode )
        {
            Type type = Type.LOAD_SENSITIVE;
            type.setMode( mode );
            return type;
        }
        
        /*@Override
        public long selectCore( Time time, EnergyCPU cpu, QueryInfo q )
        {
            // NOTE: This is a new core selection technique,
            //       based on the frequency evaluation.
            
            long id = -1;
            long minFrequency = Long.MAX_VALUE;
            long tiedSelection = Long.MAX_VALUE;
            boolean tieSituation = false;
            for (Core core : cpu.getCores()) {
                long frequency = core.getFrequency();
                core.addQuery( q, false );
                if (core.getFrequency() < minFrequency) {
                    id = core.getId();
                    minFrequency = core.getFrequency();
                    tiedSelection = core.tieSelected;
                    tieSituation = false;
                } else if (core.getFrequency() == minFrequency) {
                    if (core.tieSelected < tiedSelection) {
                        id = core.getId();
                        minFrequency = core.getFrequency();
                        tiedSelection = core.tieSelected;
                    }
                    tieSituation = true;
                }
                core.removeQuery( core.getQueue().size() - 1 );
                core.setFrequency( frequency );
            }
            
            if (tieSituation) {
                cpu.getCore( id ).tieSelected++;
            }
            
            return cpu.lastSelectedCore = id;
        }*/
        
        // TODO Miglior soluzione per LOAD SENSITIVE
        @Override
        public long selectCore( Time time, EnergyCPU cpu, QueryInfo q )
        {
            long id = -1;
            long minExecutionTime = Long.MAX_VALUE;
            long tiedSelection    = Long.MAX_VALUE;
            boolean tieSituation = false;
            for (Core core : cpu.getCores()) {
                long executionTime = ((LOAD_SENSITIVEcore) core).getQueryExecutionTime( time );
                if (executionTime < minExecutionTime) {
                    id = core.getId();
                    minExecutionTime = executionTime;
                    tiedSelection = core.tieSelected;
                    tieSituation = false;
                } else if (executionTime == minExecutionTime) {
                    if (core.tieSelected < tiedSelection) {
                        id = core.getId();
                        minExecutionTime = executionTime;
                        tiedSelection = core.tieSelected;
                    }
                    tieSituation = true;
                }
            }
            
            if (tieSituation) {
                cpu.getCore( id ).tieSelected++;
            }
            
            return cpu.lastSelectedCore = id;
        }
        
        @Override
        public Long eval( Time now, QueryInfo... queries )
        {
            QueryInfo currentQuery = queries[0];
            Time currentDeadline = currentQuery.getArrivalTime().addTime( timeBudget );
            if (currentDeadline.compareTo( now ) <= 0) {
                // Time to complete the query is already over.
                return _device.getMaxFrequency();
            }
            
            QueryInfo last = queries[queries.length-1];
            Time deltaN = (last.getArrivalTime().addTime( timeBudget )).subTime( now );
            
            // Get the predicted time, at max speed, for the remaining queries.
            Time predictedTimeToCompute = new Time( 0, TimeUnit.MICROSECONDS );
            for (int i = 0; i < queries.length - 1; i++) {
                QueryInfo query = queries[i];
                int ppcRMSE = regressors.get( "class." + query.getTerms() + ".rmse" ).intValue();
                long pcost = query.getPostings() + ppcRMSE;
                long time = predictServiceTimeAtMaxFrequency( query.getTerms(), pcost );
                predictedTimeToCompute.addTime( new Time( time, TimeUnit.MICROSECONDS ) );
            }
            
            //Time deltaNsigned = deltaN.subTime( predictedTimeToCompute );
            deltaN.subTime( predictedTimeToCompute );
            if (deltaN.getTimeMicros() == 0) {
                return _device.getMaxFrequency();
            } else {
                // Get the slack time.
                int ppcRMSE = regressors.get( "class." + currentQuery.getTerms() + ".rmse" ).intValue();
                final long pcost = currentQuery.getPostings() + ppcRMSE;
                Time extimatedTime = new Time( predictServiceTimeAtMaxFrequency( currentQuery.getTerms(), pcost ) +
                                               deltaN.getTimeMicros() / queries.length,
                                               TimeUnit.MICROSECONDS );
                Time delta1 = (currentQuery.getArrivalTime().addTime( timeBudget )).subTime( now );
                Time targetTime = extimatedTime.min( delta1 );
                return identifyTargetFrequency( currentQuery.getTerms(), pcost, targetTime.getTimeMicros() );
            }
        }
        
        public long predictServiceTimeAtMaxFrequency( int terms, long postings )
        {
            String base  = _device.getMaxFrequency() + "." + terms;
            double alpha = regressors.get( base + ".alpha" );
            double beta  = regressors.get( base + ".beta" );
            double rmse  = regressors.get( base + ".rmse" );
            return Utils.getTimeInMicroseconds( alpha * postings + beta + rmse, TimeUnit.MILLISECONDS );
        }
        
        private long identifyTargetFrequency( int terms, long postings, double targetTime )
        {
            for (Long frequency : _device.getFrequencies()) {
                final String base = frequency + "." + terms;
                double alpha = regressors.get( base + ".alpha" );
                double beta  = regressors.get( base + ".beta" );
                double rmse  = regressors.get( base + ".rmse" );
                
                long extimatedTime = Utils.getTimeInMicroseconds( alpha * postings + beta + rmse, TimeUnit.MILLISECONDS );
                if (extimatedTime <= targetTime) {
                    return frequency;
                }
            }
            
            return _device.getMaxFrequency(); 
        }
        
        public int getRMSE( int terms ) {
            return regressors.get( "class." + terms + ".rmse" ).intValue();
        }
        
        @Override
        public String getModelType( boolean delimeters )
        {
            if (delimeters) {
                return "LOAD_SENSITIVE_" + getMode() + "_" + getTimeBudget().getTimeMillis() + "ms";
            } else {
                return "LOAD_SENSITIVE (" + getMode() + ", t = " + getTimeBudget().getTimeMillis() + "ms)";
            }
        }

        @Override
        protected CPUModel cloneModel()
        {
            CPUModel model = new LOAD_SENSITIVEmodel( timeBudget, getMode(), "", _postings, _effective_time_energy, _regressors );
            try { model.loadModel(); }
            catch ( IOException e ) { e.printStackTrace(); }
            return model;
        }

        @Override
        public void close() {}
    }
    
    public static class PESOSmodel extends CPUModel
    {
        private static final String REGRESSORS_TIME_CONSERVATIVE   = "regressors.txt";
        private static final String REGRESSORS_ENERGY_CONSERVATIVE = "regressors_normse.txt";
        
        
        /**
         * Creates a new PESOS model.
         * 
         * @param time_budget    time limit to complete a query (in ms).
         * @param mode           PESOS modality (see {@linkplain CPUModel.Mode Mode}).
         * @param directory      directory used to load the files.
         * 
         * @throws IOException if a file doesn't exist or is malformed.
        */
        public PESOSmodel( long time_budget, Mode mode, String directory )
        {
            this( new Time( time_budget, TimeUnit.MILLISECONDS ), mode, directory,
                  POSTINGS_PREDICTORS, EFFECTIVE_TIME_ENERGY,
                  (mode == Mode.ENERGY_CONSERVATIVE) ? REGRESSORS_ENERGY_CONSERVATIVE : REGRESSORS_TIME_CONSERVATIVE );
        }
        
        /**
         * Creates a new PESOS model.
         * 
         * @param time_budget    time limit to complete a query (in ms).
         * @param mode           PESOS modality (see {@linkplain CPUModel.Mode Mode}).
         * @param directory      directory used to load the files.
         * @param files          list of file used to load the model.
         * 
         * @throws IOException if a file doesn't exist or is malformed.
        */
        public PESOSmodel( long time_budget, Mode mode, String directory, String... files ) {
            this( new Time( time_budget, TimeUnit.MILLISECONDS ), mode, directory, files );
        }
        
        public PESOSmodel( Time time_budget, Mode mode, String directory, String... files ) {
            super( getType( mode ), directory, files );
            timeBudget = time_budget;
        }
        
        private static Type getType( Mode mode )
        {
            Type type = Type.PESOS;
            type.setMode( mode );
            return type;
        }
        
        @Override
        public Long eval( Time now, QueryInfo... queries )
        {
            QueryInfo currentQuery = queries[0];
            Time currentDeadline = currentQuery.getArrivalTime().addTime( timeBudget );
            if (currentDeadline.compareTo( now ) <= 0) {
                // Time to complete the query is already over.
                return _device.getMaxFrequency();
            }
            
            long lateness = getLateness( now, queries );
            currentDeadline.subTime( lateness, TimeUnit.MICROSECONDS );
            if (currentDeadline.compareTo( now ) <= 0) {
                return _device.getMaxFrequency();
            }
            
            int ppcRMSE = regressors.get( "class." + currentQuery.getTerms() + ".rmse" ).intValue();
            long pcost = currentQuery.getPostings() + ppcRMSE;
            long volume = pcost;
            double maxDensity = volume / currentDeadline.subTime( now ).getTimeMicros();
            
            for (int i = 1; i < queries.length; i++) {
                QueryInfo q = queries[i];
                
                ppcRMSE = regressors.get( "class." + q.getTerms() + ".rmse" ).intValue();
                volume += q.getPostings() + ppcRMSE;
                
                Time qArrivalTime = q.getArrivalTime();
                Time deadline = qArrivalTime.addTime( timeBudget.clone().subTime( lateness, TimeUnit.MICROSECONDS ) );
                if (deadline.compareTo( now ) <= 0) {
                    return _device.getMaxFrequency();
                } else {
                    double density = volume / (deadline.subTime( now ).getTimeMicros());
                    if (density > maxDensity) {
                        maxDensity = density;
                    }
                }
            }
            
            double targetTime = pcost/maxDensity;
            return identifyTargetFrequency( currentQuery.getTerms(), pcost, targetTime );
        }
        
        private long getLateness( Time now, QueryInfo[] queries )
        {
            double lateness = 0;
            int cnt = 0;
            
            for (int i = 0; i < queries.length; i++) {
                QueryInfo q = queries[i];
                int ppcRMSE = regressors.get( "class." + q.getTerms() + ".rmse" ).intValue();
                long pcost = q.getPostings() + ppcRMSE;
                long predictedTimeMaxFreq = predictServiceTimeAtMaxFrequency( q.getTerms(), pcost );
                Time qArrivalTime = q.getArrivalTime();
                long budget = timeBudget.clone().subTime( now.clone().subTime( qArrivalTime ) ).getTimeMicros();
                
                if (predictedTimeMaxFreq > budget) {
                    lateness += predictedTimeMaxFreq - budget;
                } else {
                    cnt++;
                }
            }
            
            double result = lateness / cnt;
            return Utils.getTimeInMicroseconds( result, TimeUnit.MICROSECONDS );
        }
        
        public long predictServiceTimeAtMaxFrequency( int terms, long postings )
        {
            String base  = _device.getMaxFrequency() + "." + terms;
            double alpha = regressors.get( base + ".alpha" );
            double beta  = regressors.get( base + ".beta" );
            double rmse  = regressors.get( base + ".rmse" );
            return Utils.getTimeInMicroseconds( alpha * postings + beta + rmse, TimeUnit.MILLISECONDS );
        }
        
        private long identifyTargetFrequency( int terms, long postings, double targetTime )
        {
            for (Long frequency : _device.getFrequencies()) {
                final String base = frequency + "." + terms;
                double alpha = regressors.get( base + ".alpha" );
                double beta  = regressors.get( base + ".beta" );
                double rmse  = regressors.get( base + ".rmse" );
                
                long extimatedTime = Utils.getTimeInMicroseconds( alpha * postings + beta + rmse, TimeUnit.MILLISECONDS );
                if (extimatedTime <= targetTime) {
                    return frequency;
                }
            }
            
            return _device.getMaxFrequency(); 
        }
        
        public int getRMSE( int terms ) {
            return regressors.get( "class." + terms + ".rmse" ).intValue();
        }
        
        // TODO Per PESOS utilizzare questo: vince (di molto) nei 1000ms ma perde (di poco) nei 500ms.
        /*@Override
        public long selectCore( Time time, EnergyCPU cpu, QueryInfo q )
        {
            // NOTE: This is a new core selection technique,
            //       based on the frequency evaluation.
            
            long id = -1;
            long minFrequency = Long.MAX_VALUE;
            long tiedSelection = Long.MAX_VALUE;
            boolean tieSituation = false;
            for (Core core : cpu.getCores()) {
                long frequency = core.getFrequency();
                core.addQuery( q, false );
                if (core.getFrequency() < minFrequency) {
                    id = core.getId();
                    minFrequency = core.getFrequency();
                    tiedSelection = core.tieSelected;
                    tieSituation = false;
                } else if (core.getFrequency() == minFrequency) {
                    if (core.tieSelected < tiedSelection) {
                        id = core.getId();
                        minFrequency = core.getFrequency();
                        tiedSelection = core.tieSelected;
                    }
                    tieSituation = true;
                }
                core.removeQuery( time, core.getQueue().size() - 1, false );
                core.setFrequency( frequency );
            }
            
            if (tieSituation) {
                cpu.getCore( id ).tieSelected++;
            }
            
            return cpu.lastSelectedCore = id;
        }*/
        
        // Alternative technique selecting the core with the earliest completion time.
        /*@Override
        public long selectCore( Time time, EnergyCPU cpu, QueryInfo q )
        {
            long id = -1;
            long minExecutionTime = Long.MAX_VALUE;
            long tiedSelection = Long.MAX_VALUE;
            boolean tieSituation = false;
            for (Core core : cpu.getCores()) {
                long executionTime = ((PESOScore) core).getQueryExecutionTime( time );
                if (executionTime < minExecutionTime) {
                    id = core.getId();
                    minExecutionTime = executionTime;
                    tiedSelection = core.tieSelected;
                    tieSituation = false;
                } else if (executionTime == minExecutionTime) {
                    if (core.tieSelected < tiedSelection) {
                        id = core.getId();
                        minExecutionTime = executionTime;
                        tiedSelection = core.tieSelected;
                    }
                    tieSituation = true;
                }
            }
            
            if (tieSituation) {
                cpu.getCore( id ).tieSelected++;
            }
            
            return cpu.lastSelectedCore = id;
        }*/
        
        @Override
        public String getModelType( boolean delimeters )
        {
            if (delimeters) {
                return "PESOS_" + getMode() + "_" + getTimeBudget().getTimeMillis() + "ms";
            } else {
                return "PESOS (" + getMode() + ",t=" + getTimeBudget().getTimeMillis() + "ms)";
            }
        }

        @Override
        protected CPUModel cloneModel()
        {
            PESOSmodel model = new PESOSmodel( timeBudget.clone(), getMode(), "", _postings, _effective_time_energy, _regressors );
            try { model.loadModel(); }
            catch ( IOException e ) { e.printStackTrace(); }
            return model;
        }

        @Override
        public void close() {}
    }
    
    public static class PERFmodel extends CPUModel
    {
        /**
         * Creates a new PERF model.
         * 
         * @param directory    directory used to load the files.
         * 
         * @throws IOException if a file doesn't exist or is malformed.
        */
        public PERFmodel( String directory ) {
            super( Type.PERF, directory, POSTINGS_PREDICTORS, EFFECTIVE_TIME_ENERGY );
        }
        
        /**
         * Creates a new PERF model.
         * 
         * @param directory    directory used to load the files.
         * @param files        list of files used to load the model.
         * 
         * @throws IOException if a file doesn't exist or is malformed.
        */
        public PERFmodel( String directory, String... files ) {
            super( Type.PERF, directory, files );
        }
        
        @Override
        public Long eval( Time now, QueryInfo... queries ) {
            return _device.getMaxFrequency();
        }
        
        @Override
        public String getModelType( boolean delimeters ) {
            return type.toString();
        }
        
        @Override
        protected CPUModel cloneModel()
        {
            PERFmodel model = new PERFmodel( "", _postings, _effective_time_energy );
            try { model.loadModel(); }
            catch ( IOException e ) { e.printStackTrace(); }
            return model;
        }

        @Override
        public void close() {}
    }
    
    public static class CONSmodel extends CPUModel
    {
        private static final double TARGET = 0.70;
        private static final double UP_THRESHOLD = 0.80;
        private static final double DOWN_THRESHOLD = 0.20;
        public static final long PERIOD = 2000; // in ms.
        
        /**
         * Creates a new CONS model.
         * 
         * @param directory    directory used to load the files.
         * 
         * @throws IOException if a file doesn't exist or is malformed.
        */
        public CONSmodel( String directory ) {
            super( Type.CONS, directory, POSTINGS_PREDICTORS, EFFECTIVE_TIME_ENERGY );
        }
        
        /**
         * Creates a new CONS model.
         * 
         * @param directory    directory used to load the files.
         * @param files        list of files used to load the model.
         * 
         * @throws IOException if a file doesn't exist or is malformed.
        */
        public CONSmodel( String directory, String... files ) {
            super( Type.CONS, directory, files );
        }
        
        /**
         * Evaluate the input parameter to decide which is the "best" frequency
         * to complete the queued queries.
         * 
         * @param now       time of evaluation.
         * @param queries   list of parameters.
         * 
         * @return the "best" frequency, expressed in KHz.
        */
        @Override
        public Long eval( Time now, QueryInfo... queries )
        {
            EnergyCPU cpu = (EnergyCPU) _device;
            CONScore core = (CONScore) cpu.getCore( cpu.getCurrentCoreId() );
            return controlCPUfrequency( core );
        }
        
        private long controlCPUfrequency( CONScore core )
        {
            double utilization = getUtilization( core );
            if (utilization >= UP_THRESHOLD || utilization <= DOWN_THRESHOLD) {
                long targetFrequency = computeTargetFrequency( core );
                core.reset();
                return targetFrequency;
            }
            
            core.reset();
            return core.getFrequency();
        }
        
        private double getUtilization( CONScore core )
        {
            double utilization;
            double serviceRate = core.getServiceRate();
            double arrivalRate = core.getArrivalRate();
            
            if (serviceRate == 0) {
                if (arrivalRate == 0) {
                    utilization = 0.0;
                } else {
                    utilization = 1.0;
                }
            } else {
                utilization = arrivalRate / serviceRate; //ro=lamda/mu
            }
            
            return utilization;
        }
        
        private long getFrequencyGEQ( long targetFrequency )
        {
            for (Long frequency : _device.getFrequencies()) {
                if (frequency >= targetFrequency) {
                    return frequency;
                }
            }
            return _device.getMaxFrequency();
        }
        
        private long computeTargetFrequency( CONScore core )
        {
            double serviceRate       = core.getServiceRate();
            double targetServiceRate = core.getArrivalRate() / TARGET;
            
            if (serviceRate == 0.0) {
                if (targetServiceRate == 0.0) {
                    return _device.getMinFrequency();
                } else {
                    return _device.getMaxFrequency();
                }
            } else {
                return getFrequencyGEQ( (long) Math.ceil( core.getFrequency() * (targetServiceRate / serviceRate) ) );
            }
        }
        
        @Override
        public String getModelType( boolean delimeters ) {
            return type.toString();
        }
        
        @Override
        protected CPUModel cloneModel()
        {
            CONSmodel model = new CONSmodel( "", _postings, _effective_time_energy );
            try { model.loadModel(); }
            catch ( IOException e ) { e.printStackTrace(); }
            return model;
        }

        @Override
        public void close() {}
    }
    
    public static class PEGASUSmodel extends CPUModel
    {
        /**
         * Creates a new PEGASUS model.
         * 
         * @param time_budget    time limit to complete a query (in ms).
         * @param directory      directory used to load the files.
         * 
         * @throws IOException if a file doesn't exist or is malformed.
        */
        public PEGASUSmodel( long time_budget, String directory ) {
            this( new Time( time_budget, TimeUnit.MILLISECONDS ), directory, POSTINGS_PREDICTORS, EFFECTIVE_TIME_ENERGY );
        }
        
        public PEGASUSmodel( long time_budget, String dir, String... files ){
            this( new Time( time_budget, TimeUnit.MILLISECONDS ), dir, files );
        }
        
        public PEGASUSmodel( Time time_budget, String dir, String... files )
        {
            super( Type.PEGASUS, dir, files );
            timeBudget = time_budget;
        }
        
        @Override
        public String getModelType( boolean delimeters )
        {
            if (delimeters) {
                return "PEGASUS_" + getTimeBudget().getTimeMillis() + "ms";
            } else {
                return "PEGASUS (t = " + getTimeBudget().getTimeMillis() + "ms)";
            }
        }
        
        @Override
        protected CPUModel cloneModel()
        {
            CPUModel model = new PEGASUSmodel( timeBudget, "", _postings, _effective_time_energy );
            try { model.loadModel(); }
            catch ( IOException e ) { e.printStackTrace(); }
            return model;
        }
        
        @Override
        public Long eval( Time now, QueryInfo... params ) {
            return _device.getFrequency();
        }
        
        @Override
        public void close() {}
    }

    public static class QueryInfo
    {
        private long _id;
        private long coreId;
        
        private boolean isAvailable = false;
        
        private int _terms;
        private int _postings;
        
        private Event event;
        private Time arrivalTime = new Time( 0, TimeUnit.MILLISECONDS );
        
        private Time startTime = new Time( 0, TimeUnit.MILLISECONDS );
        private Time currentTime;
        private Time endTime;
        private long _frequency;
        private double previousEnergy, lastEnergy;
        private double energyConsumption;
        
        private Map<Long,Pair<Time,Double>> timeAndEnergyPerFrequency;
        
        
        public QueryInfo( long id ) {
            _id = id;
        }
        
        public QueryInfo( long id, int terms, int postings )
        {
            _id = id;
            _terms = terms;
            _postings = postings;
            timeAndEnergyPerFrequency = new HashMap<>();
        }
        
        public long getId() {
            return _id;
        }
        
        public int getTerms() {
            return _terms;
        }
        
        public int getPostings() {
            return _postings;
        }
        
        public long getFrequency() {
            return _frequency;
        }
        
        public void setFrequency( long frequency ) {
            _frequency = frequency;
        }
        
        public void setEvent( Event event ) {
            this.event = event;
        }
        
        public Event getEvent() {
            return event;
        }
        
        public void setArrivalTime( Time t ) {
            arrivalTime.setTime( t );
        }
        
        public Time getArrivalTime() {
            return arrivalTime.clone();
        }
        
        public void setCoreId( long coreId ) {
            this.coreId = coreId;
        }
        
        public long getCoreId() {
            return coreId;
        }
        
        public Time getStartTime() {
            return startTime.clone();
        }
        
        public Time getEndTime() {
            return endTime.clone();
        }
        
        public boolean isComplete( Time time ) {
            return endTime.compareTo( time ) <= 0;
        }
        
        public double getElapsedEnergy() {
            return previousEnergy;
        }
        
        public double getLastEnergy() {
            return lastEnergy;
        }
        
        public void setTimeToComplete( Time startTime, Time endTime )
        {
            this.startTime   = startTime;
            this.currentTime = startTime;
            this.endTime     = endTime;
        }
        
        public void setEnergyConsumption( double energy ) {
            previousEnergy = lastEnergy = energy;
            energyConsumption = energy;
        }
        
        public void updateTimeEnergy( Time time, long newFrequency, double energy )
        {
            /*if (time.clone().subTime( currentTime ).getTimeMicros() == 0) {
                return;
            }*/
            
            //System.out.println( "\nSTO AGGIORNANDO: " + time );
            //System.out.println( "FROM: " + currentTime + ", OLD_TO: " + endTime + ", ENERGY: " + lastEnergy );
            //System.out.println( "TOTAL NEW TIME: " + getTime( newFrequency ) );
            double timeFrequency    = getTime( _frequency ).getTimeMicros();
            double completionTime   = endTime.getTimeMicros() - currentTime.getTimeMicros();
            double oldCompletedTime = timeFrequency - completionTime;
            double completedTime    = (time.getTimeMicros() - currentTime.getTimeMicros()) + oldCompletedTime;
            
            double percentageCompleted = completedTime / timeFrequency;
            
            long newCompletionTime = getTime( newFrequency ).getTimeMicros();
            long newQueryDuration  = newCompletionTime - (long) (newCompletionTime * percentageCompleted);
            //System.out.println( "NEW QUERY DURATION: " + newCompletionTime + ", REAL: " + newQueryDuration );
            Time newEndTime        = time.clone().addTime( newQueryDuration, TimeUnit.MICROSECONDS );
            
            double timeElapsed   = time.getTimeMicros() - currentTime.getTimeMicros();
            double elapsedEnergy = (lastEnergy / newCompletionTime) * timeElapsed;
            double energyUnitNew = energy / newCompletionTime;
            double newEnergy     = energyUnitNew * newQueryDuration;
            //System.out.println( "PERCENTAGE: " + percentageCompleted + ", ENERGY_ELAPSED: " + elapsedEnergy );
            //System.out.println( "NEW_ENERGY: " + energy + ", NEW_ENERGY_REAL: " + newEnergy );
            
            //EnergyCPU.writeResult( _frequency, elapsedEnergy );
            
            //System.out.println( "ARRIVAL: " + arrivalTime + ", OLD TIME: " + endTime + ", NEW: " + newEndTime );
            
            //if (time.getTimeMicros() == 19231494457L)
            //   System.out.println( "CURRENT: " + time + ", OLD_END_TIME: " + endTime + ", NEW_END_TIME: " + newEndTime );
            
            energyConsumption += elapsedEnergy + newEnergy - lastEnergy;
            previousEnergy = elapsedEnergy;
            lastEnergy     = newEnergy;
            currentTime    = time;
            endTime        = newEndTime;
            
            //System.out.println( "CURRENT: " + time + ", NEW_END_TIME: " + endTime );
            //System.out.println( "TOTAL_ENERGY: " + energyConsumption );
            
            setFrequency( newFrequency );
        }
        
        public double getEnergyConsumption() {
            return energyConsumption;
        }
        
        public double getCompletionTime() {
            return endTime.clone().subTime( startTime ).getTimeMicros();
        }
    
        public void setTimeAndEnergy( long frequency,
                                      long time,
                                      double energy ) {
            setTimeAndEnergy( frequency, new Time( time, TimeUnit.MICROSECONDS ), energy );
        }
        
        public void setTimeAndEnergy( long frequency,
                                      Time time,
                                      double energy )
        {
            timeAndEnergyPerFrequency.put( frequency, new Pair<>( time, energy ) );
            isAvailable = true;
        }
        
        public double getEnergy( long frequency ) {
            return timeAndEnergyPerFrequency.get( frequency ).getSecond();
        }
        
        public Time getTime( long frequency ) {
            return timeAndEnergyPerFrequency.get( frequency ).getFirst();
        }
        
        public boolean isAvailable() {
            return isAvailable;
        }
        
        @Override
        public QueryInfo clone()
        {
            QueryInfo query = new QueryInfo( _id, _terms, _postings );
            if (arrivalTime != null) query.arrivalTime = arrivalTime.clone();
            query.timeAndEnergyPerFrequency = timeAndEnergyPerFrequency;
            query.isAvailable = isAvailable;
            return query;
        }
        
        @Override
        public String toString() {
            return "{ID: " + getId() + ", Arrival: " + arrivalTime +
                   ", Start: " + startTime + ", End: " + endTime + "}";
        }
    }
}
