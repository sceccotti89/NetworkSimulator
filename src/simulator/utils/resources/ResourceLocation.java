/**
 * @author Stefano Ceccotti
*/

package simulator.utils.resources;

import java.io.InputStream;
import java.net.URL;

/**
 * A location from which resources can be loaded
 */
public interface ResourceLocation
{
    /**
     * Gets a resource as an input stream.
     * 
     * @param ref The reference to the resource to retrieve
     * 
     * @return A stream from which the resource can be read or
     *         {@code null} if the resource can't be found in this location
    */
    public InputStream getResourceAsStream( final String ref );

    /**
     * Gets a resource as a URL.
     * 
     * @param ref The reference to the resource to retrieve
     * 
     * @return A URL from which the resource can be read
    */
    public URL getResource( final String ref );
}
