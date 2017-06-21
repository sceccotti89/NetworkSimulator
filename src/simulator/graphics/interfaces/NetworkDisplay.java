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
	
	private long timer = 0;
	
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
	
	public void checkActivityPackets() {
		for (Packet packet: packets) {
			if (timer <= packet.getStartTime()) {
				// TODO SETTARE LA POSIZIONE DEL PACCHETTO IN RELAZIONE AL TIMER
				// ORA HO FATTO CHE RIPARTE SEMPRE DA CAPO MA E' SCORRETTO
				packet.setStartConditions( getNode( packet.getSourceID() ) );
			}
		}
	}
	
	public float getMaxY() {
		return zone.getMaxY();
	}
	
	public long getTimingSimulation() {
		return timer;
	}
	
	public void setTiminigSimulator( long val ) {
		timer = val;
	}
	
	public void startPositions() {
		for (Packet packet: packets) {
			packet.setStartConditions( getNode( packet.getSourceID() ) );
		}
	}
	
	public boolean isOperating() {
		return start;
	}
	
	public boolean isInPause() {
		return pause;
	}
	
	public boolean startAnimation( final int frames ) {
		if (!start) {
			start = true;
			for (Packet packet: packets) {
				packet.setActive( true );
				packet.setSpeed( frames );
			}
		}
		
		if (end) {
			startPositions();
			timer = 0;
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
		
		timer = 0;
		
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
					packet.update( gc, am.getFrames(), start, am.getFrames() );
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
			am.resetAllButtons();
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
			if (timer >= packet.getStartTime() && (start || pause) && packet.isActive()) {
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
