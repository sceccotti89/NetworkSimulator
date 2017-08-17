/**
 * @author Stefano Ceccotti
*/

package simulator.events.generator;

import simulator.events.Event;
import simulator.events.Packet;
import simulator.utils.Time;

public class SinkGenerator extends EventGenerator
{
    public SinkGenerator( final Time duration,
                          final long maxPacketsInFly,
                          final Packet reqPacket,
                          final Packet resPacket )
    {
        super( duration, Time.ZERO, maxPacketsInFly,
               reqPacket, resPacket,
               false, false, true );
    }
    
    @Override
    public Time computeDepartureTime( final Event e ) {
        return null;
    }
}