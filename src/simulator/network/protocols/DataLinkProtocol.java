/**
 * @author Stefano Ceccotti
*/

package simulator.network.protocols;

import simulator.network.NetworkLayer;

public abstract class DataLinkProtocol extends Protocol
{
    protected int etherType;
    
    public DataLinkProtocol() {
        super( NetworkLayer.DATA_LINK );
    }
    
    /**
     * Sets the ethernet type identifier.
     * 
     * @param type    the ethernet type.
    */
    public void setEtherType( final int type ) {
        etherType = type;
    }
}