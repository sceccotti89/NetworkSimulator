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
    public ResponseEvent( final Time time, final Agent from, final Agent to, final Packet packet ) {
        super( time, from, to, packet );
    }
    
    @Override
    public String toString() {
        return "Response Event: " + super.toString();
    }
}