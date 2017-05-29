/**
 * @author Stefano Ceccotti
*/

package simulator.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

public class Utils
{
    /** Verbose print. */
    public static boolean VERBOSE = true;
    
    /** One million. */
    public static final double MILLION = 1000000d;
    /** The biggest long number is used to represent the "infinite" value. */
    public static final long INFINITE = Long.MAX_VALUE;
    /** The simulator log writer. */
    public static final Logger LOGGER = Logger.getLogger( "Simulator" );
    
    /** Folder used to store the results. */
    public static final String RESULTS_DIR = "Results/";
    /** Folder used to store the images. */
    public static final String IMAGES_DIR = "Results/Images/";
    
    
    
    public static long getTimeInMicroseconds( final double time, final TimeUnit tUnit )
    {
        if (time != Double.POSITIVE_INFINITY || time != Double.MAX_VALUE) {
            switch ( tUnit ) {
                case DAYS:         return (long) (time * 24 * 60 * 60 * 1000L * 1000L);
                case HOURS:        return (long) (time * 60 * 60 * 1000L * 1000L);
                case MINUTES:      return (long) (time * 60 * 1000L * 1000L);
                case SECONDS:      return (long) (time * 1000L * 1000L);
                case MILLISECONDS: return (long) (time * 1000L);
                case MICROSECONDS: return (long) (time);
                case NANOSECONDS:  return (long) (time / 1000L);
                default: break;
            }
        }
        
        return (long) time;
    }
    
    public static long getNormalizedTime( final TimeUnit tUnit )
    {
        switch ( tUnit ) {
            case DAYS:         return 24 * 60 * 60 * 1000L * 1000L;
            case HOURS:        return 60 * 60 * 1000L * 1000L;
            case MINUTES:      return 60 * 1000L * 1000L;
            case SECONDS:      return 1000L * 1000L;
            case MILLISECONDS: return 1000L;
            case MICROSECONDS: return 1;
            default: break;
        }
        
        return 1;
    }
    
    /** 
     * Serializes an object.
     * 
     * @param obj    the object to serialize. It must implements the
     *                {@link java.io.Serializable} interface
     * 
     * @return the byte serialization of the object, if no error happens, null otherwise
    */
    public static <T extends Serializable> byte[] serializeObject( final T obj )
    {
        if(obj instanceof String)
            return ((String) obj).getBytes( StandardCharsets.UTF_8 );
        
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream( out );
            os.writeObject( obj );
            os.close();
            
            return out.toByteArray();
        }
        catch( IOException e ){
            return null;
        }
    }
    
    /** 
     * Deserializes an object from the given byte data.
     * 
     * @param data        bytes of the serialized object
     * 
     * @return the deserialization of the object,
     *            casted to the type specified in {@link T}
    */
    public static <T extends Serializable> T deserializeObject( final byte data[] )
    {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream( data );
            ObjectInputStream is = new ObjectInputStream( in );
            
            @SuppressWarnings("unchecked")
            T obj = (T) is.readObject();
            is.close();
            
            return obj;
        }
        catch( ClassNotFoundException | IOException e ){
            return null;
        }
    }
    
    public static boolean existsFile( final String filename )
    {
        File file = new File( filename );
        return file.exists();
    }
    
    public static void checkFile( final String filename ) throws IOException
    {
        File file = new File( filename );
        if (file.exists()) {
            file.delete();
            file.createNewFile();
        } else {
            checkDirectory( file.getParent() );
        }
    }
    
    public static void checkDirectory( final String filename )
    {
        File file = new File( filename );
        if (!file.exists()) {
            file.mkdirs();
        }
    }
}