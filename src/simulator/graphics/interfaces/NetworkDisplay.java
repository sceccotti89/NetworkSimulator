package simulator.graphics.interfaces;

import java.util.ArrayList;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;

import simulator.elements.Node;
import simulator.elements.Packet;

public class NetworkDisplay
{
	private Rectangle zone;
	
	private boolean animate;
	
	private ArrayList<Node> nodes;
	private Packet packet;
	
	private int nextNode = 0;
	
	public NetworkDisplay( final GameContainer gc, final float startY, final float height, final ArrayList<Node> nodes, final Packet packet ){
		
		zone = new Rectangle( 0, startY, gc.getWidth(), height );
		
		animate = false;
		
		this.nodes = nodes;
		
		this.packet = packet;
	}
	
	public void startAnimation(){
		animate = true;
	}
	
	public void pauseAnimation(){
		animate = false;
	}
	
	public void stopAnimation(){
		animate = false;
	}
	
	public boolean getAnimate(){
		return animate;
	}
	
	public void update( GameContainer gc, AnimationManager am )
	{
		if (packet.getArea().intersects( nodes.get( nextNode ).getArea() )) {
			if (nodes.get( nextNode ).getIDTo() == nextNode) {
				animate = false;
				am.resetAllButtons();
				return;
			} else {
				nextNode++;
				packet.setColor( nodes.get( nextNode ).getColor() );
			}
		}
		
		packet.getArea().setX( packet.getArea().getX() + 5 );
	}
	
	public void render( GameContainer gc ){
		Graphics g = gc.getGraphics();
		
		g.setColor( Color.white );
		g.fill( zone );
		
		for (int i = 0; i < nodes.size() - 1; i++) {
			g.drawGradientLine( nodes.get( i ).getCenterX(), nodes.get( i ).getCenterY(), nodes.get( i + 1 ).getColor(), nodes.get( i + 1 ).getCenterX(), nodes.get( i + 1 ).getCenterY(), nodes.get( i + 1 ).getColor() );
		}
		
		for (Node node: nodes) {
			node.draw( g );
		}
		
		packet.draw( g );
	}
}
