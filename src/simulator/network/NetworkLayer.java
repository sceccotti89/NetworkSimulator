/**
 * @author Stefano Ceccotti
*/

package simulator.network;

/** The ISO/OSI stack layers. */
public enum NetworkLayer {
    APPLICATION( 0 ),
    PRESENTATION( 1 ),
    SESSION( 2 ),
    TRANSPORT( 3 ),
    NETWORK( 4 ),
    DATA_LINK( 5 ),
    PHYSICAL( 6 );
    
    private int index;
    
    NetworkLayer( final int index ) {
        this.index = index;
    }
    
    public int getIndex() {
        return index;
    }
    
    /** Number of stack layers. */
    public static final int STACK_LENGTH = 7;
}