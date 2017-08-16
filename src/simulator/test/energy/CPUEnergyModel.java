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
import simulator.utils.Pair;
import simulator.utils.Time;
import simulator.utils.Utils;

public class CPUEnergyModel implements Model<Long,QueryInfo>
{
    private static final String SEPARATOR = "=";
    
    private static final String DIR = "Models/PESOS/MaxScore/";
    private static final String POSTINGS_PREDICTORS            = DIR + "MaxScore.predictions.txt";
    private static final String REGRESSORS_TIME_CONSERVATIVE   = DIR + "regressors_MaxScore.txt";
    private static final String REGRESSORS_ENERGY_CONSERVATIVE = DIR + "regressors_normse_MaxScore.txt";
    private static final String EFFECTIVE_TIME_ENERGY          = DIR + "MaxScore_time_energy.txt";
    
    // Time limit to complete a query.
    private Time timeBudget;
    
    // List of query infos.
    private Map<Long,QueryInfo> queries;
    
    // Map used to store the predictors for the evaluation of the "best" frequency.
    private Map<String,Double> regressors;
    
    private List<Long> _frequencies;
    
    // PESOS evaluation mode.
    private Mode mode;
    
    /**
     * Types of PESOS modality:
     * <p><lu>
     * <li>TIME_CONSERVATIVE:   consumes more energy, reducing the tail latency
     * <li>ENERGY_CONSERVATIVE: consumes less energy at a higher tail latency
     * </lu>
    */
    public enum Mode {
        TIME_CONSERVATIVE( "TC" ),
        ENERGY_CONSERVATIVE( "EC" );
        
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
     * Create a new PERF model.
     * 
     * @param frequencies    file containing a list of available frequencies.
     * 
     * @throws IOException if the file of frequencies doesn't exits or is malformed.
    */
    public CPUEnergyModel( final String frequencies ) throws IOException {
        this( readFrequencies( frequencies ) );
    }
    
    /**
     * Create a new PERF model.
     * 
     * @param frequencies    list of available frequencies.
    */
    public CPUEnergyModel( final List<Long> frequencies )
    {
        // Order the frequencies from higher to lower.
        Collections.sort( _frequencies = frequencies, Collections.reverseOrder() );
    }
    
    /**
     * Create a new PESOS model.
     * 
     * @param time_budget    time limit to complete a query (in ms).
     * @param mode           PESOS modality (see {@linkplain CPUEnergyModel.Mode Mode}).
     * @param frequencies    file containing a list of available frequencies.
     * 
     * @throws IOException if the file of frequencies doesn't exits or is malformed.
    */
    public CPUEnergyModel( final long time_budget, final Mode mode, final String frequencies ) throws IOException {
        this( time_budget, mode, readFrequencies( frequencies ) );
    }
    
    /**
     * Create a new PESOS model.
     * 
     * @param time_budget    time limit to complete a query (in ms).
     * @param mode           PESOS modality (see {@linkplain CPUEnergyModel.Mode Mode}).
     * @param frequencies    list of available frequencies.
    */
    public CPUEnergyModel( final long time_budget, final Mode mode, final List<Long> frequencies )
    {
        timeBudget = new Time( time_budget, TimeUnit.MILLISECONDS );
        this.mode = mode;
        // Order the frequencies from higher to lower.
        Collections.sort( _frequencies = frequencies, Collections.reverseOrder() );
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
        if (mode != null) loadRegressors();
        loadPostingsPredictors();
        loadEffectiveTimeEnergy();
    }
    
    public String getModelType( final boolean delimeters ) {
        if (mode == null) {
            return "PERF";
        } else {
            if (delimeters) return "PESOS_" + getMode() + "_" + getTimeBudget().getTimeMicroseconds()/1000L + "ms";
            else return "PESOS (" + getMode() + ", t = " + getTimeBudget().getTimeMicroseconds()/1000L + "ms)";
        }
    }
    
    public Mode getMode() {
        return mode;
    }

    public Time getTimeBudget() {
        return timeBudget;
    }

    private void loadPostingsPredictors() throws IOException
    {
        queries = new HashMap<>( 1 << 14 );
        
        FileReader fReader = new FileReader( POSTINGS_PREDICTORS );
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
        FileReader fReader = new FileReader( (mode == Mode.TIME_CONSERVATIVE) ?
                                                    REGRESSORS_TIME_CONSERVATIVE :
                                                    REGRESSORS_ENERGY_CONSERVATIVE );
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
        FileReader fReader = new FileReader( EFFECTIVE_TIME_ENERGY );
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
     * Evaluate the input parameter to decide which is the "best" frequency
     * to complete the current task.
     * 
     * @param now       time of evaluation.
     * @param params    list of parameters.
     * 
     * @return the "best" frequency, expressed in KHz.
    */
    @Override
    public Long eval( final Time now, final QueryInfo... params )
    {
        //Utils.LOGGER.debug( "Time: " + now + " => length: " + params.length );
        if (mode == null) return _frequencies.get( 0 );
        else return getFrequency( now, params );
    }

    private long getFrequency( final Time now, final QueryInfo[] queries )
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
            //PesosCPU2.writeResult( _frequency, elapsedEnergy, false );
            
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
            return getId() + "";
        }
    }
    
    public static class PESOSmodel extends CPUEnergyModel
    {
        /**
         * Create a new PESOS model.
         * 
         * @param time_budget    time limit to complete a query (in ms).
         * @param mode           PESOS modality (see {@linkplain CPUEnergyModel.Mode Mode}).
         * @param frequencies    file containing a list of available frequencies.
         * 
         * @throws IOException if the file of frequencies doesn't exits or is malformed.
        */
        public PESOSmodel( final long time_budget, final Mode mode, final String frequencies ) throws IOException {
            this( time_budget, mode, readFrequencies( frequencies ) );
        }
        
        public PESOSmodel( final long time_budget, final Mode mode, final List<Long> frequencies ) {
            super( time_budget, mode, frequencies );
        }
    }
    
    public static class PERFmodel extends CPUEnergyModel
    {
        /**
         * Create a new PESOS model.
         * 
         * @param time_budget    time limit to complete a query (in ms).
         * @param mode           PESOS modality (see {@linkplain CPUEnergyModel.Mode Mode}).
         * @param frequencies    file containing a list of available frequencies.
         * 
         * @throws IOException if the file of frequencies doesn't exits or is malformed.
        */
        public PERFmodel( final long time_budget, final Mode mode, final String frequencies ) throws IOException {
            this( time_budget, mode, readFrequencies( frequencies ) );
        }
        
        public PERFmodel( final List<Long> frequencies ) {
            super( frequencies );
        }
        
        public PERFmodel( final long time_budget, final Mode mode, final List<Long> frequencies ) {
            super( frequencies );
        }
    }
}