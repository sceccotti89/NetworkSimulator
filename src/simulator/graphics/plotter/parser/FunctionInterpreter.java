
package simulator.graphics.plotter.parser;

import java.util.Map;

import simulator.graphics.plotter.parser.Tokenizer.Token;

public class FunctionInterpreter
{
    public static double interpret( final Expr e, final Map<String,Double> values ) {
        return readExpression( e, values );
    }
    
    private static double readExpression( final Expr e, final Map<String,Double> values )
    {
        System.out.println( "TOKEN: " + e.getToken() );
        switch (e.getToken().getType()) {
            case( Token.T_NUMBER):      return e.getExpr1().getToken().value;
            case( Token.T_IDENTIFIER ): return getValue( e.getExpr1().getToken(), values );
            case( Token.T_OPEN_PARENTHESIS ): return readExpression( e.getExpr1(), values );
            case( Token.T_PLUS ) :     return readExpression( e.getExpr1(), values ) + readExpression( e.getExpr2(), values );
            case( Token.T_DIVIDE ) :   return readExpression( e.getExpr1(), values ) / readExpression( e.getExpr2(), values );
            case( Token.T_MULTIPLY ) : return readExpression( e.getExpr1(), values ) * readExpression( e.getExpr2(), values );
            case( Token.T_MINUS ) :    return readExpression( e.getExpr1(), values ) - readExpression( e.getExpr2(), values );
            case( Token.T_POW ) :      return Math.pow(   readExpression( e.getExpr1(), values ), readExpression( e.getExpr2(), values ) );
            case( Token.T_LN ) :       return Math.log(   readExpression( e.getExpr1(), values ) );
            case( Token.T_LOG ) :      return Math.log10( readExpression( e.getExpr2(), values ) )/Math.log10( readExpression( e.getExpr1(), values ) );
            case( Token.T_SQRT ) :     return Math.sqrt(  readExpression( e.getExpr1(), values ) );
            case( Token.T_COS ) :      return Math.cos(   readExpression( e.getExpr1(), values ) );
            case( Token.T_ARC_COS ) :  return Math.acos(  readExpression( e.getExpr1(), values ) );
            case( Token.T_SIN ) :      return Math.sin(   readExpression( e.getExpr1(), values ) );
            case( Token.T_ARC_SIN ) :  return Math.asin(  readExpression( e.getExpr1(), values ) );
            case( Token.T_TAN ) :      return Math.tan(   readExpression( e.getExpr1(), values ) );
            case( Token.T_ARC_TAN ) :  return Math.atan(  readExpression( e.getExpr1(), values ) );
        }
        
        return 0;
    }
    
    private static Double getValue( final Token token, final Map<String,Double> values )
    {
        Double result = values.get( token.stringValue );
        if (result == null) {
            // TODO lancia un'eccezione indicando che non c'e' un mapping tra l'IDE e i valori in input
            
        }
        return result;
    }
}