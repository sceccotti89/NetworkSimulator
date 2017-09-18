/**
 * @author Stefano Ceccotti
*/

package simulator.utils.resources;

import java.io.InputStream;
import java.net.URL;

/**
 * A resource location that searches the classpath.
*/
public class ClasspathLocation implements ResourceLocation
{
    @Override
    public URL getResource( final String ref )
    {
        String cpRef = ref.replace( '\\', '/' );
        return ResourceLoader.class.getClassLoader().getResource( cpRef );
    }

    @Override
    public InputStream getResourceAsStream( final String ref )
    {
        String cpRef = ref.replace( '\\', '/' );
        return ResourceLoader.class.getClassLoader().getResourceAsStream( cpRef );    
    }
}
