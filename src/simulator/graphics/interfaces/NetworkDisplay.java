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
	
	public NetworkDisplay( final GameContainer gc, final float startY, final float height ){
		zone = new Rectangle( 0, startY, gc.getWidth(), height );
		
		animate = false;
		
		nodes = new ArrayList<Node>();
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
	
	public void setElements( ArrayList<Node> nodes, Packet packet )
	{
		this.nodes = nodes;
		
		this.packet = packet;
	}
	
	public void update( GameContainer gc )
	{
		
	}
	
	public void render( GameContainer gc ){
		Graphics g = gc.getGraphics();
		
		g.setColor( Color.white );
		g.fill( zone );
		
		for (int i = 0; i < nodes.size() - 1; i++) {
			// TODO DISEGNARE LA LINEA CHE CONGIUNGE I 2 NODI
			g.drawGradientLine( nodes.get( i ).getX(), nodes.get( i ).getY(), nodes.get( i + 1 ).getColor(), nodes.get( i + 1 ).getX(), nodes.get( i + 1 ).getY(), nodes.get( i + 1 ).getColor() );
		}
		
		for (Node node: nodes) {
			node.draw( g );
		}
		
		packet.draw( g );
	}
}
