/**
 * @author Stefano Ceccotti
*/

package simulator.network;

/** The OSI stack model. */
public enum NetworkLayer
{
    APPLICATION( 7 ),
    PRESENTATION( 6 ),
    SESSION( 5 ),
    TRANSPORT( 4 ),
    NETWORK( 3 ),
    DATA_LINK( 2 ),
    PHYSICAL( 1 );
    
    private int index;
    
    NetworkLayer( int index ) {
        this.index = index;
    }
    
    public int getIndex() {
        return index;
    }
    
    /** Number of stack layers. */
    public static final int STACK_LENGTH = 7;
}
