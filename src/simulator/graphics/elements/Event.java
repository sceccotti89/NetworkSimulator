package simulator.graphics.elements;

import org.newdawn.slick.Input;

public class Event
{
	private boolean consumed;
	
	private Input input;
	
	public void setConsumed( final boolean val ) {
		consumed = val;
	}
	
	public boolean isConsumed() {
		return consumed;
	}
	
	public void setInput( final Input input ) {
		this.input = input;
	}
	
	public Input getInput() {
		return input;
	}
}
