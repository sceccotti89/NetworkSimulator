/**
 * @author Stefano Ceccotti
*/

package simulator.network.protocols;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import simulator.events.Event;
import simulator.network.Packet;
import simulator.utils.Time;

public class Header extends Event implements Packet
{
    private boolean[] header;
    private int index;
    //private Header next;
    
    
    
    public Header() {
        this( 0 );
    }
    
    public Header( final byte[] data )
    {
        this( data.length * Byte.SIZE );
        byteArray2BitArray( data );
    }
    
    public Header( final int size )
    {
        super( new Time( 0, TimeUnit.MICROSECONDS ) );
        header = new boolean[size];
    }
    
    /**
     * Converts a byte array to a boolean array. Bit 0 is represented with false,
     * Bit 1 is represented with 1.
     * 
     * @param bytes    the byte array
    */
    private void byteArray2BitArray( final byte[] bytes )
    {
        for (int i = 0; i < bytes.length * 8; i++) {
            if ((bytes[i / 8] & (1 << (7 - (i % 8)))) > 0) {
                header[i] = true;
            }
        }
    }
    
    /**
     * Returns the byte representation of the data.
    */
    public byte[] getBytes()
    {
        byte[] toReturn = new byte[header.length / 8];
        for (int entry = 0; entry < toReturn.length; entry++) {
            for (int bit = 0; bit < 8; bit++) {
                if (header[entry * 8 + bit]) {
                    toReturn[entry] |= (128 >> bit);
                }
            }
        }
        return toReturn;
    }
    
    public void clear() {
        clear( 0 );
    }
    
    public void clear( final int newSize )
    {
        index = 0;
        if (newSize <= 0) {
            header = new boolean[0];
        } else {
            header = new boolean[newSize];
        }
    }
    
    /**
     * Puts the input header at the end of this.
     * 
     * @param h    the header to add.
    */
    public void addHeader( final Header h ) {
        addHeader( index, h );
    }
    
    /**
     * Puts the input header in the specified position.</br>
     * If there's no space for the header a resize will be done.
     * 
     * @param offset    position where the header will be added.
     * @param h         the header.
    */
    public void addHeader( final int offset, final Header h )
    {
        final int length = getSizeInBits();
        int dimension = offset + h.getSizeInBits();
        if (dimension > length) {
            header = Arrays.copyOf( header, dimension );
        }
        //header = Arrays.copyOf( header, length + h.getSizeInBits() );
        //System.arraycopy( h.header, 0, header, length, h.getSizeInBits() );
        System.arraycopy( h.header, 0, header, index, h.getSizeInBits() );
        index = Math.max( index, dimension );
    }
    
    /**
     * Returns the top {@code length} bits from this header.</br>
     * This is equivalent to {@code getSubHeader(0,length)}.
     * 
     * @param length    number of bits to retrieve.
    */
    public Header getHeader( final int length ) {
        return getSubHeader( 0, length );
    }
    
    /**
     * Returns {@code length} bits from this header,
     * starting from an initial {@code offset} position.
     * 
     * @param offset    the starting position.
     * @param length    number of bits to retrieve.
    */
    public Header getSubHeader( final int offset, final int length )
    {
        Header h = new Header( length );
        System.arraycopy( header, offset, h.header, 0, length );
        return h;
    }
    
    /**
     * Removes the top {@code length} bits from this header.</br>
     * This is equivalent to {@code removeHeader(0,length)}.
     * 
     * @param length    number of bits to remove.
    */
    public Header removeHeader( final int length ) {
        return removeHeader( 0, length );
    }
    
    /**
     * Removes {@code length} bits from this header,
     * starting from an initial {@code offset} position.
     * 
     * @param offset    the starting position.
     * @param length    number of bits to remove.
    */
    public Header removeHeader( final int offset, final int length )
    {
        Header h = new Header( length );
        System.arraycopy( header, offset, h.header, 0, length );
        
        if (offset < index) {
            if (offset + length < index) {
                index -= length;
            } else {
                index = offset;
            }
        }
        
        // Resize the array, copying the remaining segments.
        boolean[] newHeader = new boolean[offset + (header.length - (offset + length))];
        System.arraycopy( header, 0, newHeader, 0, offset );
        System.arraycopy( header, offset + length, newHeader, offset, header.length - (offset + length) );
        header = newHeader;
        
        return h;
    }
    
    /**
     * Sets the content of the field into the destination header,
     * starting from an initial position with a fixed number of bits.
     * 
     * @param range    contains the informatin used to identify the field.
     * @param value    the value to set.
    */
    public void setField( final Range range, final int value ) {
        setField( range.from, value, range.length );
    }
    
    /**
     * Sets the content of the field into the destination header,
     * starting from an initial position with a fixed number of bits.
     * 
     * @param offset    starting position.
     * @param input     the given input.
     * @param bits      length (in bits) of the field.
    */
    public void setField( final int offset, final int input, final int bits )
    {
        for (int i = 0; i < bits; i++) {
            header[offset+i] = (1 << bits - i - 1 & input) != 0;
        }
        index = Math.max( index, offset + bits );
    }
    
    /**
     * Sets the content of the given integer bit vector into this header,
     * whose boundaries are given by the range.
     * 
     * @param range    contain field boundaries.
     * @param bits     the given input vector.
    */
    public void setField( final Range range, final int[] bits ) {
        setField( range.from, bits );
    }
    
    /**
     * Sets the content of the given integer bit vector into this header,
     * starting from an initial position.
     * 
     * @param offset    starting position.
     * @param bits      the given input vector.
    */
    private void setField( final int offset, final int[] bits )
    {
        for (int i = 0; i < bits.length; i++) {
            header[offset+i] = (bits[i] == 1);
        }
        index = Math.max( index, offset + bits.length );
    }
    
    /**
     * Returns an integer representing the value contained in the field,
     * specified by the given range.
     * 
     * @param range    contain the values to retrieve the field.
    */
    public int getField( final Range range ) {
        return getField( range.from, range.length );
    }
    
    /**
     * Returns an integer representing the value contained in the field,
     * specified by the starting position and its length.
     * 
     * @param offset    starting position.
     * @param length    length of the filed (in bits).
    */
    public int getField( final int offset, final int length )
    {
        int value = 0;
        for (int i = 0; i < length; i++) {
            value = value << 1 | (header[offset+i] ? 1 : 0);
        }
        return value;
    }
    
    /**
     * Returns the value of the bit contained in the position start + offset,
     * where start is taken from the given range.
     * 
     * @param range     contains the starting position of the field.
     * @param offset    the given offset.
    */
    public int getBitField( final Range range, final int offset ) {
        return getBitField( range.from + offset );
    }
    
    /**
     * Returns the bit contained in the given absolute position.
     * 
     * @param position    the bit position.
    */
    public int getBitField( final int position ) {
        return header[position] ? 1 : 0;
    }
    
    /**
     * Returns the size (in Bytes) of the header,
     * summing up all the headers founded stating from this one.
    */
    public int getSizeInBits() {
        return header.length;
    }
    
    /**
     * Returns the size (in Bytes) of the header,
     * summing up all the headers founded stating from this one.
    */
    public int getSizeInBytes() {
        return header.length / Byte.SIZE;
    }
    
    public static class Range
    {
        private int from;
        private int length;
        
        public Range( final int from, final int length )
        {
            this.from = from;
            this.length = length;
        }
        
        public void setFrom( final int from ) {
            this.from = from;
        }
        
        public int from() {
            return from;
        }
        
        public int length() {
            return length;
        }
        
        @Override
        public String toString() {
            return "Offset: " + from + ", Length: " + length;
        }
    }
}