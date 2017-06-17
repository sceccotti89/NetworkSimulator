/**
 * @author Stefano Ceccotti
*/

package simulator.utils;

import java.util.concurrent.TimeUnit;

class ImmutableTime extends Time
{
    public ImmutableTime( final long time, final TimeUnit unit ) {
        super( time, unit );
    }
    
    @Override
    public Time setTime( final long time, final TimeUnit unit ) {		
        throw new UnsupportedOperationException( "You can't change this!" );
    }
    
    @Override
    public Time setTime( final Time time ) {
        throw new UnsupportedOperationException( "You can't change this!" );
    }
    
    @Override
    public Time addTime( final long i, final TimeUnit unit ) {
        throw new UnsupportedOperationException( "You can't change this!" );
    }
    
    @Override
    public Time addTime( final Time i ) {
        throw new UnsupportedOperationException( "You can't change this!" );
    }
    
    @Override
    public Time subTime( final long i, final TimeUnit unit ) {
        throw new UnsupportedOperationException( "You can't change this!" );
    }
    
    @Override
    public Time subTime( final Time i ) {
        throw new UnsupportedOperationException( "You can't change this!" );
    }
}

public class Time implements Comparable<Time>
{
    public static final Time ZERO     = new ImmutableTime(0, TimeUnit.MICROSECONDS);
    public static final Time SECOND   = new ImmutableTime(1, TimeUnit.SECONDS);
    public static final Time HOUR     = new ImmutableTime(1, TimeUnit.HOURS);
    public static final Time INFINITE = new ImmutableTime(Long.MAX_VALUE, TimeUnit.MICROSECONDS);
    
    protected TimeUnit unit;
    
    /**
     * Type of time used to tell the event generator that it's defined by the user.</br>
     * The definition of a dynamic time is one as following:
     * <p>
     * {@code new Time( -1, TimeUnit.* )}
     * <p>
     * where the first parameter is a number < 0, with any {@linkplain TimeUnit}.
    */
    public static final Time DYNAMIC = new ImmutableTime( -1, TimeUnit.MICROSECONDS );
    
    private long time;
    
    
    
    public Time( final long time, final TimeUnit unit ) {
        this.time = unit.toMicros( time );
        this.unit = unit;
    }
    
    public Long getTimeMicroseconds() {
        return time;
    }
    
    public TimeUnit getTimeUnit() {
        return unit;
    }
    
    public Time setTime( final long time, final TimeUnit unit ) {
        this.time = unit.toMicros( time );
        return this;
    }
    
    public Time setTime( final Time time ) {
        this.time = time.getTimeMicroseconds();
        return this;
    }
    
    public Time addTime( final long i, final TimeUnit unit ) {
        time += unit.toMicros( i );
        return this;
    }
    
    public Time addTime( final Time i ) {
        addTime( i.getTimeMicroseconds(), TimeUnit.MICROSECONDS );
        return this;
    }
    
    @Override
    public int compareTo( final Time o ) {
        return getTimeMicroseconds().compareTo( o.getTimeMicroseconds() ); 
    }
    
    /**
     * Subtract i unit of time from this obj. Result will never be negative.
     * 
     * @param i
     * @param unit
     * @return
    */
    public Time subTime( final long i, final TimeUnit unit ) {
        long l = unit.toMicros( i );
        if (l > time) {
            time = 0;
        } else {
            time -= unit.toMicros( i );
        }
        return this;
    }
    
    /**
     * Subtract i time from this. Result will never be negative.
     * @param i
     * @return
    */
    public Time subTime( final Time i ) {
        return subTime( i.getTimeMicroseconds(), TimeUnit.MICROSECONDS );
    }
    
    /**
     * Checks if its time is dynamic.
     * 
     * @return {@code true} if the delay is dynamic, {@code false} otherwise
    */
    public boolean isDynamic() {
        return time < 0;
    }
    
    public double convert( final TimeUnit tUnit )
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
    
    @Override
    public String toString() {
        return Long.toString( time );
    }
    
    @Override
    public Time clone() {
        return new Time( time, TimeUnit.MICROSECONDS );
    }
}