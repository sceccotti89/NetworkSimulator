/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.plotter.evaluator;

public class Evaluator
{
    private FunctionInterpreter interpreter;
    
    public Evaluator( final String expression ) {
        Expr e = FunctionParser.parse( expression );
        interpreter = new FunctionInterpreter( e );
    }
    
    public void putVariable( final String variable, final double value ) {
        interpreter.putVariable( variable, value );
    }
    
    public double eval() {
        return interpreter.eval();
    }
}