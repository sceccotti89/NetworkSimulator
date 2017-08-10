/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.plotter.parser;

public class EvaluationException extends RuntimeException
{
    /** Generated Serial ID. */
    private static final long serialVersionUID = 1573022206696964308L;
    
    public EvaluationException() {
        super();
    }
    
    public EvaluationException( final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace ) {
        super( message, cause, enableSuppression, writableStackTrace );
    }
    
    public EvaluationException( final String message, final Throwable cause ) {
        super( message, cause );
    }
    
    public EvaluationException( final String message ) {
        super( message );
    }
    
    public EvaluationException( final Throwable cause ) {
        super( cause );
    }
}