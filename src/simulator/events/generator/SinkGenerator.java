/**
 * @author Stefano Ceccotti
*/

package simulator.events.generator;

import simulator.events.Packet;
import simulator.utils.Time;

public class SinkGenerator extends EventGenerator
{
    public SinkGenerator( Time duration,
                          Packet reqPacket,
                          Packet resPacket )
    {
        super( duration, Time.ZERO, reqPacket, resPacket );
        makeAnswer( false );
    }
}
