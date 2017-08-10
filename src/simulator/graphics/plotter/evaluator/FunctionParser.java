/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.plotter.evaluator;

import simulator.graphics.plotter.evaluator.Expr.Term;
import simulator.graphics.plotter.evaluator.Tokenizer.Token;

public class FunctionParser
{
    private static Tokenizer tokenizer;
    private static Token token;
    
    public FunctionParser( final String expression ) {
        tokenizer = new Tokenizer( expression );
    }
    
    public static Expr parse( final String expression ) {
        tokenizer = new Tokenizer( expression );
        return parseEXPRESSION();
    }
    
    /**
     * Checks if the next token is the expected one.
     * 
     * @param current     the current token
     * @param expected    the expected token
    */
    private static void expectedToken( int current, int expected ) throws RuntimeException
    {
        if(current != expected) {
            throw new RuntimeException( "Error position " + tokenizer.getIndex() +
                                        ": expected token: \"" + Token.getTokenValue( expected ) +
                                        "\", instead of: \"" +  Token.getTokenValue( current ) + "\"." );
        }
    }
    
    private static Expr parseEXPRESSION()
    {
        Expr expr;
        Token base = null;
        
        token = tokenizer.nextToken();
        System.out.println( "TOKEN: " + token );
        if (token.getType() == Token.T_NUMBER || token.getType() == Token.T_IDENTIFIER) {
            Expr term = new Term( token );
            System.out.println( "TERM: " + token );
            expr = parseRIGHT_EXPR( term );
        } else {
            if (token.getType() == Token.T_LOG) {
                // Get the base of the logarithm.
                base = tokenizer.nextToken();
                expectedToken( base.getType(), Token.T_NUMBER );
            }
            
            if (token.getType() != Token.T_OPEN_PARENTHESIS) {
                System.out.println( "SONO QUI 1" );
                expectedToken( tokenizer.nextToken().getType(), Token.T_OPEN_PARENTHESIS );
            }
            
            if (base == null) {
                expr = new Expr( token, parseEXPRESSION() );
            } else {
                expr = new Expr( token, new Term( base ), parseEXPRESSION() );
                System.out.println( "TERMINATA RICORSIONE" );
            }
            
            expectedToken( token.getType(), Token.T_CLOSED_PARENTHESIS );
            
            expr = parseRIGHT_EXPR( expr );
        }
        
        return expr;
    }
    
    private static Expr parseRIGHT_EXPR( final Expr e1 )
    {
        token = tokenizer.nextToken();
        System.out.println( "RIGHT: " + token );
        if (token.getType() == Token.T_EOF) {
            return e1;
        }
        
        if(token.getType() == Token.T_CLOSED_PARENTHESIS) {
            return e1;
        }
        
        if (token.getType() != Token.T_PLUS && token.getType() != Token.T_MINUS &&
            token.getType() != Token.T_MULTIPLY && token.getType() != Token.T_DIVIDE &&
            token.getType() != Token.T_POW) {
            throw new EvaluationException( "Invalid token '" + token.toString() + "'." );
        }
        
        Token currentToken = token;
        Expr e2 = parseEXPRESSION();
        System.out.println( "TOKEN: " + currentToken + ", E2: " + e2 );
        return new Expr( currentToken, e1, e2 );
    }
}