/**
 * @author Stefano Ceccotti
*/

package simulator.network;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class MACaddressFactory
{
    private static Set<String> MACgenerated = new HashSet<>( 1 << 10 );
    
    /**
     * Generates a unique random MAC address.
    */
    public static String getMACaddress()
    {
        Random rand = new Random();
        byte[] macAddr = new byte[6];
        String address;
        do {
            rand.nextBytes( macAddr );
            
            // Zeroing last 2 bytes to make it unicast and locally adminstrated.
            macAddr[0] = (byte)(macAddr[0] & (byte)254);
    
            StringBuilder sb = new StringBuilder( 18 );
            for(byte b : macAddr){
                if(sb.length() > 0)
                    sb.append( "-" );
                sb.append( String.format( "%02x", b ) );
            }
            address = sb.toString().toUpperCase();
        } while (MACgenerated.contains( address ));
        MACgenerated.add( address );
        
        return address;
    }
}
