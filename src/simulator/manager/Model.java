/**
 * @author Stefano Ceccotti
*/

package simulator.manager;

import java.io.IOException;
import java.util.List;

import simulator.core.Time;

/**
 * Interface representing a generic model.
 * 
 * @param <E>    type of the evaluated parameters,
 *               returned by the {@linkplain #eval(Object...) eval} method.
 * @param <P>    type of input parameters of the {@linkplain #eval(Object...) eval} method.
*/
public interface Model<E,P>
{
    /**
     * Load the model.
     * 
     * @param frequencies    list of available frequencies
    */
    public void loadModel( final List<Long> frequencies ) throws IOException;
    
    /**
     * Evaluate the input parameters.
     * 
     * @param now       the current time
     * @param params    list of input parameters
     * 
     * @return object representing the evaluation of the input parameter.
    */
    @SuppressWarnings("unchecked")
    public E eval( final Time now, final P... params );
}