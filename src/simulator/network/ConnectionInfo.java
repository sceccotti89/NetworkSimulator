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
    
    public void setSourceIPaddress( String address ) {
        sourceIPaddress = address;
    }
    
    public String getSourceIPaddress() {
        return sourceIPaddress;
    }
    
    public void setDestinationIPaddress( String address ) {
        destIPaddress = address;
    }
    
    public String getDestinationIPaddress() {
        return destIPaddress;
    }
    
    public void setDestinationPort( int port ) {
        destPort = port;
    }
    
    public int getDestinationPort() {
        return destPort;
    }
    
    public void setSourcePort( int port ) {
        sourcePort = port;
    }
    
    public int getSourcePort() {
        return sourcePort;
    }
    
    public void setSourceMACaddress( String address ) {
        sourceMACaddress = address;
    }
    
    public String getSourceMACaddress() {
        return sourceMACaddress;
    }
    
    public void setDestinationMACaddress( String address ) {
        destMACaddress = address;
    }
    
    public String getDestinationMACaddress() {
        return destMACaddress;
    }
    
    public void setLink( NetworkLink link ) {
        this.link = link;
    }
    
    public NetworkLink getLink() {
        return link;
    }
}
