/**
 * @author Stefano Ceccotti
*/

package simulator.exception;

public class TimeException extends RuntimeException
{
    /** Generated serial ID */
    private static final long serialVersionUID = -8471413112932406933L;

    public TimeException() {
        super();
    }
    
    public TimeException( final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace ) {
        super( message, cause, enableSuppression, writableStackTrace );
    }
    
    public TimeException( final String message, final Throwable cause ) {
        super( message, cause );
    }
    
    public TimeException( final String message ) {
        super( message );
    }
    
    public TimeException( final Throwable cause ) {
        super( cause );
    }
}