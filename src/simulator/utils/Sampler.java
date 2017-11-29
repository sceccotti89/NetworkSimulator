/**
 * @author Stefano Ceccotti
*/

package simulator.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Sampler
{
    private List<Pair<Double,Double>> values;
    private List<Long> elements;
    private double interval = 0;
    private double totalResult = 0;
    private String logFile;
    private Sampling mode;
    
    /**
     * Types of sampling mode.
     * <p><ul>
     * <li>CUMULATIVE: sum up all the values falling in the same bucket</br>
     * <li>AVERAGE: compute the average for each bucket</br>
     * <li>MIN: get the minimum among all the values of the same bucket</br>
     * <li>MAX: get the maximum among all the values of the same bucket
     * </ul>
    */
    public enum Sampling{ CUMULATIVE, AVERAGE, MIN, MAX };
    
    /**
     * Creates a new sampler to collect some informations with a default
     * initial capacity (2^12 elements).</br>
     * By default the sampling interval is 60 seconds.</br>
     * If the given interval is {@code null} or its time is <= 0 the sampling mode is ignored.
     * 
     * @param interval           the selected time interval.
     * @param mode               type of sampling (see {@linkplain Sampler.Sampling Sampling} enum).
     * @param logFile            name of the file where to save the results (it can be {@code null}).
     * 
     * @throws RuntimeException if the sampler name already exists.
     * @throws IOException if the path specified by the logFile parameter is not valid.
    */
    public Sampler( Time interval, String logFile, Sampling mode ) throws IOException {
        this( interval, logFile, mode, 1 << 12 );
    }
    
    /**
     * Creates a new sampler to collect some informations with a specified
     * initial capacity.</br>
     * By default the sampling interval is 60 seconds.</br>
     * If the given interval is {@code null} or its time is <= 0 the sampling mode is ignored.
     * 
     * @param interval           the selected time interval.
     * @param mode               type of sampling (see {@linkplain Sampler.Sampling Sampling} enum).
     * @param logFile            name of the file where to save the results (it can be {@code null}).
     * @param initialCapacity    the initial capacity of the sampler.
     * 
     * @throws RuntimeException if the sampler name already exists.
     * @throws IOException if the path specified by the logFile parameter is not valid.
    */
    public Sampler( Time interval, String logFile, Sampling mode, int initialCapacity ) throws IOException
    {
        values = new ArrayList<>( initialCapacity );
        elements = new ArrayList<>( initialCapacity );
        if (interval != null && interval.getTimeMicros() > 0) {
            values.add( new Pair<>( 0d, 0d ) );
            elements.add( 0L );
            this.interval = interval.getTimeMicros();
        }
        if (logFile != null) {
            Utils.checkFile( this.logFile = logFile );
        }
        this.mode = mode;
    }

    /**
     * Inserts a value into the corresponding interval.
     * 
     * @param currentValue    the current value contained in the bucket
     * @param value           the value to add
     * @param elements        number of elements in the current bucket
    */
    private double insertSampledValue( double currentValue, double value, long elements )
    {
        switch (mode) {
            case CUMULATIVE: return currentValue + value;
            case MIN:        return Math.min( currentValue, value );
            case MAX:        return Math.max( currentValue, value );
            case AVERAGE:    return currentValue + ((value - currentValue) / elements);
        }
        return currentValue;
    }

    /**
     * Scans the intervals to find the correct position over the last position, adding intervals if necessary.</br>
     * Any added interval as a value depending to the one given by the {@code valueUnit} input variable. 
     * 
     * @param nextInterval    the next interval to scan
     * @param interval        length of an interval
     * @param time            time to check
     * @param valueUnit       value used to "fill" each generated interval
     * @param elements        list of elements
     * @param values          list of values
     * 
     * @return the last interval generated by this method.
    */
    private Pair<Double, Double> scanInterval( double nextInterval, double time, double valueUnit )
    {
        Pair<Double,Double> currentInterval = values.get( values.size() - 1 );
        // Check for the interval position.
        while (nextInterval <= time) {
            elements.add( 1L );
            currentInterval = new Pair<>( nextInterval, valueUnit * interval );
            values.add( currentInterval );
            nextInterval += interval;
        }
        
        return currentInterval;
    }
    
    /**
     * Adds a new value to the specified sampler with the corresponding time intervals.</br>
     * If the starting time is earlier than the ending time,
     * the given value is "distributed" in multiple buckets along the entire interval;
     * if the sampler interval is less or equal than 0 it goes in a single separate bucket,
     * whose insertion is driven only by the ending time.</br>
     * 
     * @param startTime      starting time of the event.
     * @param endTime        ending time of the event.
     * @param value          value to add.
    */
    public void addSampledValue( Time startTime, Time endTime, double value ) {
        addSampledValue( startTime.getTimeMicros(), endTime.getTimeMicros(), value );
    }

    /**
     * Adds a new value to the specified sampler with the corresponding time intervals.</br>
     * If the starting time is earlier than the ending time, the given value is "distributed" in multiple buckets along the entire interval;
     * if the sampler interval is less or equal than 0 it goes in a single separate bucket,
     * whose insertion is driven only by the ending time.</br>
     * NOTE: all times MUST be expressed in microseconds.
     * 
     * @param startTime      starting time of the event
     * @param endTime        ending time of the event
     * @param value          value to add
    */
    public void addSampledValue( double startTime, double endTime, double value )
    {
        if (interval <= 0) {
            double nextInterval = (values.size() == 0) ? 0 : values.get( values.size() - 1 ).getFirst();
            // Add the value in a single bucket.
            elements.add( 1L );
            if (endTime >= nextInterval) {
                values.add( new Pair<>( endTime, value ) );
            } else {
                for (int i = values.size() - 1; i >= 0; i--) {
                    if (endTime >= values.get( i ).getFirst()) {
                        values.add( i, new Pair<>( endTime, value ) );
                        break;
                    }
                }
            }
            return;
        }
        
        double valueUnit = value / (endTime - startTime);
        Pair<Double,Double> currentInterval = values.get( values.size() - 1 );
        double nextInterval = currentInterval.getFirst() + interval;
        
        if (startTime >= nextInterval) {
            currentInterval = scanInterval( nextInterval, startTime, 0d );
            nextInterval = currentInterval.getFirst() + interval;
            
            if (endTime < nextInterval) {
                // Same interval.
                elements.set( elements.size()-1, 1L );
                currentInterval.setSecond( value );
            } else {
                // Different intervals.
                currentInterval.setSecond( valueUnit * (nextInterval - startTime) ); // Start time contribution.
                currentInterval = scanInterval( nextInterval, endTime, valueUnit );
                nextInterval = currentInterval.getFirst();
                currentInterval.setSecond( valueUnit * (endTime - nextInterval) );
            }
        } else {
            // Find the position of the start and end time.
            double prevInterval = nextInterval - interval;
            if (startTime >= prevInterval) {
                if (endTime < nextInterval) {
                    // Same current interval.
                    elements.set( elements.size()-1, elements.get( elements.size()-1 ) + 1L );
                    double currValue = currentInterval.getSecond();
                    currentInterval.setSecond( insertSampledValue( currValue, value, elements.get( elements.size()-1 ) ) );
                } else {
                    // Different intervals.
                    double currValue = currentInterval.getSecond();
                    currentInterval.setSecond( insertSampledValue( currValue, valueUnit * (nextInterval - startTime),
                                                                   elements.get( elements.size()-1 ) ) );
                    
                    currentInterval = scanInterval( nextInterval, endTime, valueUnit );
                    nextInterval = currentInterval.getFirst() + interval;
                    elements.set( elements.size()-1, 1L );
                    currentInterval.setSecond( valueUnit * (endTime - (nextInterval - interval)) );
                }
            } else {
                // Start before this interval.
                boolean addEnd = true;
                if (endTime < nextInterval) {
                    if (endTime >= prevInterval) {
                        elements.set( elements.size()-1, 1L );
                        double currValue = currentInterval.getSecond();
                        currentInterval.setSecond( insertSampledValue( currValue, valueUnit * (endTime - prevInterval),
                                                                       elements.get( elements.size()-1 ) ) );
                        addEnd = false;
                    }
                } else {
                    double currValue = currentInterval.getSecond();
                    currentInterval.setSecond( insertSampledValue( currValue, valueUnit * interval, elements.get( elements.size()-1 ) ) );
                    
                    currentInterval = scanInterval( nextInterval, endTime, valueUnit );
                    nextInterval = currentInterval.getFirst() + interval;
                    elements.set( elements.size()-1, 1L );
                    currentInterval.setSecond( valueUnit * (endTime - (nextInterval - interval)) );
                    addEnd = false;
                }
                
                // Find the correct position of the starting and ending interval.
                int index = values.size() - 2;
                double succInterval = prevInterval;
                prevInterval -= interval;
                Pair<Double,Double> startInterval = values.get( index );
                Pair<Double,Double> endInterval   = values.get( index );
                while (prevInterval >= 0) {
                    if (endTime >= prevInterval) {
                        if (startTime >= prevInterval) {
                            double currValue = startInterval.getSecond();
                            if (addEnd) {
                                // Same interval.
                                elements.set( index, elements.get( index ) + 1L );
                                startInterval.setSecond( insertSampledValue( currValue, value, elements.get( index ) ) );
                            } else {
                                // Founded start interval.
                                startInterval.setSecond( currValue + valueUnit * (succInterval - startTime) );
                            }
                            break;
                        } else {
                            // Different interval.
                            if (addEnd) {
                                // Add the energy of (endTime - previous interval).
                                addEnd = false;
                                elements.set( index, elements.get( index ) + 1L );
                                double currValue = endInterval.getSecond();
                                endInterval.setSecond( insertSampledValue( currValue, valueUnit * (endTime - prevInterval), elements.get( index ) ) );
                            } else {
                                double currValue = startInterval.getSecond();
                                startInterval.setSecond( insertSampledValue( currValue, valueUnit * interval, elements.get( index ) ) );
                            }
                        }
                    }
                    
                    index--;
                    startInterval = values.get( index );
                    endInterval   = values.get( index );
                    
                    succInterval -= interval;
                    prevInterval -= interval;
                }
            }
        }
        
        totalResult += value;
    }
    
    public List<Pair<Double,Double>> getValues() {
        return values;
    }

    public double getTotalResult() {
        return totalResult;
    }
    
    public String getLogFile() {
        return logFile;
    }
}