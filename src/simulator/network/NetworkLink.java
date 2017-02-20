
package simulator.network;

/**
 * Link connecting two remote nodes.
*/
public class NetworkLink
{
    private final int _fromId;
    private final int _destId;
    
    private final double _bandwith;
    private final double _delay;
    
    public NetworkLink( final int fromId, final int destId,
                        final double bandwith, final double delay )
    {
        _fromId = fromId;
        _destId = destId;
        
        _bandwith = bandwith;
        _delay = delay;
    }
    
    public int getFromId() {
        return _fromId;
    }
    
    public int getDestId() {
        return _destId;
    }
    
    public double getBandwith() {
        return _bandwith;
    }
    
    public double getDelay() {
        return _delay;
    }
}