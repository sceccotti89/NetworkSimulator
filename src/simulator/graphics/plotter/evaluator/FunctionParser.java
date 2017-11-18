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
    private static boolean readNext = true;
    
    public FunctionParser( String expression ) {
        tokenizer = new Tokenizer( expression );
    }
    
    public static Expr parse( String expression ) {
        tokenizer = new Tokenizer( expression );
        return parseEXPR();
    }
    
    /**
     * Checks if the next token is the expected one.
     * 
     * @param current     the current token
     * @param expected    the expected token
    */
    private static void expectedToken( int current, int expected ) throws EvaluationException
    {
        if(current != expected) {
            throw new EvaluationException( "Error position " + tokenizer.getIndex() +
                                           ": expected token: \"" + Token.getTokenValue( expected ) +
                                           "\", instead of: \"" +  Token.getTokenValue( current ) + "\"." );
        }
    }
    
    private static Expr parseEXPR() {
        return parseSUM_OP( parseSIGNED_TERM() );
    }
    
    private static Expr parseSUM_OP( Expr e )
    {
        if (readNext)
            token = tokenizer.nextToken();
        readNext = true;
        
        if (token.getType() == Token.T_PLUS || token.getType() == Token.T_MINUS) {
            return parseSUM_OP( new Expr( token.clone(), e, parseTERM() ) );
        } else {
            readNext = false;
            return e;
        }
    }
    
    private static Expr parseSIGNED_TERM() {
        return parseTERM();
    }
    
    private static Expr parseTERM()
    {
        if (readNext)
            token = tokenizer.nextToken();
        readNext = true;
        
        Expr e;
        if (token.getType() == Token.T_PLUS || token.getType() == Token.T_MINUS) {
            e = new Expr( token.clone(), parseFACTOR() );
        } else {
            readNext = false;
            e = parseFACTOR();
        }
        
        return parseTERM_OP( e );
    }
    
    private static Expr parseTERM_OP( Expr e )
    {
        if (readNext)
            token = tokenizer.nextToken();
        readNext = true;
        
        if (token.getType() == Token.T_MULTIPLY || token.getType() == Token.T_DIVIDE) {
            return parseTERM_OP( new Expr( token.clone(), e, parseSIGNED_FACTOR() ) );
        } else {
            readNext = false;
            return e;
        }
    }
    
    private static Expr parseSIGNED_FACTOR()
    {
        if (readNext)
            token = tokenizer.nextToken();
        readNext = true;
        
        if (token.getType() == Token.T_MULTIPLY || token.getType() == Token.T_DIVIDE) {
            return new Expr( token.clone(), parseFACTOR() );
        } else {
            readNext = false;
            return parseFACTOR();
        }
    }
    
    private static Expr parseFACTOR() {
        return parseFACTOR_OP( parseARGUMENT() );
    }
    
    private static Expr parseFACTOR_OP( Expr e )
    {
        if (readNext)
            token = tokenizer.nextToken();
        readNext = true;
        
        if (token.getType() == Token.T_POW) {
            return new Expr( token.clone(), e, parseARGUMENT() );
        } else {
            readNext = false;
            return e;
        }
    }
    
    private static Expr parseARGUMENT()
    {
        if (readNext)
            token = tokenizer.nextToken();
        readNext = true;
        
        switch( token.getType() ) {
            case( Token.T_NUMBER ):
            case( Token.T_IDENTIFIER ):
                return new Term( token );
            case( Token.T_OPEN_PARENTHESIS ):
                Expr e = parseEXPR();
                expectedToken( token.getType(), Token.T_CLOSED_PARENTHESIS );
                readNext = true;
                return e;
            default: // Function.
                Token function = token.clone();
                if (token.getType() == Token.T_LOG) {
                    // Get the logarithm base.
                    Term base = new Term( tokenizer.nextToken() );
                    return new Expr( function, base, parseARGUMENT() );
                } else {
                    return new Expr( function, parseARGUMENT() );
                }
        }
    }
}
