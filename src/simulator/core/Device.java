/**
 * @author Stefano Ceccotti
*/

package simulator.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.newdawn.slick.util.ResourceLoader;

import simulator.events.EventScheduler;
import simulator.utils.Pair;
import simulator.utils.Sampler;
import simulator.utils.Sampler.Sampling;
import simulator.utils.Time;

/**
 * Class representing a generic device (e.g. CPU, RAM, etc.).</br>
 * You can associate a proper cost model for the evaluation of, e.g., the energy consumption,
 * when {@linkplain #timeToCompute(Task) timeToCompute} is called.</br>
 * In case a model will not be associated just set {@code Object}
 * for the {@linkplain I} and {@linkplain O} parameters.</br>
 * With the {@linkplain #addSampler(String, Sampler) addSampler} method a new sampler
 * is added to this device, associated with a unique identifier used to retrieve its results.
 * 
 * @param <I>    type of the Input parameters of the {@link Model#eval(Object...) eval} method
 *               of the associated model.
 * @param <O>    type of the Output parameters of the {@link Model#eval(Object...) eval} method
 *               of the associated model.
*/
public abstract class Device<I,O>
{
    private Agent _agent;
    
    protected String _name;
    protected List<Long> _frequencies;
    protected EventScheduler _evtScheduler;
    protected Time _time;
    
    protected Model<I,O> _model;
    
    protected long _frequency;
    
    // Sampling list.
    private Map<String,Sampler> samplings;
    
    
    
    /**
     * Create a new Device object.
     * 
     * @param name              name of the device
     * @param frequency_file    file where the frequencies are taken.
    */
    public Device( String name, String frequency_file ) throws IOException {
        this( name, readFrequencies( frequency_file ) );
    }
    
    /**
     * Create a new Device object.
     * 
     * @param name           name of the device.
     * @param frequencies    list of frequencies.
    */
    public Device( String name, List<Long> frequencies )
    {
        _name = name;
        _time = new Time( 0, TimeUnit.MILLISECONDS );
        
        // Order the frequencies from lower to higher.
        //Collections.sort( _frequencies = frequencies );
        
        _frequencies = frequencies;
        // By default the frequency is setted as the maximum one.
        _frequency = getMaxFrequency();
        
        samplings = new HashMap<>( 4 );
    }
    
