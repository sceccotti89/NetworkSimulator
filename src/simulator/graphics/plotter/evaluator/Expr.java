
package simulator.graphics.plotter.evaluator;

import simulator.graphics.plotter.evaluator.Tokenizer.Token;

public class Expr
{
    protected Token token;
    protected Expr e1;
    protected Expr e2;
    
    public Expr( final Token token ) {
        this.token = token;
    }
    
    public Expr( final Token token, final Expr e1 ) {
        this( token );
        this.e1 = e1;
    }
    
    public Expr( final Token token, final Expr e1, final Expr e2 ) {
        this( token );
        this.e1 = e1;
        this.e2 = e2;
    }
    
    public Expr getExpr1() {
        return e1;
    }
    
    public Expr getExpr2() {
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
    
    public static class Term extends Expr
    {
        public Term( final Token token ) {
            super( token );
            e1 = this;
        }
        
        @Override
        public String toString() {
            return token.toString();
        }
    }
}