/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.animation_swing.interfaces;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import simulator.graphics.animation_swing.elements.TimeSlider;

public class AnimationManager extends JPanel implements ComponentListener, ActionListener, MouseListener, MouseMotionListener
{
    /** Generated Serial ID. */
    private static final long serialVersionUID = 7463830294028112320L;
    
    private NetworkDisplay nd;
    
    private List<AbstractButton> buttons;
    
    private boolean animationStarted = false;
    
    private ImageIcon start;
    private ImageIcon pause;
    private ImageIcon stop;
    
    private TimeSlider slider;
    
    public static int frames = 1;
    
    private static final String START = "START";
    private static final String PAUSE = "PAUSE";
    private static final String STOP  = "STOP";
    
    
    
    public AnimationManager( final NetworkDisplay nd, final float width, final float height )
    {
        setPreferredSize( new Dimension( (int) width, (int) height ) );
        addMouseListener( this );
        addMouseMotionListener( this );
        
        // TODO utilizzare posizioni assolute, forse mi conviene...
        //setLayout( null );
        setLayout( new GridBagLayout() );
        GridBagConstraints c = new GridBagConstraints();
        
        addComponentListener( this );
        
        this.nd = nd;
        nd.setAnimationManager( this );
        
        slider = new TimeSlider( this, TimeSlider.HORIZONTAL, 0, 0, 0 );
        slider.setBounds( 50, 30, width - 100, 10 );
        
        createButtonImages();
        
        buttons = new ArrayList<>();
        JButton button = new JButton();
        button.setName( START );
        button.addActionListener( this );
        button.setFocusable( false );
        button.setIcon( start );
        c.weightx = 0.5;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 2;
        buttons.add( button );
        add( button, c );
        //add( button );
        
        buttons.add( button );
        button = new JButton(); 
        button.setName( STOP );
        button.addActionListener( this );
        button.setFocusable( false );
        button.setIcon( stop );
        c.weightx = 0.5;
        c.weighty = 1;
        c.gridx = 10;
        c.gridy = 2;
        add( button, c );
        buttons.add( button );
    }
    
    private void createButtonImages()
    {
        final Color background = new JButton().getBackground();
        final int WIDTH  = 25;
        final int HEIGHT = 20;
        
        BufferedImage image = new BufferedImage( WIDTH + 1, HEIGHT + 1, BufferedImage.TYPE_INT_RGB );
        Graphics2D g = (Graphics2D) image.createGraphics();
        g.setColor( background );
        g.fillRect( 0, 0, WIDTH + 1, HEIGHT + 1 );
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        g.setColor( Color.GREEN );
        int[] xPoly = { 0, WIDTH + 1, 0 };
        int[] yPoly = { 0, HEIGHT/2, HEIGHT };
        Polygon poly = new Polygon( xPoly, yPoly, 3 );
        g.fill( poly );
        g.setColor( Color.BLACK );
        g.draw( poly );
        g.dispose();
        start = new ImageIcon( image );
        
        image = new BufferedImage( WIDTH + 1, HEIGHT + 1, BufferedImage.TYPE_INT_RGB );
        g = (Graphics2D) image.createGraphics();
        g.setColor( background );
        g.fillRect( 0, 0 , WIDTH + 1, HEIGHT + 1 );
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        g.setColor( Color.RED );
        g.fillRect( 0, 0, (int) (WIDTH*0.4), HEIGHT );
        g.fillRect( (int) (WIDTH*0.6), 0, (int) (WIDTH*0.4), HEIGHT );
        g.setColor( Color.BLACK );
        g.drawRect( 0, 0, (int) (WIDTH*0.4), HEIGHT );
        g.drawRect( (int) (WIDTH*0.6), 0, (int) (WIDTH*0.4), HEIGHT );
        g.dispose();
        pause = new ImageIcon( image );
        
        image = new BufferedImage( WIDTH + 1, HEIGHT + 1, BufferedImage.TYPE_INT_RGB );
        g = (Graphics2D) image.createGraphics();
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        g.setColor( Color.BLUE );
        g.fillRect( 0, 0, WIDTH, HEIGHT );
        g.setColor( Color.BLACK );
        g.drawRect( 0, 0, WIDTH, HEIGHT );
        g.dispose();
        stop = new ImageIcon( image );
    }
    
    public void setAnimationTime( final long time ) {
        slider.setMaximum( time );
    }
    
    public void update()
    {
        if (!nd.isPauseAnimation()) {
            long timer = nd.getTime();
            slider.setValue( (int) (slider.getRange() * (timer / slider.getMaximum())) );
            repaint();
        }
    }
    
    @Override
    public void componentResized( final ComponentEvent e )
    {
        slider.setBounds( 50, 20, getWidth() - 100, 10 );
        repaint();
    }
    
    @Override
    public void componentMoved( final ComponentEvent e ) {}
    @Override
    public void componentShown( final ComponentEvent e ) {}
    @Override
    public void componentHidden( final ComponentEvent e ) {}
    
    public void reset()
    {
        slider.setValue( 0 );
        buttons.get( 0 ).setName( START );
        buttons.get( 0 ).setIcon( start );
        //buttons.get( 0 ).setText( START );
        animationStarted = false;
        repaint();
    }
    
    private void startAnimation( final AbstractButton button )
    {
        nd.startAnimation();
        button.setName( PAUSE );
        button.setIcon( pause );
    }
    
    private void pauseAnimation( final AbstractButton button )
    {
        nd.pauseAnimation();
        button.setName( START );
        button.setIcon( start );
    }
    
    @Override
    public void actionPerformed( final ActionEvent e )
    {
        AbstractButton button = (AbstractButton) e.getSource();
        switch (button.getName()) {
            case( START ):
                startAnimation( button );
                animationStarted = true;
                //button.setText( PAUSE );
                break;
            case( PAUSE ):
                pauseAnimation( button );
                animationStarted = false;
                //button.setText( START );
                break;
            case( STOP ):
                nd.stopAnimation();
                reset();
                break;
        }
    }
    
    @Override
    public void mousePressed( final MouseEvent e )
    {
        if (slider.mousePressed( e )) {
            pauseAnimation( buttons.get( 0 ) );
            nd.setTime( (long) slider.getValue() );
        }
        
        repaint();
    }
    
    @Override
    public void mouseReleased( final MouseEvent e )
    {
        if (slider.isPressed() && animationStarted) {
            startAnimation( buttons.get( 0 ) );
        }
        slider.mouseReleased();
    }
    
    @Override
    public void mouseClicked( final MouseEvent e ) {}
    @Override
    public void mouseEntered( final MouseEvent e ) {}
    @Override
    public void mouseExited( final MouseEvent e ) {}
    
    @Override
    public void mouseMoved( final MouseEvent e )
    {
        slider.mouseMoved( e );
        repaint();
    }
    
    @Override
    public void mouseDragged( final MouseEvent e )
    {
        if (slider.mouseMoved( e )) {
            nd.setTime( (long) slider.getValue() );
        }
        
        repaint();
    }
    
    protected void paintComponent( final Graphics g )
    {
        super.paintComponent( g );
        
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_ON );
        
        slider.draw( g2d );
        g2d.dispose();
    }
}