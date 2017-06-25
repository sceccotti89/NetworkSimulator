package simulator.graphics.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.geom.Rectangle;

public class Link
{
    private Color color = Color.black;
    
    private float angle;
    
    private double bandwidth;
    
    private int delay;
    
    private Rectangle area;
    
    Info info;
    private boolean drawInfo;
    private String infos;
    
    private final float offset;
    
    private float lenght;
    
    private long fromID, destID;
    
    public Link( long fromID, long destID, float x1, float y1, float x2, float y2, float angle, int width, int height ) {
        
        this.fromID = fromID;
        this.destID = destID;
        
        this.angle = angle;

        offset = width/100;
        
        lenght = calculateLenght( x1, y1, x2, y2 );
        
        area = new Rectangle( x1, y1 - offset, calculateLenght( x1, y1, x2, y2 ), offset * 2 );
        
        infos =   "bandwidth = " + bandwidth + "Mb/s, "
        		+ "delay = " + delay + "ms";
        
        info = new Info( Color.lightGray, infos );
    }
    
    public void update( final GameContainer gc ) {
    	if (gc.getInput().isMouseButtonDown( Input.MOUSE_RIGHT_BUTTON )) {
        	info.setAttributes( gc.getGraphics(), infos );
        	drawInfo = true;
        }
    }
    
    public long getFromID() {
        return fromID;
    }
    
    public long getDestID() {
        return destID;
    }
    
    public float calculateLenght( float x1, float y1, float x2, float y2 ) {
        return (float) Math.sqrt( (x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) );
    }
    
    public float getLenght() {
        return lenght;
    }
    
    public float getAngle() {
        return angle;
    }
    
    public void render( Graphics g ) {
        g.rotate( area.getX(), area.getY() + offset, angle );
        
        if (drawInfo) {
        	info.render( g );
        }
        
        g.setColor( color );
        g.draw( area );
        g.resetTransform();
    }
}