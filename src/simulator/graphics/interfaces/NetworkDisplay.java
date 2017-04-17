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
	private ArrayList<Packet> packets;
	
	private Packet packet;
	
	private long startTime;
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
				startTime = (int) System.currentTimeMillis();
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
		
		return true;
	}
	
	public boolean stopAnimation()
	{
		animate = false;
		for (Packet packet: packets ) {
			packet.getArea().setX( nodes.get( packet.getIDFrom() ).getCenterX() );
			packet.setNextNode( 0 );
			packet.setColor( nodes.get( packet.getNextNode() ).getColor() );
			packet.setIsInNode( false );
			packet.setFinished( false );
		}
		
		return true;
	}
	
	public boolean getAnimate(){
		return animate;
	}
	
	private boolean checkAllPacket( AnimationManager am ) {
		for (Packet packet: packets) {
			if (!packet.getFinished()) {
				return false;
			}
		}

		animate = false;
		am.resetAllButtons();
		return true;
	}
	
	public void update( GameContainer gc, AnimationManager am )
	{
	    Graphics g = gc.getGraphics();
	    
	    // TODO CAMBIARE IL DISCORSO DEL TIMER
	    timer = (int) (System.currentTimeMillis() - startTime);
	    
		if (animate) {
			for (Packet packet: packets) {
				if (timer >= packet.getTime()) {
					if (packet.isActive()) {
						if (packet.getArea().intersects( nodes.get( packet.getIDTo() ).getArea() )) {
							packet.setActive( false );
						} else {
							packet.update( am.getFrames() );
						}
					}
				} else {
					break;
				}
			}
		}

		for (Packet packet: packets) {
			packet.checkMouse( gc, gc.getInput() );
		}
		
		for (Node node: nodes) {
			node.checkMouse( gc, gc.getInput() );
		}
	}
	
	public void render( GameContainer gc ) {
		Graphics g = gc.getGraphics();
		
		g.setColor( Color.white );
		g.fill( zone );
		
		for (Packet packet: packets) {
		    packet.draw( g );
		}
		
		for (Node node: nodes) {
			node.draw( g );
		}
	}
}
