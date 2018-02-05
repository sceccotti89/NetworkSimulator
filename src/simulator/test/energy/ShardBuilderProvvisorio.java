
package simulator.test.energy;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import simulator.utils.resources.ResourceLoader;

public class ShardBuilderProvvisorio
{
    public static void main( String argv[] ) throws Exception
    {
        final String folder = "Models/DistributedModels/";
        
        for (int i = 1; i <= 5; i++) {
            // Time.
            String time = folder + "cw09a" + i + ".ef.time";
            InputStream loader = ResourceLoader.getResourceAsStream( time );
            BufferedReader reader = new BufferedReader( new InputStreamReader( loader ) );
            
            String file = folder + "Node_" + i + "/time_energy.txt";
            PrintWriter writer = new PrintWriter( file, "UTF-8" );
            
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split( " " );
                String val = Long.parseLong( values[0] ) + " ";
                for (int j = 1; j <= 15; j++) {
                    // TODO manca l'energia ad ogni frequenza
                    val += Double.parseDouble( values[16-j] );
                    if (j < 15) {
                        val += " ";
                    }
                }
                writer.println( val );
            }
            
            writer.close();
            reader.close();
            
            // TODO se qui ho finito posso commentare tutto
            // TODO cosi' se lo rimando non mi modifica i file.
            
            // Postings.
            String pp = folder + "cw09a" + i + ".ef.pp";
            loader = ResourceLoader.getResourceAsStream( pp );
            reader = new BufferedReader( new InputStreamReader( loader ) );
            
            file = folder + "Node_" + i + "/predictions.txt";
            PrintWriter predictions = new PrintWriter( file, "UTF-8" );
            
            file = folder + "Node_" + i + "/regressors.txt";
            PrintWriter regressors = new PrintWriter( file, "UTF-8" );
            
            file = folder + "Node_" + i + "/regressors_normse.txt";
            PrintWriter regressorsNOrmse = new PrintWriter( file, "UTF-8" );
            
            line = null;
            Long[] RMSE = new Long[6];
            while ((line = reader.readLine()) != null) {
                String[] values = line.split( "\t" );
                int terms = Integer.parseInt( values[1] );
                predictions.println( values[0] + "\t" + terms + "\t" + values[2] );
                RMSE[terms-1] = Long.parseLong( values[3] );
            }
            
            for (int j = 0; j < RMSE.length; j++) {
                regressors.println( "class." + (j+1) + ".rmse=" + RMSE[j] );
                regressorsNOrmse.println( "class." + (j+1) + ".rmse=0" );
            }
            
            predictions.close();
            regressors.close();
            regressorsNOrmse.close();
            reader.close();
        }
    }
}