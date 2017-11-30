/**
 * @author Stefano Ceccotti
*/

package simulator.network;

import java.io.Serializable;

import simulator.utils.SizeUnit;
import simulator.utils.Utils;

public class Message implements Packet
{
    private byte[] message;
    
    private static final char HEX_DIGIT[] = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
    
    
    public Message( long size, SizeUnit unit ) {
        //setMessage( createMessage( unit.getBytes( size ) ) );
        byte[] data = Utils.serializeObject( new byte[(int) unit.getBytes( size )] );
        setMessage( data );
    }
    
    public <T extends Serializable> Message( T message ) {
        //setMessage( Utils.serializeObject( message ) );
        addMessage( message );
    }
    
    private <T extends Serializable> void addMessage( T message )
    {
        if (message instanceof byte[]) {
            setMessage( (byte[]) message );
        } else {
            setMessage( Utils.serializeObject( message ) );
        }
    }
    
    private void setMessage( byte[] message ) {
        this.message = message;
    }
    
    /**
     * Returns the size of the message.
    */
    public long getSize() {
        return message.length * Byte.BYTES;
    }
    
    /**
     * Creates an empty message with a length as the one specified in the input.
     * 
     * @param length    length of the message
    */
    /*private static byte[] createMessage( long length )
    {
        StringBuilder message = new StringBuilder();
        for (long i = 0; i < length; i++) {
            message.append( '0' );
        }
        return Utils.serializeObject( message.toString() );
    }*/
    
    /**
     * Returns the content (as a vector of bytes) of the message.
    */
    public byte[] getBytes() {
        return message;
    }
    
    public <T extends Serializable> T getMessage() {
        return Utils.deserializeObject( message );
    }
    
    public void print()
    {
        StringBuffer buf = new StringBuffer( message.length * 2 );
        for(int j = 0; j < message.length; j++) {
            buf.append( HEX_DIGIT[(message[j] >> 4) & 0x0f] );
            buf.append( HEX_DIGIT[message[j] & 0x0f] );
        }
        System.out.println( buf );
    }
}
