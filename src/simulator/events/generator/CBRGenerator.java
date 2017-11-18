/**
 * @author Stefano Ceccotti
*/

package simulator.events.generator;

import simulator.events.Packet;
import simulator.utils.Time;

/**
 * The Constant Bit Rate generator.
*/
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
