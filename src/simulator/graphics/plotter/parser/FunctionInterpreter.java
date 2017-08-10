/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.plotter.parser;

import java.util.HashMap;
import java.util.Map;

import simulator.graphics.plotter.parser.Tokenizer.Token;

public class FunctionInterpreter
{
    private final Expr e;
    private Map<String,Double> values;
    
    public FunctionInterpreter( final Expr e ) {
        this.e = e;
        values = new HashMap<>();
    }
    
    public void putVariable( final String variable, final double value ) {
        values.put( variable, value );
    }
    
    public double eval() {
        return evalExpr( e );
    }
    
    private double evalExpr( final Expr e )
    {
        System.out.println( "TOKEN: " + e.getToken() );
        switch (e.getToken().getType()) {
            case( Token.T_NUMBER):      return e.getExpr1().getToken().value;
            case( Token.T_IDENTIFIER ): return getValue( e.getExpr1().getToken() );
            case( Token.T_OPEN_PARENTHESIS ): return evalExpr( e.getExpr1() );
            case( Token.T_PLUS ) :     return evalExpr( e.getExpr1() ) + evalExpr( e.getExpr2() );
            case( Token.T_DIVIDE ) :   return evalExpr( e.getExpr1() ) / evalExpr( e.getExpr2() );
            case( Token.T_MULTIPLY ) : return evalExpr( e.getExpr1() ) * evalExpr( e.getExpr2() );
            case( Token.T_MINUS ) :    return evalExpr( e.getExpr1() ) - evalExpr( e.getExpr2() );
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
    
    private double getValue( final Token token )
    {
        Double result = values.get( token.stringValue );
        if (result == null) {
            throw new EvaluationException( "No mappings for '" + token.stringValue + "'." );
        }
        return result;
    }
}