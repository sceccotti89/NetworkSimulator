/**
 * @author Stefano Ceccotti
*/

package simulator.events.impl;

import simulator.core.Agent;
import simulator.events.Event;
import simulator.events.Packet;
import simulator.utils.Time;

public class ResponseEvent extends Event
{
    public ResponseEvent( Time time, Agent from, Agent to, Packet packet ) {
        super( time, from, to, packet );
    }
    
    @Override
    public String toString() {
        return "Response Event: " + super.toString();
    }
}
