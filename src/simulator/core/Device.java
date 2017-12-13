/**
 * @author Stefano Ceccotti
*/

package simulator.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.newdawn.slick.util.ResourceLoader;

import simulator.events.EventScheduler;
import simulator.utils.Sampler;
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
    
    
    
    public Device( String specFile ) throws IOException {
        build( specFile );
    }
    
    /**
     * Creates a new Device object.
     * 
     * @param name              name of the device.
     * @param frequency_file    file where the frequencies are taken.
    */
    public Device( String name, String frequency_file ) throws IOException {
        this( name, readFrequencies( frequency_file ) );
    }
    
    /**
     * Creates a new Device object,
     * specifing only the name and its available frequencies.
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
    }
    
    /**
     * Builds the device using the input file which describes its internal structure.</br>
     * The formatting of the file must follow the JSON rules, where the name of the labels
     * are defined by the user, as well as their values.
     * 
     * @param inputFile    the input file used to define the device.
    */
    public abstract void build( String inputFile ) throws IOException;
    
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
     * Returns the percentage of utilization of this device respect to the input time.
     * 
     * @param time    time to check the device utilization
    */
    public abstract double getUtilization( Time time );
    
    /**
     * Shutdowns the device, closing all the opened resources
     * and writing on file any registered sampling.
    */
    public void shutdown() throws IOException
    {
        if (_model != null) {
            _model.close();
        }
    }
}
