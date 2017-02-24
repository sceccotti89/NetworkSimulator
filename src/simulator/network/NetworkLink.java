/**
 * @author Stefano Ceccotti
*/

package simulator.network;

import java.util.concurrent.TimeUnit;

import simulator.core.Time;
import simulator.utils.SimulatorUtils;

/**
 * Link connecting two remote nodes.
 * By default it's unidirectional (whose direction is defined by the from and dest ids).
 * It can be configured to be bidirectional.
*/
public class NetworkLink
{
    private final long _sourceId;
    private final long _destId;
    
    private final double _bandwith;
    private final long _delay;
    
    private int _linkType;
    
    public static final int UNIDIRECTIONAL = 0, BIDIRECTIONAL = 1;
    
    public static final String FROM_ID = "fromId", DEST_ID = "destId";
    public static final String BANDWITH = "bandwith", DELAY = "delay";
    public static final String LINK_TYPE = "linkType";
    
    public NetworkLink( final long sourceId, final long destId,
                        final double bandwith, final long delay )
    {
        this( sourceId, destId, bandwith, delay, UNIDIRECTIONAL );
    }
    
    public NetworkLink( final long sourceId, final long destId,
                        final double bandwith, final long delay,
                        final int linkType )
    {
        _sourceId = sourceId;
        _destId = destId;
        
        _bandwith = bandwith;
        _delay = new Time( delay, TimeUnit.MILLISECONDS ).getTimeMicroseconds();
        
        _linkType = linkType;
    }
    
    public long getSourceId() {
        return _sourceId;
    }
    
    public long getDestId() {
        return _destId;
    }
    
    public double getBandwith() {
        return _bandwith;
    }
    
    public long getDelay() {
        return _delay;
    }
    
    public int getLinkType() {
        return _linkType;
    }
    
    public long getTtrasm( final long size )
    {
        // Ttrasm = size/bandwith
        double tCalc = (size / SimulatorUtils.getSizeInBit( _bandwith, SimulatorUtils.Size.MB ));
        //System.out.println( "Tcalc: " + tCalc + "ns" );
    	return SimulatorUtils.getTimeInMicroseconds( tCalc, TimeUnit.SECONDS );
    }
    
    public long getTprop() {
        return (long) _delay;
    }

    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder( 512 );
        buffer.append( "SourceId: " + _sourceId + ", DestId: " + _destId +
                       ", Bandwith: " + _bandwith + " Mb/s, Delay: " + _delay + "ns\n" );
        return buffer.toString();
    }
}