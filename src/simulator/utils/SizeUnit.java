
package simulator.utils;

public enum SizeUnit
{
    BIT,
    KILOBIT,
    MEGABIT,
    GIGABIT,
    TERABIT,
    
    BYTE,
    KILOBYTE,
    MEGABYTE,
    GIGABYTE,
    TERABYTE;
    
    private static final long BYTES = 8L;
    private static final long KILO  = 1024L;
    private static final long MEGA  = 1024L * 1024L;
    private static final long GIGA  = 1024L * 1024L * 1024L;
    private static final long TERA  = 1024L * 1024L * 1024L * 1024L;
    
    
    
    public double getBytes( double value )
    {
        switch( this ) {
            case BIT :     return value / BYTES;
            case KILOBIT : return value * KILO / BYTES;
            case MEGABIT : return value * MEGA / BYTES;
            case GIGABIT : return value * GIGA / BYTES;
            case TERABIT : return value * TERA / BYTES;
            
            case BYTE :     return value;
            case KILOBYTE : return value * KILO;
            case MEGABYTE : return value * MEGA;
            case GIGABYTE : return value * GIGA;
            case TERABYTE : return value * TERA;
        }
        
        return value;
    }
    
    public static double getBytes( double value, SizeUnit size )
    {
        switch( size ) {
            case BIT :     return value / BYTES;
            case KILOBIT : return value * KILO / BYTES;
            case MEGABIT : return value * MEGA / BYTES;
            case GIGABIT : return value * GIGA / BYTES;
            case TERABIT : return value * TERA / BYTES;
            
            case BYTE :     return value;
            case KILOBYTE : return value * KILO;
            case MEGABYTE : return value * MEGA;
            case GIGABYTE : return value * GIGA;
            case TERABYTE : return value * TERA;
        }
        
        return value;
    }
    
    public double getBits( double value )
    {
        switch( this ) {
            case BIT :     return value;
            case KILOBIT : return value * KILO;
            case MEGABIT : return value * MEGA;
            case GIGABIT : return value * GIGA;
            case TERABIT : return value * TERA;
            
            case BYTE :    return value * BYTES;
            case KILOBYTE: return value * KILO * BYTES;
            case MEGABYTE: return value * MEGA * BYTES;
            case GIGABYTE: return value * GIGA * BYTES;
            case TERABYTE: return value * TERA * BYTES;
        }
        return value;
    }
    
    public static double getBits( double value, SizeUnit size )
    {
        switch( size ) {
            case BIT :     return value;
            case KILOBIT : return value * KILO;
            case MEGABIT : return value * MEGA;
            case GIGABIT : return value * GIGA;
            case TERABIT : return value * TERA;
            
            case BYTE :    return value * BYTES;
            case KILOBYTE: return value * KILO * BYTES;
            case MEGABYTE: return value * MEGA * BYTES;
            case GIGABYTE: return value * GIGA * BYTES;
            case TERABYTE: return value * TERA * BYTES;
        }
        return value;
    }
}
