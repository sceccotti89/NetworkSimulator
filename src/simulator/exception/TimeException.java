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
    
    public TimeException( String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace ) {
        super( message, cause, enableSuppression, writableStackTrace );
    }
    
    public TimeException( String message, Throwable cause ) {
        super( message, cause );
    }
    
    public TimeException( String message ) {
        super( message );
    }
    
    public TimeException( Throwable cause ) {
        super( cause );
    }
}
