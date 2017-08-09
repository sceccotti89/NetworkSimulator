
package simulator.graphics.plotter.parser;

import simulator.graphics.plotter.parser.Expression.Term;
import simulator.graphics.plotter.parser.Tokenizer.Token;

public class FunctionParser
{
    private Tokenizer tokenizer;
    
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
        Token token = tokenizer.nextToken();
        Expression expr;
        Token base = null;
        
        if (token.getType() == Token.T_NUMBER || token.getType() == Token.T_IDENTIFIER) {
            Expression term = new Term( token );
            expr = parseRIGHT_EXPR( term );
        } else {
            if (token.getType() == Token.T_LOG) {
                // Get the base of the logarithm.
                base = tokenizer.nextToken();
                expectedToken( base.getType(), Token.T_NUMBER );
            }
            if (token.getType() != Token.T_OPEN_PARENTHESIS) {
                expectedToken( tokenizer.nextToken().getType(), Token.T_OPEN_PARENTHESIS );
            }
            
            if (base == null) {
                expr = new Expression( token, parseEXPRESSION() );
            } else {
                expr = new Expression( token, new Term( base ), parseEXPRESSION() );
            }
            
            if (token.getType() != Token.T_OPEN_PARENTHESIS) {
                expectedToken( tokenizer.nextToken().getType(), Token.T_CLOSED_PARENTHESIS );
            }
        }
        
        return expr;
    }
    
    private Expression parseRIGHT_EXPR( final Expression e1 )
    {
        Token token = tokenizer.nextToken();
        if (token.getType() == Token.T_EOF) {
            return e1;
        }
        
        if (token.getType() != Token.T_PLUS && token.getType() != Token.T_MINUS &&
            token.getType() != Token.T_MULTIPLY && token.getType() != Token.T_DIVIDE &&
            token.getType() != Token.T_UPPER) {
            // TODO lanciare un'eccezione per token non corretto
            
        }
        
        Expression e2 = parseEXPRESSION();
        return new Expression( token, e1, e2 );
    }
}