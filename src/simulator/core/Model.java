/**
 * @author Stefano Ceccotti
*/

package simulator.core;

import java.io.IOException;

import simulator.utils.Time;

/**
 * Class representing a generic model.
 * 
 * @param <I>    type of the Input parameters,
 *               returned by the {@linkplain #eval(Object...) eval} method.
 * @param <O>    type of the Output parameter of the {@linkplain #eval(Object...) eval} method.
*/
public abstract class Model<I,O>
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
    public abstract O eval( Time now, I... params );
    
    /***/
    public abstract void close();
}
