/**
 * @author Stefano Ceccotti
*/

package simulator.manager;

import java.io.IOException;

import simulator.Agent;
import simulator.core.Device;

public interface Model<E,P>
{
    /**
     * Load the model.
     * 
     * @param device    associated device. It can be {@code null}.
    */
    public void loadModel( final Device device ) throws IOException;
    
    /**
     * Assign the agent.
    */
    public void setAgent( final Agent agent );
    
    /**
     * Evaluate the input parameters, passed as strings.
     * 
     * @param params    list of input parameters
     * 
     * @return object representing the evaluation of the input parameter
    */
    @SuppressWarnings("unchecked")
    public E eval( final P... params );

    /**
     * Returns the separator used in the model to generate the input parameter
     * for the {@link #eval(String) eval} method.
    */
    public String getSeparator();
}