/**
 * @author Stefano Ceccotti
*/

package simulator.network;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import simulator.network.protocols.Protocol;
import simulator.network.protocols.RoutingProtocol;
import simulator.topology.NetworkTopology;
import simulator.utils.Utils;

public class NetworkSettings
{
    /**
     * Types of IP address:
     * <p><ul>
     * <li>IPv4: IP version 4</br>
     * <li>IPv6: IP version 6</br>
     * </ul>
    */
    public static enum IPversion { IPv4, IPv6 };
    
    private Map<Integer,List<Protocol>> _protocols;
    private List<RoutingProtocol> _routingProtocols;
    
    private String loopback = "127.0.0.1";
    private String IPv4public = "";
    private String IPv4private = "";
    private String IPv6 = "";
    private String IPv6LinkLocal = "";
    //private String careOfAddress; // TODO Utilizzato per i nodi mobili.
    private boolean useDHCP = true;
    private String subnetMask;
    private String MACaddress;
    
    
    
    public NetworkSettings( NetworkTopology net )
    {
        _protocols = new HashMap<>();
        _routingProtocols = new ArrayList<>();
        
        subnetMask = "255.255.255.0";
        MACaddress = MACaddressFactory.getMACaddress();
        
        IPv6LinkLocal = IPv6StatelessConfiguration();
    }
    
    public NetworkSettings setSubnetMask( String subnetMask ) {
        this.subnetMask = subnetMask;
        return this;
    }
    
    /**
     * Sets an IP address specifying the version (4 or 6) and the type (public or private).
     * 
     * @param address     the IP address
     * @param version     the IP version (4 or 6, see {@linkplain IPversion})
     * @param publicIP    {@code true} if the IP is public, {@code false} otherwise
    */
    public NetworkSettings setIPaddress( String address, IPversion version, boolean publicIP )
    {
        if (version == IPversion.IPv4) {
            if (publicIP) {
                IPv4public = address;
            } else {
                IPv4private = address;
            }
        } else {
            if (publicIP) {
                IPv6 = address;
            } else {
                IPv6LinkLocal = address;
            }
        }
        return this;
    }
    
    public String getIPv4address() {
        return IPv4public;
    }
    
    public String getIPv6address() {
        return IPv6;
    }
    
    public NetworkSettings setMACaddress( String address ) {
        MACaddress = address;
        return this;
    }
    
    public String getMACaddress() {
        return MACaddress;
    }
    
    /**
     * Decides to use or not the DHCP protocol to get the IP address.</br>
     * If the {@code useDHCP} flag is set as {@code true} a DHCP address can be specified,</br>
     * otherwise a default one will be used (typically the gateway address).
     * 
     * @param useDHCP        {@code true} to makes use of DHCP, {@code false} otherwise
     * @param DHCPaddress    the DHCP IP address
     * @param version        the DHCP version (4 or 6, see {@linkplain IPversion})
    */
    public NetworkSettings setDHCP( boolean useDHCP, String DHCPaddress, IPversion version ) {
        this.useDHCP = useDHCP;
        if (this.useDHCP) {
            if (DHCPaddress != null) {
                // TODO this.DHCPaddress = DHCPaddress;
            }
        }
        return this;
    }
    
    private String IPv6StatelessConfiguration()
    {
        // Insert the 16 bits 0xFFFE at the 24th bit of the IEEE 802 MAC address.
        String ip = MACaddress.substring( 0, 9 ) + "FF-FE-" + MACaddress.substring( 9 );
        // Invert the bit in position 6 of the first octet.
        String binary = new BigInteger( ip.substring( 0, 2 ), 16 ).toString( 2 );
        // Pad the binary value pre-appending some zeroes.
        for (int i = binary.length(); i < 8; i++) {
            binary = "0" + binary;
        }
        if (binary.charAt( 6 ) == '0') {
            binary = binary.substring( 0, 6 ) + "1" + binary.substring( 7 );
        } else {
            binary = binary.substring( 0, 6 ) + "0" + binary.substring( 7 );
        }
        ip = new BigInteger( binary, 2 ).toString( 16 ) + ip.substring( 8 );
        
        // Create groups of 4 hexadecimal digits of the height 16-bit pieces of the address.
        int count = 0;
        for (int index = ip.indexOf( '-' ); index >= 0 && index < ip.length(); index = ip.indexOf( '-', index + 1 )) {
            if (count % 2 == 0) {
                ip = ip.substring( 0, index ) + ip.substring( index + 1 );
                count = 1;
            } else {
                count++;
            }
        }
        
        return "fe80::" + ip.replace( "-", ":" ).toLowerCase();
    }
    
    public void addNetworkProtocol( Protocol protocol )
    {
        int layer = protocol.getLayer().getIndex();
        List<Protocol> protocols = _protocols.get( layer );
        if (protocols == null) {
            protocols = new ArrayList<>();
            _protocols.put( layer, protocols );
        }
        protocols.add( protocol );
    }
    
    public List<Protocol> getNetworkProtocols( NetworkLayer layer ) {
        return getNetworkProtocols( layer.getIndex() );
    }
    
    public List<Protocol> getNetworkProtocols( int layer ) {
        return _protocols.get( layer );
    }
    
    /**
     * Adds a protocol used to transmit messages to a destination node
     * using a certain routing strategy.
     * 
     * @param protocol    the routing protocol
    */
    public void addRoutingProtocol( RoutingProtocol protocol ) {
        _routingProtocols.add( protocol );
    }
    
    public List<RoutingProtocol> getRoutingProtocols() {
        return _routingProtocols;
    }
    
    @Override
    public String toString()
    {
        if (!Utils.VERBOSE) {
            return "";
        }
        
        return "IPv4 public address .....: " + IPv4public + "\n" +
               "IPv4 private address ....: " + IPv4private + "\n" +
               "IPv6 address ............: " + IPv6 + "\n" +
               "IPv6 Link Local address .: " + IPv6LinkLocal + "\n" +
               "Loopback.................: " + loopback + "\n" +
               "MAC address .............: " + MACaddress + "\n" +
               "Subnet mask .............: " + subnetMask;
    }
}
