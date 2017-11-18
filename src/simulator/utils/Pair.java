
package simulator.utils;

public class Pair<F,S>
{
    private F left;
    private S right;
    
    public Pair( F left, S right )
    {
        this.left = left;
        this.right = right;
    }
    
    public F getFirst() {
        return left;
    }
    
    public void setFirst( F value ) {
        this.left = value;
    }
    
    public S getSecond() {
        return right;
    }
    
    public void setSecond( S value ) {
        this.right = value;
    }
    
    @Override
    public int hashCode() {
        return left.hashCode() ^ right.hashCode();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals( Object o )
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
