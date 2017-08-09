
package simulator.graphics.plotter.parser;

import java.util.Map;

import simulator.graphics.plotter.parser.Tokenizer.Token;

public class FunctionInterpreter
{
    public static double interpret( final Expression e, final Map<String,Double> values ) {
        return readExpression( e, values );
    }
    
    private static double readExpression( final Expression e, final Map<String,Double> values )
    {
        // Get the value of the first expression.
        Double value = getValue( e.getExpr1().getToken(), values );
        if (value == null)
            ;// TODO lancia un'eccezione dicendo che non c'e' un mapping tra l'IDE e i valori in input
        
        switch (e.getToken().getType()) {
            case( Token.T_PLUS ) :     value += readExpression( e.getExpr2(), values ); break;
            case( Token.T_DIVIDE ) :   value /= readExpression( e.getExpr2(), values ); break;
            case( Token.T_MULTIPLY ) : value *= readExpression( e.getExpr2(), values ); break;
            case( Token.T_MINUS ) :    value -= readExpression( e.getExpr2(), values ); break;
            case( Token.T_UPPER ) :    value = Math.pow(   value, readExpression( e.getExpr2(), values ) ); break;
            case( Token.T_LN ) :       value = Math.log(   readExpression( e.getExpr1(), values ) ); break;
            case( Token.T_LOG ) :      value = Math.log10( readExpression( e.getExpr2(), values ) )/Math.log10( value ); break;
            case( Token.T_SQRT ) :     value = Math.sqrt(  readExpression( e.getExpr1(), values ) ); break;
            case( Token.T_COS ) :      value = Math.cos(   readExpression( e.getExpr1(), values ) ); break;
            case( Token.T_ARC_COS ) :  value = Math.acos(  readExpression( e.getExpr1(), values ) ); break;
            case( Token.T_SIN ) :      value = Math.sin(   readExpression( e.getExpr1(), values ) ); break;
            case( Token.T_ARC_SIN ) :  value = Math.asin(  readExpression( e.getExpr1(), values ) ); break;
            case( Token.T_TAN ) :      value = Math.tan(   readExpression( e.getExpr1(), values ) ); break;
            case( Token.T_ARC_TAN ) :  value = Math.atan(  readExpression( e.getExpr1(), values ) ); break;
        }
        
        return value;
    }
    
    private static Double getValue( final Token token, final Map<String,Double> values )
    {
        if (token.getType() == Token.T_NUMBER)     return token.value;
        if (token.getType() == Token.T_IDENTIFIER) return values.get( token.stringValue );
        return 0d;
    }
}