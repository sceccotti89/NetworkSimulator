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
	
	private long startTime;
	private int timer;
	
	private boolean inPause;
	
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
							System.out.println( "ANGOLO = " + nodes.get( packet.getIndexRotation() ).getAngle() );
							System.out.println( "VAL_X = " + (packet.getArea().getX() + am.getFrames() * (float) Math.sin( nodes.get( packet.getIndexRotation() ).getAngle() )) );
							
							//g.rotate( nodes.get( packet.getIndexRotation() ).getCenterX(), nodes.get( packet.getIndexRotation() ).getCenterY(), nodes.get( packet.getIndexRotation() ).getAngle() );
							
							packet.getArea().setX( (float) (packet.getArea().getX() + am.getFrames() * Math.sin( nodes.get( packet.getIndexRotation() ).getAngle() ) ) );
							packet.getArea().setY( (float) (packet.getArea().getY() + am.getFrames() * Math.cos( nodes.get( packet.getIndexRotation() ).getAngle() ) ) );
							
							//packet.getArea().setX( packet.getArea().getX() + am.getFrames() );
							//g.resetTransform();
						}
					}
				} else {
					break;
				}
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
	
	public void render( GameContainer gc ) {
		Graphics g = gc.getGraphics();
		
		g.setColor( Color.white );
		g.fill( zone );
		
		for (Packet packet: packets) {
			//System.out.println( "INDEX = " + packet.getIndexRotation() );
			//System.out.println( "ROTATION = " + nodes.get( packet.getIndexRotation() ).getAngle() );
		    // TODO RUOTARE IL PACCHETTO E POI RIPORTARLO ALLE CONDIZIONI INIZIALI
		    //g.rotate( nodes.get( packet.getIndexRotation() ).getCenterX(), nodes.get( packet.getIndexRotation() ).getCenterY(), nodes.get( packet.getIndexRotation() ).getAngle() );
			packet.draw( g );
			//g.resetTransform();
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
