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
    
    
    public Message( long size, SizeUnit unit, NetworkLayer layer ) {
        setMessage( createMessage( (long) unit.getBytes( size ) ) );
        this.layer = layer;
    }
    
    public <T extends Serializable> Message( T message, NetworkLayer layer ) {
        addMessage( message );
        this.layer = layer;
    }
    
    public <T extends Serializable> void addMessage( T message )
    {
        if (message instanceof byte[]){
            setMessage( (byte[]) message );
        } else {
            setMessage( Utils.serializeObject( message ) );
        }
    }
    
    private void setMessage( byte[] message ) {
        this.message = message;
    }
    
    /**
     * Creates an empty message with a length as the one specified in the input.
     * 
     * @param length    length of the message
    */
    private static byte[] createMessage( long length )
    {
        StringBuilder message = new StringBuilder();
        for (long i = 0; i < length; i++) {
            message.append( '0' );
        }
        return Utils.serializeObject( message );
    }
    
    public <T extends Serializable> T getMessage() {
        return Utils.deserializeObject( message );
    }
    
    public NetworkLayer getLayer() {
        return layer;
    }
}
