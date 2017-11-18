/**
 * @author Stefano Ceccotti
*/

package simulator.exception;

public class SimulatorException extends RuntimeException
{
	/** Generated serial ID. */
	private static final long serialVersionUID = -2698911322390606215L;

	public SimulatorException() {
        super();
    }
    
    public SimulatorException( String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace ) {
        super( message, cause, enableSuppression, writableStackTrace );
    }
    
    public SimulatorException( String message, Throwable cause ) {
        super( message, cause );
    }
    
    public SimulatorException( String message ) {
        super( message );
    }
    
    public SimulatorException( Throwable cause ) {
        super( cause );
    }
}
