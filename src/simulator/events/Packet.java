/**
 * @author Stefano Ceccotti
*/

package simulator.events;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import simulator.utils.Size;
import simulator.utils.SizeUnit;

// FIXME DEPRECATED: Utilizzare Message al posto di packet quando implementere' i protocolli di rete.
public class Packet
{
    private Size<Long> _size;
    private Map<String,Object> _contents;
    
    
    
    
    /**
     * Type of packet used to tell the event generator that it's defined by the user.</br>
     * The definition of a dynamic packet is the following:
     * <p>
     * {@code new Packet( -1, null )}
    */
    public static final Packet DYNAMIC = new Packet( -1, null );
    
    
    
    public Packet( long pktSize, SizeUnit sizeType )
    {
        _size = new Size<Long>( pktSize, sizeType );
        _contents = new HashMap<>();
    }
    
    public long getSize() {
        return _size.getSize();
    }
    
    public SizeUnit getSizeType() {
        return _size.getSizeUnit();
    }
    
    public void addContent( String field, Object value ) {
        _contents.put( field, value );
    }
    
    public boolean hasContent( String field ) {
        return _contents.containsKey( field );
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getContent( String field ) {
        return (T) _contents.get( field );
    }
    
    public Map<String,Object> getContents() {
        return _contents;
    }
    
    public boolean isDynamic() {
        return _size.getSize() == -1;
    }
    
    public long getSizeInBits() {
        return (long) _size.getBits();
    }
    
    public long getSizeInBytes() {
        return (long) _size.getBytes();
    }
    
    @Override
    public Packet clone()
    {
        Packet p = new Packet( _size.getSize(), _size.getSizeUnit() );
        if (!_contents.isEmpty()) {
            for (Entry<String,Object> entry : _contents.entrySet()) {
                Object value = entry.getValue();
                try {
                    Method cloneMethod = value.getClass().getMethod( "clone" );
                    value = cloneMethod.invoke( value );
                } catch ( Exception e ) {
                    // Method "clone" not present for this object.
                    //e.printStackTrace();
                }
                
                p.addContent( entry.getKey(), value );
            }
        }
        
        return p;
    }
}
