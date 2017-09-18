/**
 * @author Stefano Ceccotti
*/

package simulator.test.energy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import simulator.core.Model;
import simulator.events.Event;
import simulator.test.energy.CPUEnergyModel.QueryInfo;
import simulator.test.energy.EnergyCPU.Core;
import simulator.utils.Pair;
import simulator.utils.Time;
import simulator.utils.Utils;

public abstract class CPUEnergyModel extends Model<Long,QueryInfo> implements Cloneable
{
    private static final String SEPARATOR = "=";
    
    private static final String DIR = "Models/PESOS/MaxScore/";
    private static final String POSTINGS_PREDICTORS            = DIR + "predictions.txt";
    private static final String REGRESSORS_TIME_CONSERVATIVE   = DIR + "regressors.txt";
    private static final String REGRESSORS_ENERGY_CONSERVATIVE = DIR + "regressors_normse.txt";
    private static final String EFFECTIVE_TIME_ENERGY          = DIR + "time_energy.txt";
    
    protected String _postings;
    protected String _regressors;
    protected String _effective_time_energy;
    
    // Time limit to complete a query.
    protected Time timeBudget;
    
    // List of query infos.
    private Map<Long,QueryInfo> queries;
    
    // Map used to store the predictors for the evaluation of the "best" frequency.
    protected Map<String,Double> regressors;
    
    protected List<Long> _frequencies;
    
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
        CONS( "CONS CONSERVATIVE" );
        
        private Mode mode;
        private String name;
        
        private Type( final String name ) {
            this.name = name;
        }
        
