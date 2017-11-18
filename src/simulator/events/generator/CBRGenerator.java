/**
 * @author Stefano Ceccotti
*/

package simulator.events.generator;

import simulator.events.Packet;
import simulator.utils.Time;

public class CBRGenerator extends EventGenerator
{
    public CBRGenerator( Time startTime,
                         Time duration,
                         Time departureTime,
                         Packet reqPacket,
                         Packet resPacket )
    {
        super( duration, departureTime, reqPacket, resPacket );
        startAt( startTime );
    }
}
