
package simulator.core;

import java.util.concurrent.TimeUnit;

import simulator.network.NetworkNode;

public abstract class EventGenerator
{
    protected Time _time;
    protected Time _duration;
    protected Time _arrivalTime;
    protected Time _serviceTime;
    
    public EventGenerator( final Time duration, final Time arrivalTime, final Time serviceTime )
    {
        _time = new Time( 0, TimeUnit.MILLISECONDS );
        _duration = duration;
        _arrivalTime = arrivalTime;
        _serviceTime = serviceTime;
    }
    
    public abstract void generate( NetworkNode destNode );
    public abstract Event nextEvent();
}