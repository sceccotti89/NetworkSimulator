/**
 * @author Stefano Ceccotti
*/

package simulator.core;

import java.io.IOException;

import simulator.utils.Time;

/**
 * Class representing a generic model.
 * 
 * @param <E>    type of the evaluated parameters,
 *               returned by the {@linkplain #eval(Object...) eval} method.
 * @param <P>    type of input parameters of the {@linkplain #eval(Object...) eval} method.
*/
public abstract class Model<E,P>
{
    protected Device<?,?> _device;
    
    /**
     * Loads the model.
    */
    public abstract void loadModel() throws IOException;
    
    /**
     * Sets the associated device.
     * 
     * @param device    the associated device.
    */
    public void setDevice( Device<?,?> device ) {
        _device = device;
    }
    
    /**
     * Evaluates the input parameters.
     * 
     * @param now       the current time
     * @param params    list of input parameters
     * 
     * @return object representing the evaluation of the input parameter.
    */
    @SuppressWarnings("unchecked")
    public abstract E eval( Time now, P... params );
    
    /***/
    public abstract void close();
}
