/**
 * @author Stefano Ceccotti
*/

package simulator;

import simulator.utils.SimulatorUtils.Size;

public class Packet
{
	private long _pktSize;
	private Size _sizeType = Size.B;
	
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
}