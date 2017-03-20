
package simulator.utils;

public class Pair<F,S>
{
    private final F left;
    private final S right;
    
    public Pair( final F left, final S right )
    {
        this.left = left;
        this.right = right;
    }
    
    public F getFirst() {
        return left;
    }
    
    public S getSecond() {
        return right;
    }
    
    @Override
    public int hashCode() {
        return left.hashCode() ^ right.hashCode();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals( final Object o )
    {
        if (!(o instanceof Pair)) {
            return false;
        } else {
            Pair<F,S> pairo = (Pair<F,S>) o;
            return this.left.equals(  pairo.getFirst() ) &&
                   this.right.equals( pairo.getSecond() );
        }
    }
    
    @Override
    public String toString()
    {
        return this.left.toString() + ", " +
               this.right.toString();
    }
}