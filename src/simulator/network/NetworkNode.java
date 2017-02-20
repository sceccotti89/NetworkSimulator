
package simulator.network;

public class NetworkNode
{
    private final long   _id;
    private final String _name;
    
    private final int _xPos;
    private final int _yPos;
    
    public static final String ID = "id", NAME = "name",
                               X_POS = "x_pos", Y_POS = "y_pos";
    
    public NetworkNode( final long id, final String name )
    {
        this( id, name, 0, 0 );
    }
    
    public NetworkNode( final long id, final String name, final int xPos, final int yPos )
    {
        _id = id;
        _name = name;
        
        _xPos = xPos;
        _yPos = yPos;
    }
    
    public long getId() {
        return _id;
    }
    
    public String getName() {
        return _name;
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
        // TODO implement..
        return "";
    }
}