/**
 * @author Stefano Ceccotti
*/

package simulator.network;

import simulator.topology.NetworkLink;

public class ConnectionInfo
{
    private int destPort;
    private int sourcePort;
    private String sourceIPaddress;
    private String destIPaddress;
    private String sourceMACaddress;
    private String destMACaddress;
    private NetworkLink link;
    
    public ConnectionInfo() {
        
    }
    
    public void setSourceIPaddress( final String address ) {
        sourceIPaddress = address;
    }
    
    public String getSourceIPaddress() {
        return sourceIPaddress;
    }
    
    public void setDestinationIPaddress( final String address ) {
        destIPaddress = address;
    }
    
    public String getDestinationIPaddress() {
        return destIPaddress;
    }
    
    public void setDestinationPort( final int port ) {
        destPort = port;
    }
    
    public int getDestinationPort() {
        return destPort;
    }
    
    public void setSourcePort( final int port ) {
        sourcePort = port;
    }
    
    public int getSourcePort() {
        return sourcePort;
    }
    
    public void setSourceMACaddress( final String address ) {
        sourceMACaddress = address;
    }
    
    public String getSourceMACaddress() {
        return sourceMACaddress;
    }
    
    public void setDestinationMACaddress( final String address ) {
        destMACaddress = address;
    }
    
    public String getDestinationMACaddress() {
        return destMACaddress;
    }
    
    public void setLink( final NetworkLink link ) {
        this.link = link;
    }
    
    public NetworkLink getLink() {
        return link;
    }
}