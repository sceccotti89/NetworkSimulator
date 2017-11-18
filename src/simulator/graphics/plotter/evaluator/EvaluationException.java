/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.plotter.evaluator;

public class EvaluationException extends RuntimeException
{
    /** Generated Serial ID. */
    private static final long serialVersionUID = 1573022206696964308L;
    
    public EvaluationException() {
        super();
    }
    
    public EvaluationException( String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace ) {
        super( message, cause, enableSuppression, writableStackTrace );
    }
    
    public EvaluationException( String message, Throwable cause ) {
        super( message, cause );
    }
    
    public EvaluationException( String message ) {
        super( message );
    }
    
    public EvaluationException( Throwable cause ) {
        super( cause );
    }
}
