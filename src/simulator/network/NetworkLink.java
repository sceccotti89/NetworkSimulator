
package simulator.network;

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
    private final double _delay;
    
    private int _type = UNIDIRECTIONAL;
    
    public static final int UNIDIRECTIONAL = 0, BIDIRECTIONAL = 1;
    
    public static final String FROM_ID = "fromId", DEST_ID = "destId",
                               BANDWITH = "bandwith", DELAY = "delay";
    
    public NetworkLink( final long sourceId, final long destId,
                        final double bandwith, final double delay )
    {
        _sourceId = sourceId;
        _destId = destId;
        
        _bandwith = bandwith;
        _delay = delay;
    }
    
    public NetworkLink( final long sourceId, final long destId,
                        final double bandwith, final double delay, final int type )
    {
        _sourceId = sourceId;
        _destId = destId;
        
        _bandwith = bandwith;
        _delay = delay;
        
        _type = type;
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
    
    public double getDelay() {
        return _delay;
    }
    
    public int getType() {
        return _type;
    }
    
    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder( 512 );
        buffer.append( "SourceId: " + _sourceId + ", DestId: " + _destId +
                       ", Bandwith: " + _bandwith + " Mb/s, Delay: " + _delay + "\n" );
        return buffer.toString();
    }
}