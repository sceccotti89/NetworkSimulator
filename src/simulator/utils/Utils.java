/**
 * @author Stefano Ceccotti
*/

package simulator.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
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
    
    
    
    public static long getTimeInMicroseconds( double time, TimeUnit tUnit )
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
    
    public static long getNormalizedTime( TimeUnit tUnit )
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
    public static <T extends Serializable> byte[] serializeObject( T obj )
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
    public static <T extends Serializable> T deserializeObject( byte data[] )
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
    
    public static boolean existsFile( String filename )
    {
        File file = new File( filename );
        return file.exists();
    }
    
    public static void checkFile( String filename )
    {
        File file = new File( filename );
        if (!file.exists()) {
            checkDirectory( file.getParent() );
        }
    }
    
    public static void checkDirectory( String filename )
    {
        File file = new File( filename );
        if (!file.exists()) {
            file.mkdirs();
        }
    }
    
    /**
     * Computes the percentile over the specified input file and time interval.
     * 
     * @param percentile        the selected percentile.
     * @param interval          time to collect values and perform the percentile.
     *                          Time must be expressed in microseconds.
     * @param inputFileName     the input file name.
     *                          The file must be formatted in such a way that each line contains
     *                          only 2 values: the time and its associated value,
     *                          separated by a space.
     * @param outputFileName    where to save the generated output.
     *                          If {@code null} the generated output will be not saved.
     *                        
     * @return the generated list of precentiles.
    */
    public static List<Pair<Double,Double>> getPercentiles( int percentile,
                                                            double interval,
                                                            String inputFileName,
                                                            String outputFileName ) throws IOException
    {
        FileReader fReader = new FileReader( inputFileName );
        BufferedReader reader = new BufferedReader( fReader );
        String line;
        List<Pair<Double,Double>> values = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            String[] val = line.split( "\\s" );
            double time  = Double.parseDouble( val[0] );
            double value = Double.parseDouble( val[1] );
            values.add( new Pair<>( time, value ) );
        }
        reader.close();
        
        return getPercentiles( percentile, interval, values, outputFileName );
    }
    
    /**
     * Computes the percentile over the input list and time interval.
     * 
     * @param percentile        the selected percentile.
     * @param interval          sampling time to collect values and perform the percentile.
     *                          Time must be expressed in microseconds.
     * @param values            list of values on which the percentiles are calculated.
     *                          It must be a list of pairs (Time,Value),
     *                          where Time must be expressed in microseconds.
     * @param outputFileName    where to save the generated output, if different from {@code null}.
     *                        
     * @return the generated list of precentiles.
    */
    public static List<Pair<Double,Double>> getPercentiles( int percentile,
                                                            double interval,
                                                            List<Pair<Double,Double>> values,
                                                            String outputFileName ) throws IOException
    {
        final double _percentile = percentile * 0.01d;
        
        String results = "";
        double nextInterval = interval;
        Queue<Double> set = new PriorityQueue<>();
        List<Pair<Double,Double>> percentiles = new ArrayList<>();
        for (Pair<Double,Double> val : values) {
            double time  = val.getFirst();
            double value = val.getSecond();
            if (time < nextInterval) {
                set.add( value );
            } else {
                // Change of interval.
                double index = _percentile * set.size();
                double realIndex = Math.ceil( index );
                for (int i = 0; i < realIndex-1; i++) {
                    set.poll();
                }
                
                if (realIndex > index) {
                    // Not rounded.
                    results += nextInterval + " " + set.peek() + "\n";
                    percentiles.add( new Pair<>( nextInterval, set.poll() ) );
                } else {
                    // Rounded: mean of the two next values.
                    double first  = set.poll();
                    double second = set.poll();
                    results += nextInterval + " " + ((first+second)/2d) + "\n";
                    percentiles.add( new Pair<>( nextInterval, (first+second)/2d ) );
                }
                
                set.clear();
                set.add( value );
                nextInterval += interval;
            }
        }
        
        if (outputFileName != null) {
            PrintWriter writer = new PrintWriter( outputFileName, "UTF-8" );
            writer.print( results );
            writer.close();
        }
        
        return percentiles;
    }
}
