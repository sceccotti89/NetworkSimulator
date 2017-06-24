package simulator.graphics.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Point;
import org.newdawn.slick.geom.Rectangle;

public class Packet implements Comparable<Packet>
{
    private Color color;
    
    private Rectangle area, areaRotated;
    
    private long startTime, endTime;
    
    private boolean active;
    
    private float speed;
    
    private float angle;
    
    private int width, height;
    
    private float linkLenght, distance;
    
    private long timer;
    
    private Node source, dest;
    
    private Info info;
    private boolean drawInfo;
    private String infos;
    
    public Packet( final Node source, final Node dest,
                   final Color color,
                   final long startTime, final long endTime,
                   final int width, final int height ) {
        
        this.source = source;
        this.dest = dest;
        this.color = color;
        this.height = height;
        this.width = width;
        
        this.startTime = startTime;
        this.endTime = endTime;
        
        infos =   "startTime = " + startTime + "\n"
        		+ "endTime = " + endTime + "\n"
        		+ "source = " + source.getNodeID() + "\n"
        		+ "dest = " + dest.getNodeID();
        
        info = new Info( Color.magenta, infos );
        
        drawInfo = false;
        
        init();
    }
    
    public void init()
    {
        area = new Rectangle( source.getCenterX() + source.getRay(), source.getCenterY() - height/120, width/80, height/60 );
        distance = 0;
        timer = 0;
        active = true;
    }
    
    public float getSpeed() {
        return speed;
    }
    
    public void setLinkLenght() {
        linkLenght = source.getLinkLenght( dest.getNodeID() ) - 2*source.getRay();
    }
    
    public void setPosition( final long time )
    {
        active = true;
        timer = Math.max( 0, time - startTime );
        if (timer == 0) {
            distance = 0;
        } else {
            distance = (float) ((((double) time - startTime) / (endTime - startTime)) * linkLenght);
        }
        
        area.setX( source.getCenterX() + source.getRay() + distance );
    }
    
    public void setAngle() {
        angle = source.getAngle( dest.getNodeID() );
    }
    
    public void setSpeed( final int frames ) {
        speed = (linkLenght / (endTime - startTime)) * frames;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive( boolean val ) {
        active = val;
    }
    
    public long getStartTime() {
        return startTime;
    }

    @Override
    public int compareTo( Packet packet ) {
        if (startTime < packet.startTime) return -1;
        if (startTime > packet.startTime) return 1;
        return 0;
    }
    
    /*
    // ruota un punto intorno all'origine degli assi di un angolo teta
    member this.Rotate( x:float32, y:float32, teta:float32 ) =
        // ottiene i radianti dell'angolo di rotazione espresso in sessagesimali
        let angle = teta * single System.Math.PI / 180.f
        // calcola le nuove coordinate
        let X = x * cos( angle ) - y * sin( angle )
        let Y = x * sin( angle ) + y * cos( angle )
    */
    
    private Point worldToView( final float x, final float y, float angle ) {
    	angle = (float) (angle * Math.PI/180.f);
    	System.out.println( "X = " + x );
    	return new Point( (float) (x * Math.cos( angle ) - y * Math.sin( angle )), (float) (x * Math.sin( angle ) + y * Math.cos( angle )) );
    }
    
    public void update( final GameContainer gc, final float time )
    {
    	float mouseX = gc.getInput().getMouseX();
    	float mouseY = gc.getInput().getMouseY();
    	
        if (true) {
            if (time >= startTime) {
                if (time >= endTime) {
                    setActive( false );
                    return;
                }
                
                distance = distance + speed;
                area.setLocation( area.getX() + speed, area.getY() );
                
                Point p = worldToView( area.getX() - source.getCenterX(), (area.getY() - source.getCenterY()), angle );
            	System.out.println( "X = " + p.getX() );
                areaRotated = new Rectangle( p.getX() + source.getCenterX(), p.getY() + source.getCenterY(), area.getWidth(), area.getHeight() );

            	System.out.println( "AREA.X = " + area.getX() + ", AREA.Y = " + area.getY() );
            	System.out.println( "ROT.X = " + areaRotated.getX() + ", ROT.Y = " + areaRotated.getY() );
            	
                if (areaRotated.contains( mouseX, mouseY )) {
                	info.setAttributes( gc.getGraphics(), infos );
                	float offset = width/80;
                	info.setPosition( mouseX + offset, mouseY + offset );
                	drawInfo = true;
                }
            }
        }
    }
    
    public void render( final long time, final Graphics g )
    {
    	float offset;
    	
        if (time < startTime || time > endTime)
            return;
        
        g.rotate( source.getCenterX(), source.getCenterY(), angle );
        
        if (drawInfo) {
        	offset = width/80;
        	info.render( g, area.getMaxX() + offset, area.getMaxY() + offset, angle );
        	g.resetTransform();
            g.rotate( source.getCenterX(), source.getCenterY(), angle );
        }
        
        offset = height/30;
        if (angle == -180) offset *= -1;
        
        area.setY( area.getY() + offset );
        g.setColor( color );
        g.fill( area );
        g.resetTransform();
        area.setY( area.getY() - offset );
        
        g.fill( areaRotated );
    }
}