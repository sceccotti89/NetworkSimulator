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
	
	private Rectangle infos;
	private boolean drawInfo;
	
	private float widthInfo, heightInfo;
	
	private Packet packet;
	
	public NetworkDisplay( final GameContainer gc, final float startY, final float height, final ArrayList<Node> nodes, final ArrayList<Packet> packets )
	{
		zone = new Rectangle( 0, startY, gc.getWidth(), height );
		
		animate = false;
		
		this.nodes = nodes;
		
		this.packets = packets;
		
		widthInfo  = gc.getWidth()/7;
		heightInfo = gc.getHeight()/13;
		
		infos = new Rectangle( 0, 0, widthInfo, heightInfo );
		
		drawInfo = false;
	}
	
	public void startAnimation() {
		for (Packet packet: packets) {
			if (!packet.getFinished()) {
				animate = true;
				return;
			}
		}
	}
	
	public void pauseAnimation() {
		animate = false;
	}
	
	public void stopAnimation()
	{
		animate = false;
		for (Packet packet: packets ) {
			packet.getArea().setX( nodes.get( 0 ).getCenterX() );
			packet.setNextNode( 0 );
			packet.setColor( nodes.get( packet.getNextNode() ).getColor() );
			packet.setIsInNode( false );
			packet.setFinished( false );
		}
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
		if (animate) {
			for (Packet packet: packets) {
				if (packet.getArea().intersects( nodes.get( packet.getNextNode() ).getArea() )) {
					if (!packet.getIsInNode()) {
						packet.setIsInNode( true );
						if (packet.getIDTo() == nodes.get( packet.getNextNode() ).getIDFrom()) {
							packet.setFinished( true );
							if (checkAllPacket( am )) {
								return;
							}
						}
						packet.incNextNode();
					}
				} else {
					packet.setIsInNode( false );
					packet.setColor( nodes.get( packet.getNextNode() ).getColor() );
				}
				
				packet.getArea().setX( packet.getArea().getX() + am.getFrames() );
			}
		}

		for (Packet packet: packets) {
			if (packet.getArea().contains( gc.getInput().getMouseX(), gc.getInput().getMouseY() )) {
				drawInfo = true;
				infos.setLocation( gc.getInput().getMouseX() + gc.getWidth()/80, gc.getInput().getMouseY() - heightInfo );
				this.packet = packet;
			} else {
				drawInfo = false;
			}
		}
	}
	
	public void render( GameContainer gc ){
		Graphics g = gc.getGraphics();
		
		g.setColor( Color.white );
		g.fill( zone );
		
		for (int i = 0; i < nodes.size() - 1; i++) {
			g.drawGradientLine( nodes.get( i ).getCenterX(), nodes.get( i ).getCenterY(), nodes.get( i + 1 ).getColor(), nodes.get( i + 1 ).getCenterX(), nodes.get( i + 1 ).getCenterY(), nodes.get( i + 1 ).getColor() );
		}
		
		for (Packet packet: packets) {
			packet.draw( g );
		}
		
		for (Node node: nodes) {
			node.draw( g );
		}
		
		if (drawInfo) {
			g.setColor( Color.magenta );
			g.fill( infos );
			g.setColor( Color.black );
			g.draw( infos );
			
			g.drawString( "ID_From = " + packet.getIDFrom(), infos.getX(), infos.getY() );
			g.drawString( "ID_To = " + packet.getIDTo(), infos.getX(), infos.getY() + gc.getHeight()/30 );
		}
	}
}
