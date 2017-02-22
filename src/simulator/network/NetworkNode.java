/**
 * @author Stefano Ceccotti
*/

package simulator.network;

public class NetworkNode
{
    private final long   _id;
    private final String _name;
    
    private final long _delay;
    
    private final int _xPos;
    private final int _yPos;
    
    public static final String ID = "id", NAME = "name", DELAY = "delay";
    public static final String X_POS = "xPos", Y_POS = "yPos";
    
    public NetworkNode( final long id, final String name, final long delay )
    {
        this( id, name, delay, 0, 0 );
    }
    
    public NetworkNode( final long id, final String name, final long delay,
                        final int xPos, final int yPos )
    {
        _id = id;
        _name = name;
        
        _delay = delay;
        
        _xPos = xPos;
        _yPos = yPos;
    }
    
    public long getId() {
        return _id;
    }
    
    public String getName() {
        return _name;
    }
    
    public long getTcalc() {
        return _delay;
    }
    
    public int getXPos() {
        return _xPos;
    }
    
    public int getYPos() {
        return _yPos;
    }
    
    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder( 512 );
        buffer.append( "Id: " + _id + ", Name: \"" + _name +
                       "\", Delay: " + _delay + " at (" + _xPos + ", " + _yPos + ")\n" );
        return buffer.toString();
    }
}