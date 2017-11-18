
package simulator.test.energy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import simulator.core.Model;
import simulator.utils.Time;
import simulator.utils.resources.ResourceLoader;

public class ClientModel extends Model<Object,Object>
{
    private Set<Long> _queries;
    private String[] files;
    
    
    public ClientModel( String... files ) {
        this.files = files;
    }
    
    @Override
    public void loadModel() throws IOException
    {
        _queries = new HashSet<>( 1 << 14 );
        for (String file : files) {
            loadQueries( file );
        }
    }
    
    private void loadQueries( String file ) throws IOException
    {
        InputStream fReader = ResourceLoader.getResourceAsStream( file );
        BufferedReader queryReader = new BufferedReader( new InputStreamReader( fReader ) );
        
        String line = null;
        while ((line = queryReader.readLine()) != null) {
            String[] values = line.split( "\\s+" );
            long queryID = Long.parseLong( values[0] );
            _queries.add( queryID );
        }
        
        queryReader.close();
        fReader.close();
    }
    
    public boolean isQueryAvailable( Long queryId ) {
        return _queries.contains( queryId );
    }
    
    @Override
    public Object eval( Time now, Object... params ) {
        return null;
    }
    
    @Override
    public void close() {
        // TODO Auto-generated method stub
    }
}
