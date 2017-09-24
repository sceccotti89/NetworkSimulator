/**
 * @author Stefano Ceccotti
*/

package simulator.events.generator;

import simulator.events.Packet;
import simulator.utils.Time;

public class CBRGenerator extends EventGenerator
{
    public CBRGenerator( final Time startTime,
                         final Time duration,
                         final Time departureTime,
                         final Packet reqPacket,
                         final Packet resPacket )
    {
        super( duration, departureTime, reqPacket, resPacket );
        startAt( startTime );
    }
}