    /**
     * Reads the list of available frequencies for this device.
     * 
     * @param frequencies_file    file where the frequencies are taken
     * 
     * @return the list of available frequencies
    */
    protected static List<Long> readFrequencies( String frequencies_file ) throws IOException
    {
        final Pattern p = Pattern.compile( "\\w+" );
        
        InputStream loader = ResourceLoader.getResourceAsStream( frequencies_file );
        BufferedReader frequencyReader = new BufferedReader( new InputStreamReader( loader ) );
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
    
    /**
     * Assigns the cost model.
    */
    public void setModel( Model<I,O> model ) {
        _model = model;
    }
    
    public Model<I,O> getModel() {
        return _model;
    }
    
    public void setAgent( Agent agent ) {
        _agent = agent;
    }
    
    public Agent getAgent() {
        return _agent;
    }
    
    public void setEventScheduler( EventScheduler evtScheduler ) {
        _evtScheduler = evtScheduler;
    }
    
    protected EventScheduler getEventScheduler() {
        return _evtScheduler;
    }
    
    /**
     * Sets the device time.
     * 
     * @param time    time to set.
    */
    public void setTime( Time time ) {
        _time.setTime( time );
    }
    
    /**
     * Returns the current time of this device.
    */
    public Time getTime() {
        return _time.clone();
    }
    
    /**
     * Adds a new sampler to collect some informations with a default
     * initial capacity (2^12 elements).</br>
     * By default the sampling interval is 60 seconds.</br>
     * If the given interval is {@code null} or its time is <= 0 the sampling mode is ignored.
     * 
     * @param samplerId    name of the sampler. Must be UNIQUE.
     * @param interval     the selected time interval.
     * @param mode         type of sampling (see {@linkplain Sampler.Sampling Sampling} enum).
     * @param logFile      name of the file where to save the results (it can be {@code null}).
     * 
     * @throws RuntimeException if the sampler name already exists.
     * @throws IOException if the path specified by the logFile parameter is not valid.
    */
    public void addSampler( String samplerId, Time interval,
                            Sampling mode, String logFile ) throws IOException {
        addSampler( samplerId, interval, mode, logFile, 1 << 12 );
    }
    
    /**
     * Adds a new sampler to collect some informations with a specified
     * initial capacity.</br>
     * By default the sampling interval is 60 seconds.</br>
     * If the given interval is {@code null} or its time is <= 0 the sampling mode is ignored.
     * 
     * @param samplerId          name of the sampler. Must be UNIQUE.
     * @param interval           the selected time interval.
     * @param mode               type of sampling (see {@linkplain Sampler.Sampling Sampling} enum).
     * @param logFile            name of the file where to save the results (it can be {@code null}).
     * @param initialCapacity    the initial capacity of the sampler.    
     * 
     * @throws RuntimeException if the sampler name already exists.
     * @throws IOException if the path specified by the logFile parameter is not valid.
    */
    public void addSampler( String samplerId, Time interval,
                            Sampling mode, String logFile,
                            int initialCapacity ) throws IOException {
        addSampler( samplerId, new Sampler( interval, logFile, mode, initialCapacity ) );
    }
    
    /**
     * Adds a new sampler to collect some informations about this device.
     * 
     * @param samplerId    name of the sampler. Must be UNIQUE.
     * @param sampler      the sampler object
     * 
     * @throws RuntimeException if the samplerId already exists.
    */
    public void addSampler( String samplerId, Sampler sampler )
    {
        if (samplings.containsKey( samplerId )) {
            throw new RuntimeException( "Selected name \"" + samplerId + "\" already exists." );
        }
        samplings.put( samplerId, sampler );
    }
    
    /**
     * Returns the list of available frequencies for this device in increasing order.
    */
    public List<Long> getFrequencies() {
        return new ArrayList<>( _frequencies );
    }
    
    public long getMaxFrequency() {
        return _frequencies.get( _frequencies.size() - 1 );
    }
    
    public long getMinFrequency() {
        return _frequencies.get( 0 );
    }
    
    public long getFrequency() {
        return _frequency;
    }

    /**
     * Sets the new frequency, expressed in KHz.
     * 
     * @param frequency    the clock frequency.
    */
    public void setFrequency( long frequency ) {
        _frequency = frequency;
    }
    
    /**
     * Increases the current frequency by the given steps.</br>
     * The new frequency cannot be higher than the maximum.
     * 
     * @param steps    number of steps to increase.
    */
    public void increaseFrequency( int steps )
    {
        int index = _frequencies.indexOf( _frequency );
        index = Math.min( index + steps, _frequencies.size() - 1 );
        setFrequency( _frequencies.get( index ) );
    }
    
    /**
     * Decreases the current frequency by the given steps.</br>
     * The new frequency cannot be lower than the minimum.
     * 
     * @param steps    number of steps to decrease.
    */
    public void decreaseFrequency( int steps )
    {
        int index = _frequencies.indexOf( _frequency );
        index = Math.max( index - steps, 0 );
        setFrequency( _frequencies.get( index ) );
    }

    /**
     * Returns the time spent to compute the given task.</br>
     * NOTE: time is expressed in microseconds.
     * 
     * @param task    the input task
     * 
     * @return time spent to compute the input task.
    */
    public abstract Time timeToCompute( Task task );
    
    /**
     * Adds a new value to the specified sampler with the corresponding time intervals.</br>
     * If the starting time is earlier than the ending time,
     * the given value is "distributed" in multiple buckets along the entire interval;
     * if the sampler interval is less or equal than 0 it goes in a single separate bucket,
     * whose insertion is driven by the ending time.</br>
     * 
     * @param sampler        the specified sampler in which insert the value.
     * @param startTime      starting time of the event.
     * @param endTime        ending time of the event.
     * @param value          value to add.
    */
    protected void addSampledValue( String sampler, Time startTime, Time endTime, double value ) {
        addSampledValue( sampler, startTime.getTimeMicros(), endTime.getTimeMicros(), value );
    }
    
    /**
     * Adds a new value to the specified sampler with the corresponding time intervals.</br>
     * If the starting time is earlier than the ending time,
     * the given value is "distributed" in multiple buckets along the entire interval;
     * if the sampler interval is less or equal than 0 it goes in a single separate bucket,
     * whose insertion is driven by the ending time.</br>
     * NOTE: all times MUST be expressed in microseconds.
     * 
     * @param sampler        the specified sampler in which insert the value.
     * @param startTime      starting time of the event.
     * @param endTime        ending time of the event.
     * @param value          value to add.
    */
    protected void addSampledValue( String sampler, double startTime, double endTime, double value )
    {
        Sampler samplerObj = samplings.get( sampler );
        if (samplerObj != null) {
            samplerObj.addSampledValue( startTime, endTime, value );
        }
    }
    
    /**
     * Returns the list of values sampled by the requested sampler.
     * 
     * @param sampler    the requested sampler
     * 
     * @return {@code null} if the requested sampler is not present,
     *         its list of values otherwise.
    */
    public List<Pair<Double,Double>> getSampledValues( String sampler )
    {
        if (!samplings.containsKey( sampler )) {
            return null;
        } else {
            return samplings.get( sampler ).getValues();
        }
    }
    
    /**
     * Returns the sum of all the results sampled by the given sampler.
     * 
     * @param sampler    the requested sampler
     * 
     * @return {@code null} if the requested sampler is not present,
     *         its result value otherwise.
    */
    public Double getResultSampled( String sampler ) {
        if (!samplings.containsKey( sampler )) {
            return null;
        } else {
            return samplings.get( sampler ).getTotalResult();
        }
    }
    
    /**
     * Returns the percentage of utilization of this device respect to the input time.
     * 
     * @param time    time to check the device utilization
    */
    public abstract double getUtilization( Time time );
    
    /**
     * Returns the associated ID.
    */
    public abstract String getID();
    
    /**
     * Shutdowns the device, closing all the opened resources
     * and writing on file any registered sampling.
    */
    public void shutdown() throws IOException
    {
        for (Sampler sampler : samplings.values()) {
            String logFile = sampler.getLogFile();
            if (logFile != null) {
                PrintWriter writer = new PrintWriter( logFile, "UTF-8" );
                for (Pair<Double,Double> point : sampler.getValues()) {
                    writer.println( point.getFirst() + " " + point.getSecond() );
                }
                writer.close();
            }
        }
        
        if (_model != null) {
            _model.close();
        }
    }
}
