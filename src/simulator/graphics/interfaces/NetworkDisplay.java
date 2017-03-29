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
	
	private Rectangle infos;
	private boolean drawInfo;
	
	private boolean isInNode;
	
	private float widthInfo, heightInfo;
	
	public NetworkDisplay( final GameContainer gc, final float startY, final float height, final ArrayList<Node> nodes, final Packet packet ){
		
		zone = new Rectangle( 0, startY, gc.getWidth(), height );
		
		animate = false;
		
		this.nodes = nodes;
		
		this.packet = packet;
		
		widthInfo  = gc.getWidth()/7;
		heightInfo = gc.getHeight()/13;
		
		infos = new Rectangle( 0, 0, widthInfo, heightInfo );
		
		drawInfo = false;
		
		isInNode = false;
	}
	
	public void startAnimation(){
		animate = true;
	}
	
	public void pauseAnimation(){
		animate = false;
	}
	
	public void stopAnimation(){
		animate = false;
		isInNode = false;
		packet.getArea().setX( nodes.get( 0 ).getCenterX() );
		packet.setColor( nodes.get( 0 ).getColor() );
		nextNode = 0;
	}
	
	public boolean getAnimate(){
		return animate;
	}
	
	public void update( GameContainer gc, AnimationManager am )
	{
		if (animate) {		
			if (packet.getArea().intersects( nodes.get( nextNode ).getArea() )) {
				if (!isInNode) {
					isInNode = true;
					nextNode++;
					if (nodes.get( nextNode ).getIDTo() == nodes.get( nextNode ).getIDFrom()) {
						animate = false;
						am.resetAllButtons();
						return;
					}
					packet.setColor( nodes.get( nextNode ).getColor() );
				}
			} else {
				isInNode = false;
			}
			
			packet.getArea().setX( packet.getArea().getX() + am.getFrames() );
		}
		
		if (packet.getArea().contains( gc.getInput().getMouseX(), gc.getInput().getMouseY() )) {
			drawInfo = true;
			infos.setLocation( gc.getInput().getMouseX() + gc.getWidth()/80, gc.getInput().getMouseY() - heightInfo );
		} else {
			drawInfo = false;
		}
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
