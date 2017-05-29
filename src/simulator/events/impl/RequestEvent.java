/**
 * @author Stefano Ceccotti
*/

package simulator.events.impl;

import simulator.core.Agent;
import simulator.events.Event;
import simulator.events.Packet;
import simulator.utils.Time;

public class RequestEvent extends Event
{
    public RequestEvent( final Time time, final Agent from, final Agent to, final Packet pkt ) {
        super( time, from, to, pkt );
    }
    
    @Override
    public String toString() {
        return "Request Event: " + super.toString();
    }
}