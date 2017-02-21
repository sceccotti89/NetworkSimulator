/**
 * @author Stefano Ceccotti
*/

package simulator.coordinator;

import simulator.core.Time;
import simulator.network.NetworkNode;

public abstract class EventGenerator
{
    protected Time _duration;
    protected Time _arrivalTime;
    protected Time _serviceTime;
    
    public EventGenerator( final Time duration, final Time arrivalTime, final Time serviceTime )
    {
        _duration = duration;
        _arrivalTime = arrivalTime;
        _serviceTime = serviceTime;
    }
    
    public abstract void generate( final EventHandler evHandler, final NetworkNode destNode );
    public abstract Event nextEvent( final EventHandler evHandler );
}