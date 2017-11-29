/**
 * @author Stefano Ceccotti
*/

package simulator.network.protocols;

public class ProtocolReference
{
    private Integer nextProtocol;
    private Header response;
    
    public ProtocolReference( final Integer nextProtocol ) {
        this( nextProtocol, null );
    }
    
    public ProtocolReference( final Header response ) {
        this( null, response );
    }
    
    public ProtocolReference( final Integer nextProtocol, final Header response )
    {
        this.nextProtocol = nextProtocol;
        this.response = response;
    }
    
    public Integer getNextProtocol() {
        return nextProtocol;
    }
    
    public Header getResponse() {
        return response;
    }
}