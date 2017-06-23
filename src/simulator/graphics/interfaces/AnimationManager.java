
package simulator.graphics.interfaces;

import java.util.ArrayList;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import org.newdawn.slick.Input;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Rectangle;

import simulator.graphics.dataButton.ImageButton;

public class AnimationManager implements AnimationInterface
{
    private float width, height;
    
    private Rectangle speed, showFrame;
    
    private ArrayList<ImageButton> buttons;
    private ImageButton start, stop, pause, plus, minus;
    
    private int mouseX, mouseY;
    
    public static int frames = 1;
    
    private static final int limit = Integer.MAX_VALUE;
    
    private final String START = "Start", STOP = "Stop", PAUSE = "Pause", PLUS = "Plus", MINUS = "Minus";
    
    private boolean mouseDown;
    
    private String frameLenght = "" + frames;
    
    public AnimationManager( final GameContainer gc, final float startY, final float widthM, final float heightM ) throws SlickException
    {
        height = heightM*10/95;
        width  = widthM*10/53;
        float widthFrame = widthM/10;
        
        buttons = new ArrayList<ImageButton>();

        start = new ImageButton( 0, startY, width, height, START, Color.gray, 0, gc, new Image( "./data/Image/Start.png" ), widthM/20, heightM/20 );
        pause = new ImageButton( start.getMaxX(), startY, width, height, PAUSE, Color.gray, 1, gc, new Image( "./data/Image/Pause.png" ), widthM/20, heightM/20 );
        stop  = new ImageButton( pause.getMaxX(), startY, width, height, STOP, Color.gray, 2, gc, new Image( "./data/Image/Stop.png" ), widthM/20, heightM/20 );
        minus = new ImageButton( stop.getMaxX() + widthM/15, startY + height/2 - heightM/40, widthM/20, heightM/20, MINUS, Color.yellow, 3, gc, new Image( "./data/Image/Minus.png" ), widthM/45, heightM/70 );
        plus  = new ImageButton( stop.getMaxX() + widthM/4, startY + height/2 - heightM/40, widthM/20, heightM/20, PLUS, Color.yellow, 4, gc, new Image( "./data/Image/Plus.png" ), widthM/40, heightM/30 );

        showFrame = new Rectangle( minus.getMaxX() + (plus.getX() - minus.getMaxX())/2 - widthFrame/2, minus.getY(), widthFrame, minus.getAlt());
        speed     = new Rectangle( pause.getMaxX(), startY, gc.getWidth() - pause.getMaxX(), height );
        
        buttons.add( start );
        buttons.add( stop );
        buttons.add( pause );
        buttons.add( plus );
        buttons.add( minus );
    }
    
    @Override
    public void update( final int delta, final Input input, final boolean leftMouse, OptionBar ob, AnimationManager am, TimeAnimation ta, NetworkDisplay nd )
    {        
        mouseX = input.getMouseX();
        mouseY = input.getMouseY();
        
        if (leftMouse && !mouseDown) {
            mouseDown = true;
            
            for (ImageButton button : buttons) {
                if (button.checkClick( mouseX, mouseY ) && !button.isPressed()) {
                    button.setPressed( true );
                }
            }
        } else if (!leftMouse && mouseDown) {
            mouseDown = false;
            
            boolean buttonFounded = false;
            for (ImageButton button: buttons) {
                if (button.checkClick( mouseX, mouseY )) {
                    buttonFounded = true;
                    if (button.getName().equals( PLUS )) {
                        frames = Math.min( limit, frames + 1 );
                        nd.setPacketSpeed( frames );
                        button.setPressed( false );
                    } else if (button.getName().equals( MINUS )) {
                        frames = Math.max( 1, frames - 1 );
                        nd.setPacketSpeed( frames );
                        button.setPressed( false );
                    } else if (button.getName().equals( START )) {
                        nd.startAnimation();
                        ob.resetAllButtons();
                        button.setPressed( true );
                        resetButtons( button );
                    } else if (button.getName().equals( PAUSE )) {
                    	if (nd.isInExecution()) {
                    		nd.pauseAnimation();
                            ob.resetAllButtons();
                            button.setPressed( true );
                            resetButtons( button );
                    	} else {
                    		button.setPressed( false );
                    	}
                    } else if (button.getName().equals( STOP )) {
                        nd.stopAnimation();
                        ob.resetAllButtons();
                        resetButtons( null );
                    }
                }
            }
            
            if (!buttonFounded){
                for (ImageButton button: buttons) {
                    if (button.isPressed()) {
                    	if (button.getName().equals( START )) {
                    		if (!nd.isInExecution()) {
                    			button.setPressed( false );
                    		}
                    	} else if (button.getName().equals( PAUSE )) {
                    		if (!nd.isInPause()) {
                    			button.setPressed( false );
                    		}
                    	} else {
                    		button.setPressed( false );
                    	}
                    }
                }
            }
        }
    }
    
    public float getMaxY() {
        return start.getMaxY();
    }
    
    @Override
    public void render( final GameContainer gc )
    {        
        Graphics g = gc.getGraphics();
        
        g.setColor( Color.gray );
        g.fill( speed );
        
        for (ImageButton button: buttons) {
            button.draw( g );
        }
        
        g.setColor( Color.black );
        g.draw( showFrame );
        
        frameLenght = "" + frames;
        int fWidth = g.getFont().getWidth( frameLenght ), fHeight = g.getFont().getHeight( frameLenght );
        g.drawString( frameLenght, showFrame.getCenterX() - fWidth/2, showFrame.getCenterY() - fHeight/2 );
    }
    
    public void resetAllButtons() {
        for (ImageButton button: buttons) {
            if (button.isPressed()) {
                button.setPressed( false );
            }
        }
    }
    
    private void resetButtons( ImageButton button ) {
        for (ImageButton imButton: buttons) {
            if (imButton != button) {
                imButton.setPressed( false );
            }
        }
    }
    
    public int getFrames() {
        return frames;
    }
}
