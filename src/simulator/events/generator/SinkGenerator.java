/**
 * @author Stefano Ceccotti
*/

package simulator.events.generator;

import simulator.events.Packet;
import simulator.utils.Time;

public class SinkGenerator extends EventGenerator
{
    public SinkGenerator( final Time duration,
                          final Packet reqPacket,
                          final Packet resPacket )
    {
        super( duration, Time.ZERO,
               reqPacket, resPacket,
               false, true );
        
        makeAnswer( false );
    }
}