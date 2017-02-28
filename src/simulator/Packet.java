/**
 * @author Stefano Ceccotti
*/

package simulator;

import java.util.HashMap;
import java.util.Map;

import simulator.utils.SimulatorUtils.Size;

public class Packet
{
	private long _pktSize;
	private Size _sizeType = Size.B;
	private Map<String,Object> _contents;
	
	/**
	 * Type of packet used to tell the event generator that it's defined by the user.</br>
	 * The definition of a dynamic packet is the following:
	 * <p>
	 * {@code new Packet( -1, null )}
	*/
	public static final Packet DYNAMIC = new Packet( -1, null );
	
	public Packet( final long pktSize, final Size sizeType )
	{
		_pktSize = pktSize;
		_sizeType = sizeType;
		_contents = new HashMap<>();
	}
	
	public long getSize() {
		return _pktSize;
	}
	
	public Size getSizeType() {
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
	public Packet clone() {
	    // TODO se devo copiare una HashMap mi conviene copiare anche il contenuto
	    return new Packet( _pktSize, _sizeType );
	}
}