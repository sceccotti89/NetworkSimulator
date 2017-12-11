/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.plotter.evaluator;

import java.util.HashMap;
import java.util.Map;

import simulator.graphics.plotter.evaluator.Tokenizer.Token;

public class FunctionInterpreter
{
    private final Expr e;
    private Map<String,Double> values;
    
    public FunctionInterpreter( Expr e ) {
        this.e = e;
        values = new HashMap<>();
    }
    
    public void putVariable( String variable, double value ) {
        values.put( variable, value );
    }
    
    public double eval() {
        return evalExpr( e );
    }
    
    private double evalExpr( Expr e )
    {
        switch (e.getOp().getType()) {
            case( Token.T_NUMBER):      return e.getExpr1().getOp().getValue();
            case( Token.T_IDENTIFIER ): return getValue( e.getExpr1().getOp() );
            case( Token.T_OPEN_PARENTHESIS ): return evalExpr( e.getExpr1() );
            case( Token.T_PLUS ) :     if (e.getExpr2() == null) return evalExpr( e.getExpr1() );
                                       else return evalExpr( e.getExpr1() ) + evalExpr( e.getExpr2() );
            case( Token.T_DIVIDE ) :   return evalExpr( e.getExpr1() ) / evalExpr( e.getExpr2() );
            case( Token.T_MULTIPLY ) : return evalExpr( e.getExpr1() ) * evalExpr( e.getExpr2() );
            case( Token.T_MINUS ) :    if (e.getExpr2() == null) return -evalExpr( e.getExpr1() );
                                       else return evalExpr( e.getExpr1() ) - evalExpr( e.getExpr2() );
            case( Token.T_POW ) :      return Math.pow(   evalExpr( e.getExpr1() ), evalExpr( e.getExpr2() ) );
            case( Token.T_LN ) :       return Math.log(   evalExpr( e.getExpr1() ) );
            case( Token.T_LOG ) :      return Math.log10( evalExpr( e.getExpr2() ) )/Math.log10( evalExpr( e.getExpr1() ) );
            case( Token.T_SQRT ) :     return Math.sqrt(  evalExpr( e.getExpr1() ) );
            case( Token.T_COS ) :      return Math.cos(   evalExpr( e.getExpr1() ) );
            case( Token.T_ARC_COS ) :  return Math.acos(  evalExpr( e.getExpr1() ) );
            case( Token.T_SIN ) :      return Math.sin(   evalExpr( e.getExpr1() ) );
            case( Token.T_ARC_SIN ) :  return Math.asin(  evalExpr( e.getExpr1() ) );
            case( Token.T_TAN ) :      return Math.tan(   evalExpr( e.getExpr1() ) );
            case( Token.T_ARC_TAN ) :  return Math.atan(  evalExpr( e.getExpr1() ) );
        }
        
        return 0;
    }
    
    private double getValue( Token token )
    {
        Double result = values.get( token.getStringValue() );
        if (result == null) {
            throw new EvaluationException( "No mappings for '" + token.getStringValue() + "'." );
        }
        return result;
    }
}
