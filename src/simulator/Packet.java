/**
 * @author Stefano Ceccotti
*/

package simulator;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import simulator.utils.SizeUnit;

public class Packet
{
    private long _pktSize;
    private SizeUnit _sizeType = SizeUnit.BYTE;
    private Map<String,Object> _contents;
    
    /**
     * Type of packet used to tell the event generator that it's defined by the user.</br>
     * The definition of a dynamic packet is the following:
     * <p>
     * {@code new Packet( -1, null )}
    */
    public static final Packet DYNAMIC = new Packet( -1, null );
    
    public Packet( final long pktSize, final SizeUnit sizeType )
    {
        _pktSize = pktSize;
        _sizeType = sizeType;
        _contents = new HashMap<>();
    }
    
    public long getSize() {
        return _pktSize;
    }
    
    public SizeUnit getSizeType() {
        return _sizeType;
    }
    
    public void addContent( final String field, final Object value ) {
        _contents.put( field, value );
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getContent( final String field ) {
        return (T) _contents.get( field );
    }
    
    public boolean isDynamic() {
        return _pktSize == -1 && _sizeType == null;
    }
    
    @Override
    public Packet clone()
    {
        Packet p = new Packet( _pktSize, _sizeType );
        if (!_contents.isEmpty()) {
            for (Entry<String,Object> entry : _contents.entrySet()) {
                // FIXME we have to clone the object.
                p.addContent( entry.getKey(), entry.getValue() );
            }
        }
        
        return p;
    }
}