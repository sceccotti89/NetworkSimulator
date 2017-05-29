package simulator.graphics.interfaces;

import java.util.List;

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
	
	private List<Node> nodes;
	private List<Packet> packets;
	
	private int timer;
	
	private boolean inPause;
	
	private boolean end;
	
	public NetworkDisplay( final GameContainer gc, final float startY, final float height, final List<Node> nodes, final List<Packet> packets )
	{
		zone = new Rectangle( 0, startY, gc.getWidth(), height );
		
		animate = false;
		
		this.nodes = nodes;
		
		this.packets = packets;
		
		inPause = false;
		
		end = false;
	}
	
	private Node getNode( final long nodeID ) {
    	for (Node node: nodes) {
    		if (node.getNodeID() == nodeID) {
    			return node;
    		}
    	}
    	
    	return null;
    }
	
	public void startPositions() {
		for (Packet packet: packets ) {
			packet.setStartConditions( getNode( packet.getSourceID() ) );
		}
	}
	
	public boolean startAnimation() {
		if (end) {
			startPositions();
			timer = 0;
		} else {
			if (inPause) {
				inPause = false;
			}		
		}
		
		animate = true;
		
		return true;
	}
	
	public boolean pauseAnimation() {
		if (!end) {
			animate = false;
			inPause = true;
		}
		
		return true;
	}
	
	public boolean stopAnimation() {
		startPositions();
		
		animate = false;
		timer = 0;
		
		return true;
	}
	
	public void update( GameContainer gc, AnimationManager am ) {
		end = true;

	    // TODO IL TIMER ORA E' CORRETTO?????
		if (animate) {
			timer = timer + am.getFrames();
		}
	
		for (Packet packet: packets) {
			if (timer >= packet.getStartTime()) {
				packet.update( gc, am.getFrames(), animate );
				if (packet.isActive()) {
					if (packet.getArea().intersects( getNode( packet.getDestID() ).getArea() )) {
						packet.setActive( false );
					}
				}
			}
		}
		
		// CONTROL PACKETS ENDING
		for (Packet packet: packets) {
			if (packet.isActive()) {
				end = false;
				break;
			}
		}
		
		if (end) {
			animate = false;
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
			node.drawLinks( g );
		}
		
		for (Node node: nodes) {
			node.drawNode( g );
		}
		
		for (Node node: nodes) {
			node.drawInfo( g );
		}
	}
}
