
package simulator.test.energy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import simulator.core.Model;
import simulator.utils.Time;

public class ClientModel extends Model<Object,Object>
{
    private Set<Long> _queries;
    
    private static final String FILE = "Models/Monolithic/PESOS/MaxScore/time_energy.txt";
    
    
    public ClientModel() {
        // Empty body.
    }
    
    @Override
    public void loadModel() throws IOException {
        loadPostingsPredictors();
    }
    
    private void loadPostingsPredictors() throws IOException
    {
        _queries = new HashSet<>( 1 << 14 );
        
        FileReader fReader = new FileReader( FILE );
        BufferedReader predictorReader = new BufferedReader( fReader );
        
        String line = null;
        while ((line = predictorReader.readLine()) != null) {
            String[] values = line.split( "\\s+" );
            long queryID = Long.parseLong( values[0] );
            _queries.add( queryID );
        }
        
        predictorReader.close();
        fReader.close();
    }
    
    public boolean isQueryAvailable( final Long queryId ) {
        return _queries.contains( queryId );
    }
    
    @Override
    public Object eval( final Time now, final Object... params ) {
        return null;
    }
}