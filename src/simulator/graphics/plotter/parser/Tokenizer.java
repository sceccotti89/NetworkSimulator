
package simulator.graphics.plotter.parser;

import java.util.HashMap;
import java.util.Map;

public class Tokenizer
{
    private int index;
    private String expression;
    
    /* Table maps used to reserve words for token type. */
    private Map<String, Integer> table;
    
    public Tokenizer( final String expression ) {
        this.expression = expression;
        index = 0;
        
        init_table();
    }
    
    private void init_table()
    {
        table = new HashMap<>( 8 );
        
        table.put( "log",    Token.T_LOG );
        table.put( "ln",     Token.T_LN );
        table.put( "sin",    Token.T_SIN );
        table.put( "arcsin", Token.T_ARC_SIN );
        table.put( "cos",    Token.T_COS );
        table.put( "arccos", Token.T_ARC_COS );
        table.put( "tan",    Token.T_TAN );
        table.put( "arctan", Token.T_ARC_TAN );
    }
    
    public int getIndex() {
        return index;
    }

    private void consume() {
        index++;
    }
    
    /**
     * Ignores tabs and whitespaces.
    */
    private void whiteSpaces()
    {
        while (index < expression.length()) {
            char c = expression.charAt( index );
            if(c == ' ' || c == '\t'){
                consume();
            } else {
                break;
            }
        }
    }
    
    public Token nextToken()
    {
        whiteSpaces();
        
        if (index == expression.length())
            return new Token( Token.T_EOF );
        
        return null;
    }
    
    public static class Token
    {
        public static final int
            // Operational tokens.
            T_DIVIDE                = '/',
            T_MULTIPLY              = '*', // 42
            T_PLUS                  = '+', // 43
            T_MINUS                 = '-', // 45
            
            // Miscellaneous tokens.
            T_COMMA                 = ',', // 44
            T_OPEN_PARENTHESIS      = '(', // 40
            T_CLOSED_PARENTHESIS    = ')', // 41
            T_IDENTIFIER            =  24,
            T_NUMBER                =  25,
            
            // Command tokens.
            T_LOG                   = 26,
            T_LN                    = 27,
            T_SIN                   = 28,
            T_ARC_SIN               = 29,
            T_COS                   = 30,
            T_ARC_COS               = 31,
            T_TAN                   = 32,
            T_ARC_TAN               = 33,
            T_SQRT                  = 34,
            T_UPPER                 = '^',
            
            // Exception tokens.
            T_EOF                   = 46,
            T_UNKNOWN               = 47;
        
        /* One of the above token codes. */
        public Integer type;
        /* Holds value if number. */
        public Double value = null;
        /* Holds value if string. */
        public String stringValue = null;
        
        public Token( final int type ) {
            this.type = type;
        }
        
        public int getType() {
            return type;
        }

        /** 
         * Returns the string value of a token.
         * 
         * @param TAG    ID of the token
         * 
         * @return the string representation.
        */
        public static String getTokenValue( int TAG )
        {
            switch( TAG ) {
                // Operational tokens.
                case( T_DIVIDE ):             return "/";
                case( T_MULTIPLY ):           return "*";
                case( T_PLUS ):               return "+";
                case( T_MINUS ):              return "-";
                
                // Miscellaneous tokens.
                case( T_COMMA ):              return ",";
                case( T_OPEN_PARENTHESIS ):   return "(";
                case( T_CLOSED_PARENTHESIS ): return ")";
                case( T_IDENTIFIER ):         return "Identifier";
                
                // Type tokens.
                case( T_NUMBER ):             return "Number";
            }
            
            return null;
        }
        
        @Override
        public String toString() {
            return getTokenValue( type );
        }
    }
}