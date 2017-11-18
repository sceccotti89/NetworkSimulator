/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.plotter.evaluator;

import simulator.graphics.plotter.evaluator.Tokenizer.Token;

public class Expr
{
    protected Token op;
    protected Expr e1;
    protected Expr e2;
    
    public Expr( Token op ) {
        this.op = op;
    }
    
    public Expr( Token op, Expr e1 ) {
        this( op );
        this.e1 = e1;
    }
    
    public Expr( Token op, Expr e1, Expr e2 ) {
        this( op );
        this.e1 = e1;
        this.e2 = e2;
    }
    
    public Expr getExpr1() {
        return e1;
    }
    
    public Expr getExpr2() {
        return e2;
    }
    
    public Token getOp() {
        return op;
    }
    
    @Override
    public String toString()
    {
        if (e2 == null) {
            return op + "(" + e1 + ")";
        } else {
            if (op.getType() == Token.T_LOG) {
                return "log" + e1 + "(" + e2 + ")";
            } else {
                return e1 + "" + op + (e2 != null ? e2 : "");
            }
        }
    }
    
    public static class Term extends Expr
    {
        public Term( Token token )
        {
            super( token );
            e1 = this;
        }
        
        @Override
        public String toString() {
            return op.toString();
        }
    }
}
