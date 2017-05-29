/**
 * @author Stefano Ceccotti
*/

package simulator.network;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import simulator.network.protocols.IP.IPv4;
import simulator.core.Agent;
import simulator.network.protocols.NetworkProtocol;
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
    
    private List<NetworkProtocol> _netProtocols;
    private List<NetworkProtocol> _routingProtocols;
    
    private String IPv4public;
    private String IPv4private;
    private String IPv6;
    private String IPv6LinkLocal;
    //private String careOfAddress; // TODO Utilizzato per i nodi mobili.
    private boolean useDHCP = true;
    private String subnetMask;
    private String MACaddress;
    
    
    
    public NetworkSettings( final NetworkTopology net, final Agent agent )
    {
        _netProtocols = new ArrayList<>( NetworkLayer.STACK_LENGTH );
        for (int i = 0; i < NetworkLayer.STACK_LENGTH; i++) {
            _netProtocols.add( null );
        }
        // By default the IP version is the 4th.
        _netProtocols.set( NetworkLayer.NETWORK.getIndex(), new IPv4( agent ) );
        
        _routingProtocols = new ArrayList<>();
        // By default every node executes the RIP protocol.
        //_routingProtocols.add( new RIP( net, agent ) );
        
        subnetMask = "255.255.255.0";
        MACaddress = MACAddressFactory.getMACaddress();
        
        IPv6LinkLocal = IPv6StatelessConfiguration();
    }
    
    public NetworkSettings setSubnetMask( final String subnetMask ) {
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
    public NetworkSettings setIPaddress( final String address, final IPversion version, final boolean publicIP )
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
    
    public NetworkSettings setMACaddress( final String address ) {
        MACaddress = address;
        return this;
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
    public NetworkSettings setDHCP( final boolean useDHCP, final String DHCPaddress, final IPversion version ) {
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
        if (binary.charAt( 6 ) == '0') { binary = binary.substring( 0, 6 ) + "1" + binary.substring( 7 ); }
        else { binary = binary.substring( 0, 6 ) + "0" + binary.substring( 7 ); }
        ip = new BigInteger( binary, 2 ).toString( 16 ) + ip.substring( 8 );
        
        // Create groups of 4 hexadecimal digits of the height 16-bit pieces of the address.
        int count = 0;
        for(int index = ip.indexOf( '-' ); index >= 0 && index < ip.length(); index = ip.indexOf( '-', index + 1 )) {
            if (count % 2 == 0) {
                ip = ip.substring( 0, index ) + ip.substring( index + 1 );
                count = 1;
            } else {
                count++;
            }
        }
        
        return "fe80::" + ip.replace( "-", "::" ).toLowerCase();
    }
    
    public NetworkSettings addRoutingProtocol( final NetworkProtocol protocol ) {
        _routingProtocols.add( protocol );
        return this;
    }
    
    public List<NetworkProtocol> getRoutingProtocols() {
        return _routingProtocols;
    }
    
    public NetworkSettings setNetworkProtocol( final NetworkProtocol protocol,
                                               final NetworkLayer layer ) {
        _netProtocols.set( layer.getIndex(), protocol );
        return this;
    }
    
    public List<NetworkProtocol> getNetworkProtocols() {
        return _netProtocols;
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
               "MAC address .............: " + MACaddress + "\n" +
               "Subnet mask .............: " + subnetMask;
    }
}