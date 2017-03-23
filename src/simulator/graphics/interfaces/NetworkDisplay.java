package simulator.graphics.interfaces;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;

public class NetworkDisplay
{
	private Rectangle zone;
	
	public NetworkDisplay( final GameContainer gc, final float startY, final float height ){
		zone = new Rectangle( 0, startY, gc.getWidth(), height );
	}
	
	public void startAnimation(){
		
	}
	
	public void pauseAnimation(){
		
	}
	
	public void stopAnimation(){
		
	}
	
	public void render( GameContainer gc ){
		Graphics g = gc.getGraphics();
		
		g.setColor( Color.white );
		g.fill( zone );
	}
}
