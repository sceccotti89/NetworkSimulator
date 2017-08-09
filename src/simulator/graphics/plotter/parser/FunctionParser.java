
package simulator.graphics.plotter.parser;

import simulator.graphics.plotter.parser.Expression.Term;
import simulator.graphics.plotter.parser.Tokenizer.Token;

public class FunctionParser
{
    private Tokenizer tokenizer;
    private Token token;
    
    public FunctionParser( final String expression ) {
        tokenizer = new Tokenizer( expression );
    }
    
    public Expression parse() {
        return parseEXPRESSION();
    }
    
    /**
     * Checks if the next token is the expected one.
     * 
     * @param current     the current token
     * @param expected    the expected token
    */
    private void expectedToken( int current, int expected ) throws RuntimeException
    {
        if(current != expected)
            throw new RuntimeException( "Error position " + tokenizer.getIndex() +
                                        ": expected token: \"" + Token.getTokenValue( expected ) +
                                        "\", instead of: \"" +  Token.getTokenValue( current ) + "\"." );
    }

    private Expression parseEXPRESSION()
    {
        Expression expr;
        Token base = null;
        
        token = tokenizer.nextToken();
        System.out.println( "TOKEN: " + token );
        if (token.getType() == Token.T_NUMBER || token.getType() == Token.T_IDENTIFIER) {
            Expression term = new Term( token );
            System.out.println( "TERM: " + token );
            expr = parseRIGHT_EXPR( term );
        } else {
            if (token.getType() == Token.T_LOG) {
                // Get the base of the logarithm.
                base = tokenizer.nextToken();
                expectedToken( base.getType(), Token.T_NUMBER );
            }
            
            Token oldToken = token;
            if (token.getType() != Token.T_OPEN_PARENTHESIS) {
                System.out.println( "SONO QUI 1" );
                expectedToken( tokenizer.nextToken().getType(), Token.T_OPEN_PARENTHESIS );
            }
            
            if (base == null) {
                expr = new Expression( token, parseEXPRESSION() );
            } else {
                expr = new Expression( token, new Term( base ), parseEXPRESSION() );
                System.out.println( "TERMINATA RICORSIONE" );
            }
            
            if (oldToken.getType() != Token.T_OPEN_PARENTHESIS) {
                System.out.println( "SONO QUI 2" );
                expectedToken( token.getType(), Token.T_CLOSED_PARENTHESIS );
            }
        }
        
        return expr;
    }
    
    private Expression parseRIGHT_EXPR( final Expression e1 )
    {
        token = tokenizer.nextToken();
        System.out.println( "RIGHT: " + token );
        if (token.getType() == Token.T_EOF) {
            return e1;
        }
        
        if (token.getType() != Token.T_PLUS && token.getType() != Token.T_MINUS &&
            token.getType() != Token.T_MULTIPLY && token.getType() != Token.T_DIVIDE &&
            token.getType() != Token.T_UPPER) {
            return e1;
        }
        
        Token currentToken = token;
        Expression e2 = parseEXPRESSION();
        return new Expression( currentToken, e1, e2 );
    }
}