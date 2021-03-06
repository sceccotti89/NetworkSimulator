
package simulator.graphics.animation_swing.elements;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

public class Node
{
    private long nodeID;
    private Color color;
    
    private Rectangle node;
    final private int ray = 25;
    
    final private long delay;
    final private String name;
    
    
    
    public Node( double x, double y, long nodeID, String name, long delay, Color color )
    {
        this.nodeID = nodeID;
        this.color = color;
        
        this.delay = delay;
        this.name = name;
        
        node = new Rectangle( (int) x, (int) y, ray * 2, ray * 2 );
    }
    
    public long getID() {
        return nodeID;
    }
    
    public Color getColor() {
        return color;
    }
    
    public int getCenterX() {
        return (int) node.getCenterX();
    }
    
    public int getCenterY() {
        return (int) node.getCenterY();
    }
    
    public Rectangle getArea() {
        return node;
    }
    
    public float calculateAngle( Node dest ) {
        return calculateAngle( getCenterX(), getCenterY(),
                               dest.getCenterX(), dest.getCenterY() );
    }
    
    public float calculateAngle( double x1, double y1, float x2, float y2 )
    {
        double m = (y2 - y1)/(x2 - x1);
        
        if (y1 == y2) {
            if (x1 <= x2) return 0;
            else return  -180;
        }
        
        if (y1 > y2) {
            if (x1 < x2) {
                return  (float) (90 - (Math.PI/2 - Math.atan( m ))*180/Math.PI);
            } else if (x1 > x2) {
                return (float) (270 - (Math.PI/2 - Math.atan( m ))*180/Math.PI);
            }
        } else {
            if (x1 < x2) {
                return (float) (90 - (Math.PI/2 - Math.atan( m ))*180/Math.PI);
            } else if (x1 > x2) {
                return (float) (270 - (Math.PI/2 - Math.atan( m ))*180/Math.PI);
            }
        }
        
        return 0;
    }
    
    public float getRay() {
        return ray;
    }
    
    public long getDelay() {
        return delay;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean checkCollision( int mouseX, int mouseY ) {
    	return node.contains( mouseX, mouseY );
    }
    
    public Node clone( Node dest, int width, int height ) {
    	Node tmp = new Node( node.getCenterX(), node.getCenterY(), nodeID, name, delay, color );
		return tmp;
    }
    
    public void update()
    {
    	// TODO inserire la gestione dello spostamento di un nodo
        
    }
    
    public void draw( Graphics2D g )
    {
        g.setColor( color );
        g.fillOval( node.x, node.y, node.width, node.height );
        
        g.setColor( Color.black );
        g.drawOval( node.x, node.y, node.width, node.height );
        
        g.setColor( Color.white );
        Rectangle2D bounds = g.getFontMetrics().getStringBounds( nodeID + "", g );
        g.drawString( nodeID + "",
                     (int) (node.getCenterX() - bounds.getWidth()/2),
                     (int) (node.getCenterY() + bounds.getHeight()/2) );
    }
}