        public void setMode( final Mode mode ) {
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
     * PESOS modality:
     * <p><lu>
     * <li>PESOS_TIME_CONSERVATIVE:   consumes more energy, reducing the tail latency.
     * <li>PESOS_ENERGY_CONSERVATIVE: consumes less energy at a higher tail latency.
     * </lu>
    */
    public enum Mode
    {
        PESOS_TIME_CONSERVATIVE( "TC" ),
        PESOS_ENERGY_CONSERVATIVE( "EC" );
        
        private final String name;
        private Mode( final String name ) {
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
     * @param type           type of model (see {@linkplain CPUEnergyModel.Type Type}).
     * @param frequencies    file containing a list of available frequencies.
     * 
     * @throws IOException if the file of frequencies doesn't exits or is malformed.
    */
    public CPUEnergyModel( final Type type, final String frequencies, final String... files ) throws IOException {
        this( type, readFrequencies( frequencies ) );
    }
    
    public CPUEnergyModel( final Type type, final List<Long> frequencies, final String... files )
    {
        this.type = type;
        // Order the frequencies from higher to lower.
        Collections.sort( _frequencies = frequencies, Collections.reverseOrder() );
        
        if (files.length == 0) {
            _postings = POSTINGS_PREDICTORS;
            _effective_time_energy = EFFECTIVE_TIME_ENERGY;
            if (type == Type.PESOS) {
                if (type.getMode() == Mode.PESOS_TIME_CONSERVATIVE) {
                    _regressors = REGRESSORS_TIME_CONSERVATIVE;
                } else {
                    _regressors = REGRESSORS_ENERGY_CONSERVATIVE;
                }
            }
        } else {
            _postings = DIR + files[0];
            _effective_time_energy = DIR + files[1];
            if (files.length == 3) {
                _regressors = DIR + files[2];
            }
        }
    }
    
    /**
     * Reads the list of available frequencies for this device.
     * 
     * @param frequencies_file    file where the frequencies are taken
     * 
     * @return the list of available frequencies
    */
    private static List<Long> readFrequencies( final String frequencies_file ) throws IOException
    {
        final Pattern p = Pattern.compile( "\\w+" );
        
        BufferedReader frequencyReader = new BufferedReader( new FileReader( frequencies_file ) );
        List<Long> frequencies = new ArrayList<>();
        
        String frequencyLine;
        while ((frequencyLine = frequencyReader.readLine()) != null) {
            Matcher m = p.matcher( frequencyLine );
            while (m.find()) {
                frequencies.add( Long.parseLong( m.group() ) );
            }
        }
        
        frequencyReader.close();
        
        return frequencies;
    }
    
    @Override
    public void loadModel() throws IOException
    {
        if (type == Type.PESOS) {
            loadRegressors();
        }
        loadPostingsPredictors();
        loadEffectiveTimeEnergy();
    }
    
    public String getModelType( final boolean delimeters )
    {
        if (type == Type.PESOS) {
            if (delimeters) return "PESOS_" + getMode() + "_" + getTimeBudget().getTimeMicroseconds()/1000L + "ms";
            else return "PESOS (" + getMode() + ", t = " + getTimeBudget().getTimeMicroseconds()/1000L + "ms)";
        } else {
            if (type == Type.CONS) {
                return "CONS_" + getMode();
            } else {
                return type.toString();
            }
        }
    }
    
    public Type getType() {
        return type;
    }
    
    public Mode getMode() {
        return type.getMode();
    }

    public Time getTimeBudget() {
        return timeBudget;
    }

    private void loadPostingsPredictors() throws IOException
    {
        queries = new HashMap<>( 1 << 14 );
        
        FileReader fReader = new FileReader( _postings );
        BufferedReader predictorReader = new BufferedReader( fReader );
        
        String line = null;
        while ((line = predictorReader.readLine()) != null) {
            String[] values = line.split( "\\t+" );
            long queryID = Long.parseLong( values[0] );
            int terms    = Integer.parseInt( values[1] );
            int postings = Integer.parseInt( values[2] );
            QueryInfo query = new QueryInfo( queryID, terms, postings );
            queries.put( queryID, query );
        }
        
        predictorReader.close();
        fReader.close();
    }
    
    private void loadRegressors() throws IOException
    {
        FileReader fReader = new FileReader( _regressors );
        BufferedReader regressorReader = new BufferedReader( fReader );
        
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
        FileReader fReader = new FileReader( _effective_time_energy );
        BufferedReader regressorReader = new BufferedReader( fReader );
        
        String line;
        while ((line = regressorReader.readLine()) != null) {
            String[] values = line.split( "\\s+" );
            
            long queryID = Long.parseLong( values[0] );
            QueryInfo query = queries.get( queryID );
            int index = 0;
            for (int i = 1; i < values.length; i+=2) {
                double qTime  = Double.parseDouble( values[i] );
                double energy = Double.parseDouble( values[i+1] );
                long time = Utils.getTimeInMicroseconds( qTime, TimeUnit.MILLISECONDS );
                query.setTimeAndEnergy( _frequencies.get( index++ ), time, energy );
            }
        }
        
        regressorReader.close();
        fReader.close();
    }

    public QueryInfo getQuery( final long queryID ) {
        return queries.get( queryID ).clone();
    }
    
    /**
     * Clones this model.
    */
    protected abstract CPUEnergyModel cloneModel();
    
    @Override
    protected CPUEnergyModel clone() {
        return cloneModel();
    }
    
    public static class QueryInfo
    {
        private long _id;
        private long coreId;
        
        private boolean isAvailable = false;
        
        private int _terms;
        private int _postings;
        
        private Event event;
        private Time arrivalTime;
        
        private Time startTime = Time.ZERO;
        private Time currentTime;
        private Time endTime;
        private long _frequency;
        private double previousEnergy, lastEnergy;
        private double energyConsumption;
        
        private Map<Long,Pair<Time,Double>> timeAndEnergyPerFrequency;
        
        
        public QueryInfo( final long id ) {
            _id = id;
        }
        
        public QueryInfo( final long id, final int terms, final int postings )
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
        
        public void setFrequency( final long frequency ) {
            _frequency = frequency;
        }
        
        public void setEvent( final Event event ) {
            this.event = event;
        }
        
        public Event getEvent() {
            return event;
        }
        
        public void setArrivalTime( final Time t ) {
            arrivalTime = t;
        }
        
        public Time getArrivalTime() {
            return arrivalTime.clone();
        }
        
        public void setCoreId( final long coreId ) {
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
        
        public double getElapsedEnergy() {
            return previousEnergy;
        }
        
        public double getLastEnergy() {
            return lastEnergy;
        }
        
        public void setTimeToComplete( final Time startTime, final Time endTime )
        {
            this.startTime    = startTime;
            this.currentTime  = startTime;
            this.endTime      = endTime;
        }
        
        public void setEnergyConsumption( final double energy ) {
            previousEnergy = lastEnergy = energy;
            energyConsumption = energy;
        }
        
        public void updateTimeEnergy( final Time time, final long newFrequency, final double energy )
        {
            //System.out.println( "\nSTO AGGIORNANDO: " + time );
            //System.out.println( "FROM: " + currentTime + ", OLD_TO: " + endTime + ", ENERGY: " + lastEnergy );
            //System.out.println( "TOTAL NEW TIME: " + getTime( newFrequency ) );
            double timeFrequency    = getTime( _frequency ).getTimeMicroseconds();
            double completionTime   = endTime.getTimeMicroseconds() - currentTime.getTimeMicroseconds();
            double oldCompletedTime = timeFrequency - completionTime;
            double completedTime    = (time.getTimeMicroseconds() - currentTime.getTimeMicroseconds()) + oldCompletedTime;
            
            double percentageCompleted = completedTime / timeFrequency;
            
            long newCompletionTime = getTime( newFrequency ).getTimeMicroseconds();
            long newQueryDuration  = newCompletionTime - (long) (newCompletionTime * percentageCompleted);
            //System.out.println( "NEW QUERY DURATION: " + newCompletionTime + ", REAL: " + newQueryDuration );
            Time newEndTime        = time.clone().addTime( newQueryDuration, TimeUnit.MICROSECONDS );
            
            double timeElapsed   = time.getTimeMicroseconds() - currentTime.getTimeMicroseconds();
            double elapsedEnergy = (lastEnergy / newCompletionTime) * timeElapsed;
            double energyUnitNew = energy / newCompletionTime;
            double newEnergy     = energyUnitNew * newQueryDuration;
            //System.out.println( "PERCENTAGE: " + percentageCompleted + ", ENERGY_ELAPSED: " + elapsedEnergy );
            //System.out.println( "NEW_ENERGY: " + energy + ", NEW_ENERGY_REAL: " + newEnergy );
            //EnergyCPU.writeResult( _frequency, elapsedEnergy, false );
            
            //System.out.println( "ARRIVAL: " + arrivalTime + ", OLD TIME: " + endTime + ", NEW: " + newEndTime );
            
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
            return endTime.clone().subTime( startTime ).getTimeMicroseconds();
        }

        public void setTimeAndEnergy( final long frequency,
                                      final long time,
                                      final double energy ) {
            setTimeAndEnergy( frequency, new Time( time, TimeUnit.MICROSECONDS ), energy );
        }
        
        public void setTimeAndEnergy( final long frequency,
                                      final Time time,
                                      final double energy )
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
    
    public static class PESOSmodel extends CPUEnergyModel
    {
        /**
         * Creates a new PESOS model.
         * 
         * @param time_budget    time limit to complete a query (in ms).
         * @param mode           PESOS modality (see {@linkplain CPUEnergyModel.Mode Mode}).
         * @param frequencies    file containing a list of available frequencies.
         * 
         * @throws IOException if the file of frequencies doesn't exists or is malformed.
        */
        public PESOSmodel( final long time_budget, final Mode mode, final String frequencies, final String... files ) throws IOException {
            this( time_budget, mode, readFrequencies( frequencies ), files );
        }
        
        public PESOSmodel( final long time_budget, final Mode mode, final List<Long> frequencies, final String... files ) {
            this( new Time( time_budget, TimeUnit.MILLISECONDS ), mode, frequencies, files );
        }
        
        public PESOSmodel( final Time time_budget, final Mode mode, final List<Long> frequencies, final String... files ) {
            super( getType( mode ), frequencies, files );
            timeBudget = time_budget;
        }
        
        private static Type getType( final Mode mode )
        {
            Type type = Type.PESOS;
            type.setMode( mode );
            return type;
        }
        
        /**
         * Evaluates the input parameters to decide which is the "best" frequency
         * to complete the current queue of queries.
         * 
         * @param now       time of evaluation.
         * @param queries   list of parameters.
         * 
         * @return the "best" frequency, expressed in KHz.
        */
        @Override
        public Long eval( final Time now, final QueryInfo... queries )
        {
            QueryInfo currentQuery = queries[0];
            
            if (now.getTimeMicroseconds() == 44709952703L || now.getTimeMicroseconds() == 44709955000L) {
                System.out.println( "QUERY: " + currentQuery.getId() );
                System.out.println( "NOW: " + now + ", ARRIVAL: " + currentQuery.getArrivalTime() );
            }
            
            Time currentDeadline = currentQuery.getArrivalTime().addTime( timeBudget );
            if (now.getTimeMicroseconds() == 44709952703L || now.getTimeMicroseconds() == 44709955000L)
                System.out.println( "CURRENT_DEADLINE: " + currentDeadline + ", NOW: " + now );
            if (currentDeadline.compareTo( now ) <= 0) {
                // Time to complete the query is already over.
                // We can only set the maximum frequency.
                return _frequencies.get( 0 );
            }
            
            int ppcRMSE = regressors.get( "class." + currentQuery.getTerms() + ".rmse" ).intValue();
            long pcost = currentQuery.getPostings() + ppcRMSE;
            long volume = pcost;
            double maxDensity = Double.MIN_VALUE;
            long lateness = getLateness( now, queries );
            currentDeadline.subTime( lateness, TimeUnit.MICROSECONDS );
            if (now.getTimeMicroseconds() == 44709952703L || now.getTimeMicroseconds() == 44709955000L) {
                System.out.println( "LATENESS: " + lateness );
                System.out.println( "CURRENT_DEADLINE: " + currentDeadline );
            }
            if (currentDeadline.compareTo( now ) <= 0) {
                return _frequencies.get( 0 );
            } else {
                maxDensity = volume / (currentDeadline.subTime( now )).getTimeMicroseconds();
            }
            
            for (int i = 1; i < queries.length; i++) {
                QueryInfo q = queries[i];
                
                ppcRMSE = regressors.get( "class." + q.getTerms() + ".rmse" ).intValue();
                volume += q.getPostings() + ppcRMSE;
                
                Time qArrivalTime = q.getArrivalTime();
                Time deadline = qArrivalTime.addTime( timeBudget.clone().subTime( lateness, TimeUnit.MICROSECONDS ) );
                if (deadline.compareTo( now ) <= 0) {
                    return _frequencies.get( 0 );
                } else {
                    double density = volume / (deadline.subTime( now ).getTimeMicroseconds());
                    if (density > maxDensity) {
                        maxDensity = density;
                    }
                }
            }
            
            if (now.getTimeMicroseconds() == 44709952703L || now.getTimeMicroseconds() == 44709955000L) {
                System.out.println( "P_COST: " + pcost + ", MAX_DENSITY: " + maxDensity );
                System.out.println( "TARGET TIME: " + pcost/maxDensity );
            }
            
            double targetTime = pcost/maxDensity;
            return identifyTargetFrequency( currentQuery.getTerms(), pcost, targetTime );
        }
        
        private long getLateness( final Time now, final QueryInfo[] queries )
        {
            double lateness = 0;
            int cnt = 0;
            
            for (int i = 0; i < queries.length; i++) {
                QueryInfo q = queries[i];
                int ppcRMSE  = regressors.get( "class." + q.getTerms() + ".rmse" ).intValue();
                long pcost4q = q.getPostings() + ppcRMSE;
                long predictedRemainingTime4q = predictServiceTimeAtMaxFrequency( q.getTerms(), pcost4q );
                Time qArrivalTime = q.getArrivalTime();
                long budget4q = Math.max( 0, timeBudget.clone().subTime( now.clone().subTime( qArrivalTime ) ).getTimeMicroseconds() );
                
                if (now.getTimeMicroseconds() == 44709952703L || now.getTimeMicroseconds() == 44709955000L)
                    System.out.println( "PREDICTED: " + predictedRemainingTime4q + ", BUDGET: " + budget4q );
                
                if (predictedRemainingTime4q > budget4q) {
                    lateness += predictedRemainingTime4q - budget4q;
                } else {
                    cnt++;
                }
            }
            
            double result = lateness / cnt;
            return Utils.getTimeInMicroseconds( result, TimeUnit.MICROSECONDS );
        }
        
        private long predictServiceTimeAtMaxFrequency( final int terms, final long postings )
        {
            String base  = _frequencies.get( 0 ) + "." + terms;
            double alpha = regressors.get( base + ".alpha" );
            double beta  = regressors.get( base + ".beta" );
            double rmse  = regressors.get( base + ".rmse" );
            return Utils.getTimeInMicroseconds( alpha * postings + beta + rmse, TimeUnit.MILLISECONDS );
        }
        
        private long identifyTargetFrequency( final int terms, final long postings, final double targetTime )
        {
            for (int i = _frequencies.size() - 1; i > 0; i--) {
                final String base  = _frequencies.get( i ) + "." + terms;
                double alpha = regressors.get( base + ".alpha" );
                double beta  = regressors.get( base + ".beta" );
                double rmse  = regressors.get( base + ".rmse" );
                //System.out.println( "ALPHA: " + alpha + ", BETA: " + beta + ", RMSE: " + rmse );
                
                long extimatedTime = Utils.getTimeInMicroseconds( alpha * postings + beta + rmse, TimeUnit.MILLISECONDS );
                //System.out.println( "EXTIMATED TIME: " + extimatedTime + "ns @ " + _frequencies.get( i ) );
                if (extimatedTime <= targetTime) {
                    return _frequencies.get( i );
                }
            }
            
            return _frequencies.get( 0 ); 
        }

        @Override
        public CPUEnergyModel cloneModel()
        {
            Collections.sort( _frequencies, Collections.reverseOrder() );
            List<Long> freqs = new ArrayList<>( _frequencies );
            PESOSmodel model = new PESOSmodel( timeBudget.clone(), getMode(), freqs );
            try { model.loadModel(); }
            catch ( IOException e ) { e.printStackTrace(); }
            Collections.sort( _frequencies );
            return model;
        }
    }
    
    public static class PERFmodel extends CPUEnergyModel
    {
        /**
         * Creates a new PERF model.
         * 
         * @param frequencies    file containing a list of available frequencies.
         * 
         * @throws IOException if the file of frequencies doesn't exists or is malformed.
        */
        public PERFmodel( final String frequencies ) throws IOException {
            this( readFrequencies( frequencies ) );
        }
        
        public PERFmodel( final List<Long> frequencies ) {
            super( Type.PERF, frequencies );
        }
        
        /**
         * Evaluates the input parameters to decide which is the "best" frequency
         * to complete the current queue of queries.
         * 
         * @param now       time of evaluation.
         * @param queries   list of parameters.
         * 
         * @return the "best" frequency, expressed in KHz.
        */
        @Override
        public Long eval( final Time now, final QueryInfo... queries ) {
            return _device.getMaxFrequency();
        }
        
        @Override
        public CPUEnergyModel cloneModel()
        {
            Collections.sort( _frequencies, Collections.reverseOrder() );
            List<Long> freqs = new ArrayList<>( _frequencies );
            PERFmodel model = new PERFmodel( freqs );
            try { model.loadModel(); }
            catch ( IOException e ) { e.printStackTrace(); }
            Collections.sort( _frequencies );
            return model;
        }
    }
    
    public static class CONSmodel extends CPUEnergyModel
    {
        private static final double TARGET = 0.70;
        private static final double UP_THRESHOLD = 0.80;
        private static final double DOWN_THRESHOLD = 0.20;
        public static final long PERIOD = 2000;
        
        /**
         * Creates a new CONS model.
         * 
         * @param frequencies    file containing a list of available frequencies.
         * 
         * @throws IOException if the file of frequencies doesn't exists or is malformed.
        */
        public CONSmodel( final Mode mode, final String frequencies ) throws IOException {
            this( mode, readFrequencies( frequencies ) );
        }
        
        public CONSmodel( final Mode mode, final List<Long> frequencies ) {
            super( getType( mode ), frequencies );
        }
        
        private static Type getType( final Mode mode )
        {
            Type type = Type.CONS;
            type.setMode( mode );
            return type;
        }
        
        /**
         * Evaluate the input parameter to decide which is the "best" frequency
         * to complete the current queue of queries.
         * 
         * @param now       time of evaluation.
         * @param queries   list of parameters.
         * 
         * @return the "best" frequency, expressed in KHz.
        */
        @Override
        public Long eval( final Time now, final QueryInfo... queries )
        {
            EnergyCPU cpu = (EnergyCPU) _device;
            Core core = cpu.getCore( cpu.getCurrentCoreId() );
            /*double utilization = 0;
            double lambda = cpu.getFrequencyArrivals();
            double mu     = cpu.getFrequencyDepartures();
            utilization   = lambda/mu;
            
            if (utilization >= UP_THRESHOLD) {
                return _device.getMaxFrequency();
            }
            
            int freqIdx = getFrequencyIndex( core.getFrequency() );
            if (utilization <= DOWN_THRESHOLD) { // Step down the frequency.
                return _frequencies.get( Math.min( _frequencies.size() - 1, freqIdx + 1 ) );
            } else {
                return _frequencies.get( freqIdx );
            }*/
            
            return controlCPUFrequency( core );
        }
        
        /*private int getFrequencyIndex( final long frequency )
        {
            for (int i = 0; i < _frequencies.size(); i++) {
                if (_frequencies.get( i ) == frequency) {
                    return i;
                }
            }
            return -1;
        }*/
        
        private long controlCPUFrequency( final Core core )
        {
            double utilization = getUtilization( core );
            if (utilization >= UP_THRESHOLD || utilization <= DOWN_THRESHOLD) {
                long targetFrequency = computeTargetFrequency( core );
                System.out.println( "#DVFS-CONSERVATIVE: Target frequency is " + targetFrequency );
                core.reset();
                return targetFrequency;
            }
            
            //System.err.println( "#DVFS-CONSERVATIVE: Utilization is " + utilization );
            core.reset();
            return core.getFrequency();
        }
        
        private double getUtilization( final Core core )
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
        
        private long computeTargetFrequency( final Core core )
        {
            double serviceRate       = core.getServiceRate();
            double targetServiceRate = core.getArrivalRate() / TARGET;
                    
            if (serviceRate == 0.0) {
                if (targetServiceRate == 0.0) {
                    return _device.getMinFrequency();
                } else {
                    return _device.getMaxFrequency();
                }
            } else { // FIXME quanto vale dvfs.getFrequency(0) ??? ritorna un indice??
                //return (long) Math.ceil( dvfs.getFrequency(0) * (targetServiceRate / serviceRate) );
                return _device.getMinFrequency();
            }
        }
        
        @Override
        public CPUEnergyModel cloneModel()
        {
            Collections.sort( _frequencies, Collections.reverseOrder() );
            List<Long> freqs = new ArrayList<>( _frequencies );
            CONSmodel model = new CONSmodel( getMode(), freqs );
            try { model.loadModel(); }
            catch ( IOException e ) { e.printStackTrace(); }
            Collections.sort( _frequencies );
            return model;
        }
    }
}