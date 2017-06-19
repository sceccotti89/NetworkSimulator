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
	
	private List<Node> nodes;
	private List<Packet> packets;
	
	private long timer = 999999999999999990L;
	
	private boolean end;
	
	private boolean start, pause;
	
	public NetworkDisplay( final float width, final float height, final float startY, final List<Node> nodes, final List<Packet> packets )
	{
		zone = new Rectangle( 0, startY, width, height );
		
		this.nodes = nodes;
		
		this.packets = packets;
		
		end = false;
		
		start = false;
		pause = false;
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
		for (Packet packet: packets) {
			packet.setStartConditions( getNode( packet.getSourceID() ) );
		}
	}
	
	public boolean startAnimation() {
		if (!start) {
			start = true;
			for (Packet packet: packets) {
				packet.setActive( true );
			}
		}
		
		if (end) {
			startPositions();
			timer = 999999999999999990L;
		} else {
			if (pause) {
				pause = false;
			}		
		}
		
		return true;
	}
	
	public boolean pauseAnimation() {
		if (!end) {
			pause = true;
			start = false;
		}
		
		return true;
	}
	
	public boolean stopAnimation() {
		startPositions();
		
		start = false;
		pause = false;
		
		timer = 999999999999999990L;
		
		return true;
	}
	
	public void update( GameContainer gc, AnimationManager am ) {
		end = true;

	    // TODO IL TIMER ORA E' CORRETTO?????
		if (start) {
			timer = timer + am.getFrames();
		}
	
		for (Packet packet: packets) {
			if (packet.isActive()) {
				if (timer > packet.getEndTime()) {
					packet.setActive( false );
				} else if (timer >= packet.getStartTime()) {
					packet.update( gc, am.getFrames(), start );
					if (packet.isActive()) {
						if (packet.linkCrossed()) {
							packet.setActive( false );
						}
					}
				}
			}
		}
		
		// control packets ending
		for (Packet packet: packets) {
			if (packet.isActive()) {
				end = false;
				break;
			}
		}
		
		if (end) {
			start = false;
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
			if (timer >= packet.getStartTime() && start && packet.isActive()) {
				packet.draw( g );
			}
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
