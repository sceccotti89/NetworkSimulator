/**
 * @author Stefano Ceccotti
*/

package simulator.network.protocols;

import simulator.network.NetworkLayer;

public abstract class NetworkProtocol extends Protocol
{
    public NetworkProtocol( final Protocol... baseProtocols ) {
        super( NetworkLayer.NETWORK, baseProtocols );
    }
    
    /**
     * Returns the EtherType identifier.
    */
    public abstract int getEtherType();
}