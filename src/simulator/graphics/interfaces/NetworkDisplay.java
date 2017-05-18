package simulator.graphics.interfaces;

import java.util.ArrayList;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.geom.Rectangle;

import simulator.graphics.elements.Node;
import simulator.graphics.elements.Packet;

public class NetworkDisplay
{
	private Rectangle zone;
	
	private boolean animate;
	
	private ArrayList<Node> nodes;
	private ArrayList<Packet> packets;
	
	private int timer;
	
	private boolean inPause;
	
	public NetworkDisplay( final GameContainer gc, final float startY, final float height, final ArrayList<Node> nodes, final ArrayList<Packet> packets )
	{
		zone = new Rectangle( 0, startY, gc.getWidth(), height );
		
		animate = false;
		
		this.nodes = nodes;
		
		this.packets = packets;
		
		inPause = false;
	}
	
	public boolean startAnimation() {
		for (Packet packet: packets) {
			if (!packet.getFinished()) {
				if (inPause) {
					inPause = false;
				} else {
					timer = 0;
				}
				animate = true;
				return true;
			}
		}
		
		return true;
	}
	
	public boolean pauseAnimation() {
		animate = false;
		inPause = true;
		
		return true;
	}
	
	public boolean stopAnimation( GameContainer gc )
	{
		for (Packet packet: packets ) {
			packet.getArea().setLocation( nodes.get( packet.getIDFrom() ).getCenterX(), nodes.get( packet.getIDFrom() ).getCenterY() + gc.getWidth()/50 );
			packet.setFinished( false );
			packet.setActive( true );
		}
		
		animate = false;
		timer = 0;
		
		return true;
	}
	
	public void update( GameContainer gc, AnimationManager am )
	{
	    // TODO CAMBIARE IL DISCORSO DEL TIMER
		
		if (animate) {
			timer = timer + am.getFrames();	
		}
	
		for (Packet packet: packets) {
			if (timer >= packet.getTime()) {
				packet.update( am.getFrames(), gc, animate );
				if (packet.isActive()) {
					if (packet.getArea().intersects( nodes.get( packet.getIDTo() ).getArea() )) {
						packet.setActive( false );
					}
				}
			}
		}
		
		for (Node node: nodes) {
			node.update( gc );
		}
	}
	
	public void render( GameContainer gc ) {
		Graphics g = gc.getGraphics();
		
		g.setColor( Color.white );
		g.fill( zone );
		
		for (Packet packet: packets) {
		    packet.draw( g );
		}
		
		// TODO PROVVISORIO, POI FARO' COME HA DETTO STEFANO
		for (Node node: nodes) {
			node.drawNode( g );
		}
		
		for (Node node: nodes) {
			node.drawInfo( g );
		}
	}
}
