
package simulator.network;

/**
 * Link connecting two remote nodes.
 * By default it's unidirectional (whose direction is defined by the from and dest ids).
 * It can be configured to be bidirectional.
*/
public class NetworkLink
{
    private final long _fromId;
    private final long _destId;
    
    private final double _bandwith;
    private final double _delay;
    
    private int _type = UNIDIRECTIONAL;
    
    public static final int UNIDIRECTIONAL = 0, BIDIRECTIONAL = 1;
    
    public static final String FROM_ID = "from_id", DEST_ID = "dest_id",
                               BANDWITH = "bandwith", DELAY = "delay";
    
    public NetworkLink( final long fromId, final long destId,
                        final double bandwith, final double delay )
    {
        _fromId = fromId;
        _destId = destId;
        
        _bandwith = bandwith;
        _delay = delay;
    }
    
    public NetworkLink( final long fromId, final long destId,
                        final double bandwith, final double delay, final int type )
    {
        _fromId = fromId;
        _destId = destId;
        
        _bandwith = bandwith;
        _delay = delay;
        
        _type = type;
    }
    
    public long getFromId() {
        return _fromId;
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
        // TODO implement..
        return "";
    }
}