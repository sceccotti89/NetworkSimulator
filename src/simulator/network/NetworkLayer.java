/**
 * @author Stefano Ceccotti
*/

package simulator.network;

/** The TCP/IP stack model. */
public enum NetworkLayer
{
    APPLICATION( 5 ),
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
    public static final int STACK_LENGTH = 5;
}
