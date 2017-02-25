/**
 * @author Stefano Ceccotti
*/

package simulator;

import simulator.utils.SimulatorUtils.Size;

public class Packet
{
	private long _pktSize;
	private Size _sizeType = Size.B;
	
	/** Type of packet used to tell the event generator that it's defined by the user.</br>
	 * The definition of a dynamic packet is the following:
	 * <p>
	 * {@code new Packet( -1, null )}
	*/
	public static final Packet DYNAMIC = new Packet( -1, null );
	
	public Packet( final long pktSize, final Size sizeType )
	{
		_pktSize = pktSize;
		_sizeType = sizeType;
	}
	
	public long getSize() {
		return _pktSize;
	}
	
	public Size getSizeType() {
		return _sizeType;
	}
	
	public boolean isDynamic() {
	    return _pktSize == -1 && _sizeType == null;
	}
}