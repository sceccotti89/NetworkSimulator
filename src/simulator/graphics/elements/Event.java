package simulator.graphics.elements;

import org.newdawn.slick.Input;

public class Event
{
	private boolean consumed = false;
	
	private Input input;
	
	public void setConsumed( boolean val ) {
		consumed = val;
	}
	
	public boolean isConsumed() {
		return consumed;
	}
	
	public void setInput( Input input ) {
		this.input = input;
	}
	
	public Input getInput() {
		return input;
	}
	
	public boolean isMouseButtonDown() {
		return input.isMouseButtonDown( Input.MOUSE_LEFT_BUTTON ) || input.isMouseButtonDown( Input.MOUSE_RIGHT_BUTTON );
	}
}
