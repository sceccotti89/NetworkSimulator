/**
 * @author Stefano Ceccotti
*/

package simulator.topology;

import java.util.concurrent.TimeUnit;

import simulator.utils.Utils;
import simulator.utils.Size;
import simulator.utils.SizeUnit;
import simulator.utils.Time;

/**
 * Link connecting two remote nodes.</br>
 * By default it's unidirectional (whose direction is defined by the from and dest ids),
 * but it can be configured to be bidirectional.
*/
public class NetworkLink
{
    private final long _sourceId;
    private final long _destId;
    
    private final Size<Double> _bandwith;
    private final long _delay;
    
    private double errorLink = 0;
    
    private boolean _active = true;
    private String _linkType;
    
    /** The Maximum Transmission Unit of the link. */
    private long MTU = 1500;
    
    public static final String UNIDIRECTIONAL = "simplex", BIDIRECTIONAL = "duplex";
    
    public static final String FROM_ID = "fromId", DEST_ID = "destId";
    public static final String BANDWITH = "bandwith", DELAY = "delay";
    public static final String LINK_TYPE = "linkType";
    
    
    
    public NetworkLink( long sourceId, long destId,
                        double bandwith, long delay )
    {
        this( sourceId, destId, bandwith, delay, UNIDIRECTIONAL );
    }
    
    public NetworkLink( long sourceId,   long destId,
                        double bandwith, long delay,
                        String linkType )
    {
        _sourceId = sourceId;
        _destId = destId;
        
        _bandwith = new Size<Double>( bandwith, SizeUnit.MEGABIT );
        _delay = new Time( delay, TimeUnit.MILLISECONDS ).getTimeMicros();
        
        _linkType = linkType;
    }
    
    public long getSourceId() {
        return _sourceId;
    }
    
    public long getDestId() {
        return _destId;
    }
    
    public double getBandwith() {
        return _bandwith.getSize();
    }
    
    public String linkType() {
        return _linkType;
    }
    
    public long getTtrasm( long size )
    {
        // Ttrasm = size/bandwith
        double Ttrasm = size / _bandwith.getBits();
        return Utils.getTimeInMicroseconds( Ttrasm, TimeUnit.SECONDS );
    }
    
    public long getTprop() {
        return _delay;
    }
    
    public void setError( double errorValue ) {
        errorLink = errorValue;
    }
    
    public double getError() {
        return errorLink;
    }
    
    public boolean checkErrorLink() {
        return Math.random() < errorLink;
    }
    
    public long getMTU() {
        return MTU;
    }
    
    public void setMTU( long value ) {
        MTU = value;
    }
    
    /**
     * Creates a new link identical to this one with the inverted
     * boundary nodes.
    */
    public NetworkLink reverse() {
        return new NetworkLink( _destId, _sourceId, _bandwith.getSize(), _delay/1000L, _linkType );
    }
    
    public void setActive( boolean flag ) {
        _active = flag;
    }
    
    public boolean isActive() {
        return _active;
    }

    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder( 128 );
        buffer.append( "Source: " + _sourceId + ", Dest: " + _destId +
                       ", Bandwith: " + _bandwith.getSize() + " Mb/s, " +
                       "Delay: " + _delay + "ns, Type: " + _linkType + "\n" );
        return buffer.toString();
    }
}
