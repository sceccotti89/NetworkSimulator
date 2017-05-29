/**
 * @author Stefano Ceccotti
*/

package simulator.network;

import java.io.Serializable;

import simulator.utils.SizeUnit;
import simulator.utils.Utils;

public class Message
{
    private byte[] message;
    private NetworkLayer layer;
    
    
    public Message( final long size, final SizeUnit unit, final NetworkLayer layer ) {
        setMessage( createMessage( (long) unit.getBytes( size ) ) );
        this.layer = layer;
    }
    
    public <T extends Serializable> Message( final T message, final NetworkLayer layer ) {
        addMessage( message );
        this.layer = layer;
    }
    
    public <T extends Serializable> void addMessage( final T message ) {
        setMessage( Utils.serializeObject( message ) );
    }
    
    private void setMessage( final byte[] message ) {
        this.message = message;
    }
    
    /**
     * Creates an empty message with a length as the one specified in the input.
     * 
     * @param length    length of the message
    */
    private static byte[] createMessage( final long length ) {
        StringBuilder message = new StringBuilder();
        for (long i = 0; i < length; i++) {
            message.append( '0' );
        }
        return Utils.serializeObject( message );
    }
    
    public void addMessage( byte[] message ) {
        this.message = message;
    }
    
    public <T extends Serializable> T getMessage() {
        return Utils.deserializeObject( message );
    }
    
    public NetworkLayer getLayer() {
        return layer;
    }
}