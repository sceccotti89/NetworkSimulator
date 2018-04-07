
package simulator.core.devices;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.json.JSONObject;

import simulator.core.Task;
import simulator.test.energy.CPUModel.QueryInfo;
import simulator.utils.Time;
import simulator.utils.resources.ResourceLoader;

public class Memory extends Device<QueryInfo,Long>
{
    // By default the size of the memory is 4GB.
    private long size = 1L << 32L;
    private long used = 0;
    
    
    public Memory( String name, List<Long> frequencies ) {
        super( name, frequencies );
    }
    
    @Override
    public void build( String inputFile ) throws IOException
    {
        InputStream fReader = ResourceLoader.getResourceAsStream( inputFile );
        BufferedReader br = new BufferedReader( new InputStreamReader( fReader ) );
        StringBuilder content = new StringBuilder( 64 );
        String nextLine = null;
        while((nextLine = br.readLine()) != null) {
            content.append( nextLine.trim() );
        }
        br.close();
        
        JSONObject settings = new JSONObject( content.toString() );
        setMemorySize( settings.getLong( "SIZE" ) );
    }
    
    public void setMemorySize( long size ) {
        this.size = size;
    }
    
    public boolean getMemory( long size )
    {
        if (this.size - used >= size) {
            used += size;
            return true;
        } else {
            return false;
        }
    }
    
    public void freeMemory( long size ) {
        used -= size;
    }
    
    @Override
    public Time timeToCompute( Task task ) {
        return Time.ZERO;
    }
    
    @Override
    public double getUtilization( Time time ) {
        return ((double) size) / ((double) used);
    }
}