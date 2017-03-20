/**
 * @author Stefano Ceccotti
*/

package simulator.core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import simulator.Agent;
import simulator.manager.Model;

public abstract class Device
{
    protected List<Long> _frequencies;
    protected Agent _agent;
    
    protected Model<?,?> _model;
    
    protected long _frequency;
    
    private Time delay;
    
    
    public Device( final String frequency_file ) throws IOException {
        this( readFrequencies( frequency_file ) );
    }
    
    public Device( final List<Long> frequencies ) {
        _frequencies = frequencies;
        // By default the frequency is setted as the first of the list (tipically the maximum).
        _frequency = _frequencies.get( 0 );
    }
    
    /**
     * Reads the list of available frequencies for the input machine.
     * 
     * @param frequencies_file    file where the frequencies are taken
     * 
     * @return the list of available frequencies
    */
    protected static List<Long> readFrequencies( final String frequencies_file ) throws IOException
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
    
    public void setAgent( final Agent agent )
    {
        _agent = agent;
        if (_model != null) {
            _model.setAgent( agent );
        }
    }
    
    public Agent getAgent() {
        return _agent;
    }
    
    /**
     * Assign the model.
    */
    public void setModel( final Model<?,?> model ) {
        _model = model;
        _model.setAgent( _agent );
    }
    
    public Model<?,?> getModel() {
        return _model;
    }
    
    public Time getDelay() {
        return delay.clone();
    }
    
    /**
     * Returns the maximum frequency available on this device.
    */
    public long getMaxFrequency() {
        return _frequencies.get( 0 );
    }
    
    /**
     * Returns the list of available frequencies for this device.
    */
    public List<Long> getFrequencies() {
        return Collections.unmodifiableList( _frequencies );
    }
    
    /**
     * Returns the time spent to compute the input task.</br>
     * The time is expressed in ms.
     * 
     * @param task    the input task
     * 
     * @return time spent to compute the input task.
    */
    public abstract Time timeToCompute( final Task task );
    
    /**
     * Gets the quantity of energy required to compute the input task.
     * 
     * @param task    the input task
     * 
     * @return the total energy consumption, expressed in Joule.
    */
    public abstract double computeEnergyConsumption( final Task task );
    
    /**
     * Set the current frequency expressed in KHz.
     * 
     * @param frequency    the CPU frequency
    */
    public void setFrequency( final long frequency ) {
        _frequency = frequency;
    }
    
    public long getFrequency() {
        return _frequency;
    }
    
    /**
     * Evaluate which is the "best" frequency to use
     * to process the current task.
    */
    public abstract void evalFrequency( final Task task );
    
    /**
     * Returns the associated ID.
    */
    public abstract String getID();
}