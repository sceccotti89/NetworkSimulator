package simulator.elements;

import org.newdawn.slick.Color;

public class Package
{
	private String ID_from, ID_to;
	
	private Color color;
	
	public Package( String ID_from, String ID_to, Color color ){
		this.ID_from = ID_from;
		this.ID_to = ID_to;
		this.color = color;
	}
}
