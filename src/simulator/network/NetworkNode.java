/**
 * @author Stefano Ceccotti
*/

package simulator.network;

import java.util.concurrent.TimeUnit;

import simulator.core.Time;

public class NetworkNode
{
    private final long   _id;
    private final String _name;
    
    private final Time _delay;
    
    private final int _xPos;
    private final int _yPos;
    
    // Note: used internally for the shortest path calculation.
    private int _index;
    
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
        
        _delay = new Time( delay, TimeUnit.MILLISECONDS );
        
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
        return _delay.getTimeMicroseconds();
    }
    
    public int getXPos() {
        return _xPos;
    }
    
    public int getYPos() {
        return _yPos;
    }
    
    public void setIndex( final int index ) {
    	_index = index;
    }
    
    public int getIndex() {
    	return _index;
    }
    
    @Override
    public String toString()
    {
        StringBuilder buffer = new StringBuilder( 128 );
        buffer.append( "Id: " + _id + ", Name: \"" + _name +
                       "\", Delay: " + ((_delay.isDynamic()) ? "Dynamic\n" : _delay + "ns\n") );
        return buffer.toString();
    }
}