
package simulator.utils;

public class Size<T extends Number>
{
    private final T _size;
    private final SizeUnit _sUnit;
    
    public Size( T size, SizeUnit sUnit )
    {
        _size = size;
        _sUnit = sUnit;
    }
    
    public T getSize() {
        return _size;
    }
    
    public SizeUnit getSizeUnit() {
        return _sUnit;
    }
    
    public double getBits() {
        return _sUnit.getBits( _size.doubleValue() );
    }
    
    public double getBytes() {
        return _sUnit.getBytes( _size.doubleValue() );
    }
}
