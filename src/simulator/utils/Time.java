/**
 * @author Stefano Ceccotti
*/

package simulator.utils;

import java.util.concurrent.TimeUnit;

class ImmutableTime extends Time
{
    public ImmutableTime( long time, TimeUnit unit ) {
        super( time, unit );
    }
    
    @Override
    public Time setTime( long time, TimeUnit unit ) {		
        throw new UnsupportedOperationException( "You can't change this!" );
    }
    
    @Override
    public Time setTime( Time time ) {
        throw new UnsupportedOperationException( "You can't change this!" );
    }
    
    @Override
    public Time addTime( long i, TimeUnit unit ) {
        throw new UnsupportedOperationException( "You can't change this!" );
    }
    
    @Override
    public Time addTime( Time i ) {
        throw new UnsupportedOperationException( "You can't change this!" );
    }
    
    @Override
    public Time subTime( long i, TimeUnit unit ) {
        throw new UnsupportedOperationException( "You can't change this!" );
    }
    
    @Override
    public Time subTime( Time i ) {
        throw new UnsupportedOperationException( "You can't change this!" );
    }
}

public class Time implements Comparable<Time>
{
    public static final Time ZERO     = new ImmutableTime( 0, TimeUnit.MICROSECONDS );
    public static final Time SECOND   = new ImmutableTime( 1, TimeUnit.SECONDS );
    public static final Time HOUR     = new ImmutableTime( 1, TimeUnit.HOURS );
    public static final Time INFINITE = new ImmutableTime( Long.MAX_VALUE, TimeUnit.MICROSECONDS );
    
    protected TimeUnit unit;
    
    /**
     * Type of time used to tell the event generator that it's defined by the user.</br>
     * The definition of a dynamic time is one as following:
     * <p>
     * {@code new Time( -1, TimeUnit.* )}
     * <p>
     * where the first parameter is a number < 0, with any {@linkplain TimeUnit}.
    */
    //public static final Time DYNAMIC = new ImmutableTime( -1, TimeUnit.MICROSECONDS );
    
    /** Current time. */
    private long time;
    
    
    
    public Time( long time, TimeUnit unit ) {
        this.time = unit.toMicros( time );
        this.unit = unit;
    }
    
    public Long getTimeMicros() {
        return time;
    }
    
    public Long getTimeMillis() {
        return time / 1000L;
    }
    
    public Long getTimeSeconds() {
        return time / 1000000L;
    }
    
    public Long getTimeMinutes() {
        return time / 60000000L;
    }
    
    public Long getTimeHours() {
        return time / 3600000000L;
    }
    
    public Long getTimeDays() {
        return time / 86400000000L;
    }
    
    public TimeUnit getTimeUnit() {
        return unit;
    }
    
    public Time setTime( long time, TimeUnit unit ) {
        this.time = unit.toMicros( time );
        return this;
    }
    
    public Time setTime( Time time ) {
        this.time = time.getTimeMicros();
        return this;
    }
    
    public Time addTime( long i, TimeUnit unit ) {
        time += unit.toMicros( i );
        return this;
    }
    
    public Time addTime( Time i ) {
        addTime( i.getTimeMicros(), TimeUnit.MICROSECONDS );
        return this;
    }
    
    @Override
    public int compareTo( Time o ) {
        return getTimeMicros().compareTo( o.getTimeMicros() ); 
    }
    
    /**
     * Subtract i units of time from this object. Result will never be negative.
     * 
     * @param i
     * @param unit
     * @return
    */
    public Time subTime( long i, TimeUnit unit )
    {
        long l = unit.toMicros( i );
        if (l > time) {
            time = 0;
        } else {
            time -= l;
        }
        return this;
    }
    
    /**
     * Subtract i units of time from this object. Result will never be negative.
     * @param i
     * @return
    */
    public Time subTime( Time i ) {
        return subTime( i.getTimeMicros(), TimeUnit.MICROSECONDS );
    }
    
    /**
     * Returns the maximum time between this and the input one.
     * @param t
     * @return
    */
    public Time max( Time t ) {
        if (t.compareTo( this ) > 0) setTime( t );
        return this;
    }
    
    /**
     * Returns the minimum time between this and the input one.
     * @param t
     * @return
    */
    public Time min( Time t ) {
        if (t.compareTo( this ) < 0) setTime( t );
        return this;
    }
    
    /**
     * Checks if its time is dynamic.
     * 
     * @return {@code true} if the delay is dynamic, {@code false} otherwise
    */
    public boolean isDynamic() {
        return time < 0;
    }
    
    public double convert( TimeUnit tUnit )
    {
        switch ( tUnit ) {
            case DAYS:         return time / 24d / 60d / 60d / 1000d / 1000d;
            case HOURS:        return time / 60d / 60d / 1000d / 1000d;
            case MINUTES:      return time / 60d / 1000d / 1000d;
            case SECONDS:      return time / 1000d / 1000d;
            case MILLISECONDS: return time / 1000d;
            case MICROSECONDS: return time;
            case NANOSECONDS:  return time * 1000d;
        }
        
        return time;
    }
    
    public void printTime()
    {
        long hours   =  time/3600000000L;
        long minutes = (time - hours*3600000000L)/60000000L;
        long seconds = (time - hours*3600000000L - minutes*60000000L)/1000000L;
        long millis  =  time - hours*3600000000L - minutes*60000000L - seconds*1000000L;
        System.out.println( "Simulation completed in " + hours + "h:" + minutes + "m:" + seconds + "s:" + millis + "ms" );
    }
    
    @Override
    public String toString() {
        return Long.toString( time );
    }
    
    @Override
    public Time clone() {
        return new Time( time, TimeUnit.MICROSECONDS );
    }
}
