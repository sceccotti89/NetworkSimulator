
package simulator.graphics.plotter.parser;

import simulator.graphics.plotter.parser.Tokenizer.Token;

public class Expression
{
    protected Token token;
    protected Expression e1;
    protected Expression e2;
    
    public Expression( final Token token ) {
        this.token = token;
    }
    
    public Expression( final Token token, final Expression e1 ) {
        this( token );
        this.e1 = e1;
    }
    
    public Expression( final Token token, final Expression e1, final Expression e2 ) {
        this( token );
        this.e1 = e1;
        this.e2 = e2;
    }
    
    public Expression getExpr1() {
        return e1;
    }
    
    public Expression getExpr2() {
        return e2;
    }
    
    public Token getToken() {
        return token;
    }
    
    @Override
    public String toString() {
        if (e2 == null) return e1.toString();
        else return e1.toString() + token.toString() + (e2 != null ? e2.toString() : "");
    }
    
    public static class Term extends Expression
    {
        public Term( final Token token ) {
            super( token );
        }
    }
}