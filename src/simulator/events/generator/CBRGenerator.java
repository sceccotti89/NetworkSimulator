/**
 * @author Stefano Ceccotti
*/

package simulator.events.generator;

import simulator.events.Event;
import simulator.events.Packet;
import simulator.utils.Time;
import simulator.utils.Utils;

public class CBRGenerator extends EventGenerator
{
    public CBRGenerator( final Time duration,
                         final Time departureTime,
                         final Packet reqPacket,
                         final Packet resPacket )
    {
        super( duration, departureTime, Utils.INFINITE,
               reqPacket, resPacket, true, false, false );
    }

    @Override
    public Time computeDepartureTime( final Event e ) {
        return null;
    }
}