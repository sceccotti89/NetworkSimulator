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
    
    public SimulatorException( final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace ) {
        super( message, cause, enableSuppression, writableStackTrace );
    }
    
    public SimulatorException( final String message, final Throwable cause ) {
        super( message, cause );
    }
    
    public SimulatorException( final String message ) {
        super( message );
    }
    
    public SimulatorException( final Throwable cause ) {
        super( cause );
    }
}