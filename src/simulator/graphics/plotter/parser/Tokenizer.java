
package simulator.graphics.plotter.parser;

import java.util.HashMap;
import java.util.Map;

public class Tokenizer
{
    private int index;
    private String expression;
    
    /* Table maps used to reserve words for token type. */
    private Map<String, Integer> table;
    
    public Tokenizer( final String expression )
    {
        this.expression = expression;
        index = 0;
        init_table();
    }
    
    private void init_table()
    {
        table = new HashMap<>( 8 );
        
        table.put( "sqrt", Token.T_SQRT );
        table.put( "log",  Token.T_LOG );
        table.put( "ln",   Token.T_LN );
        table.put( "sin",  Token.T_SIN );
        table.put( "asin", Token.T_ARC_SIN );
        table.put( "cos",  Token.T_COS );
        table.put( "acos", Token.T_ARC_COS );
        table.put( "tan",  Token.T_TAN );
        table.put( "atan", Token.T_ARC_TAN );
    }
    
    public int getIndex() {
        return index;
    }
    
    private char peek() {
        return expression.charAt( index );
    }
    
    private char next() {
        return expression.charAt( index++ );
    }

    private void consume() {
        index++;
    }
    
    private boolean isEoF() {
        return index == expression.length();
    }
    
    private void rewind( final int length ) {
        index -= length;
    }
    
    private void skipTabsAndWhiteSpaces()
    {
        while (index < expression.length()) {
            char c = peek();
            if(c == ' ' || c == '\t'){
                consume();
            } else {
                break;
            }
        }
    }
    
    /**
     * Gets the next token produced by the parser.
     * 
     * @return the next token if found, null otherwise
    */
    private Token getToken() throws RuntimeException
    {
        StringBuilder ide = new StringBuilder( 16 );
        boolean is_number = true;
        boolean number_founded = false;
        boolean point = false;
        Token token = null;
        
        char c = peek();
        
        // check if the first element is a character or a number
        if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_') {
            ide.append( c );
            is_number = false;
        } else {
            if ((c >= '0' && c <= '9') || c == '.') {
                if(c == '.') point = true;
                ide.append( c );
                number_founded = true;
            } else {
                return null;
            }
        }
        
        consume();
        
        while (!isEoF()) {
            c = peek();
            
            // check if it is a character or a number
            if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_') {
                if(is_number){
                    ide.append( c );
                    throw new RuntimeException( "Error position " + getIndex() + ": syntax error on token \"" + c + "\", delete this token." );
                } else {
                    ide.append( c );
                }
            } else {
                if ((c >= '0' && c <= '9') || c == '.') {
                    ide.append( c );
                    
                    // it may be a double or a float
                    if(c == '.'){
                        if(!is_number)
                            throw new RuntimeException( "Error position " + getIndex() + ": syntax error on token \"" + c + "\", delete this token." );
                        
                        if(point)
                            throw new RuntimeException( "Error position " + getIndex() + ": duplication of token \".\"." );
                        else
                            point = true;
                    }
                    
                    if(!number_founded)
                        number_founded = true;
                } else {
                    break;
                }
            }
            
            consume();
        }
        
        if (ide.length() > 0) {
            if (is_number) {
                if (!number_founded) {
                    rewind( 1 );
                    return null;
                } else {
                    token = new Token( Token.T_NUMBER );
                    token.value = Double.parseDouble( ide.toString() );
                }
            } else {
                // Identifier.
                if(table.containsKey( ide.toString() )) {
                    token = new Token( table.get( ide.toString() ) );
                } else {
                    token = new Token( Token.T_IDENTIFIER );
                    token.stringValue = ide.toString();
                }
            }
            
            return token;
        }
        
        return null;
    }
    
    public Token nextToken()
    {
        skipTabsAndWhiteSpaces();
        
        if (isEoF())
            return new Token( Token.T_EOF );
        
        System.out.println( "CURRENT: " + peek() );
        
        Token token = getToken();
        if (token == null) {
            token = new Token( next() );
        }
        return token;
    }
    
    public static class Token
    {
        // Operational tokens.
        public static final int T_DIVIDE             = '/'; // 47
        public static final int T_MULTIPLY           = '*'; // 42
        public static final int T_PLUS               = '+'; // 43
        public static final int T_MINUS              = '-'; // 45
            
        // Miscellaneous tokens.
        public static final int T_OPEN_PARENTHESIS   = '('; // 40
        public static final int T_CLOSED_PARENTHESIS = ')'; // 41
        public static final int T_IDENTIFIER         =  24;
        public static final int T_NUMBER             =  25;
            
        // Command tokens.
        public static final int T_LOG                = 26;
        public static final int T_LN                 = 27;
        public static final int T_SIN                = 28;
        public static final int T_ARC_SIN            = 29;
        public static final int T_COS                = 30;
        public static final int T_ARC_COS            = 31;
        public static final int T_TAN                = 32;
        public static final int T_ARC_TAN            = 33;
        public static final int T_SQRT               = 34;
        public static final int T_POW                = '^'; // 94
            
        // Exception tokens.
        public static final int T_EOF                = 46;
        public static final int T_UNKNOWN            = 48;
        
        /* One of the above token codes. */
        private Integer type;
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
                case( T_POW ):                return "^";
                
                // Miscellaneous tokens.
                case( T_OPEN_PARENTHESIS ):   return "(";
                case( T_CLOSED_PARENTHESIS ): return ")";
                case( T_SQRT ):               return "sqrt";
                case( T_LN ):                 return "ln";
                case( T_LOG ):                return "log";
                case( T_SIN ):                return "sin";
                case( T_ARC_SIN ):            return "asin";
                case( T_COS ):                return "cos";
                case( T_ARC_COS ):            return "acos";
                case( T_TAN ):                return "tan";
                case( T_ARC_TAN ):            return "atan";
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