package simulator.graphics.elements;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Input;
import org.newdawn.slick.geom.Rectangle;

public class Packet implements Comparable<Packet>
{
    private Color color;
    
    private Rectangle area;
    
    private long startTime, endTime;
    
    private boolean active;
    
    private float speedX;
    
    private float angle;
    
    private boolean drawInfo;
    
    private int width, height;
    
    private float linkLenght;
    
    private float distance;
    
    private long timer;
    
    private Node source, dest;
    
    Info info;
    
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
    
    public float getSpeedX() {
        return speedX;
    }
    
    public void setLinkLenght() {
        linkLenght = source.getLinkLenght( dest.getNodeID() ) - 2*source.getRay();
    }
    
    public float getLinkLenght() {
        return linkLenght;
    }
    
    public void setPosition( final long time )
    {
        active = true;
        timer = Math.max( 0, time - startTime );
        if (timer == 0) {
            distance = 0;
        } else {
            distance = (float) ((((double) (time - startTime)) / ((double) (endTime - startTime))) * linkLenght);
        }
        
        area.setX( source.getCenterX() + source.getRay() + distance );
    }
    
    public void setAngle() {
        angle = source.getAngle( dest.getNodeID() );
    }
    
    public void setSpeed( final int frames ) {
        speedX = (linkLenght / (endTime - startTime)) * frames;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive( boolean val ) {
        active = val;
    }
    
    public void setStartTime( int val ) {
        startTime = val;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setEndTime( int val ) {
        endTime = val;
    }
    
    public long getEndTime() {
        return endTime;
    }
    
    public void setSourceNode( final Node source ) {
        this.source = source;
    }
    
    public void setDestNode( final Node dest ) {
        this.dest = dest;
    }
    
    public Rectangle getArea(){
        return area;
    }
    
    public void setColor( Color color ){
        this.color = color;
    }
    
    public Node getSourceNode(){
        return source;
    }
    
    public Node getDestNode(){
        return dest;
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
    
    public void update( final GameContainer gc, final float time )
    {
        if (active) {
            if (time >= startTime) {
                if (time >= endTime) {
                    setActive( false );
                    return;
                }
                
                distance = distance + speedX;
                area.setLocation( area.getX() + speedX, area.getY() );
                
                // TODO DA LAVORARCI
                if (gc.getInput().isMouseButtonDown( Input.MOUSE_RIGHT_BUTTON )) {
                	info.setAttributes( gc.getGraphics(), infos );
                	drawInfo = true;
                }
            }
        }
    }
    
    public void draw( final long time, final Graphics g )
    {
    	float offset;
    	
        if (time < startTime || time > endTime)
            return;
        
        g.rotate( source.getCenterX(), source.getCenterY(), angle );
        
        if (drawInfo) {
        	offset = width/80;
        	info.render( g, area.getMaxX() + offset, area.getMaxY() + offset, angle );
        }
        
        offset = height/30;
        if (angle == -180) offset *= -1;
        
        area.setY( area.getY() + offset );
        g.setColor( color );
        g.fill( area );
        g.resetTransform();
        area.setY( area.getY() - offset );
    }

    @Override
    public int compareTo( Packet packet ) {
        if (startTime < packet.startTime) return -1;
        if (startTime > packet.startTime) return 1;
        return 0;
    }
}
