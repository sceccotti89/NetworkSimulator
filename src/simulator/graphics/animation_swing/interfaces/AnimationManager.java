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
    
    private AbstractButton start_btn;
    private AbstractButton stop_btn;
    
    private boolean animationStarted = false;
    
    private ImageIcon start;
    private ImageIcon pause;
    private ImageIcon stop;
    
    private TimeSlider slider;
    
    public static int frames = 1;
    
    private static final int SLIDER_OFF_X = 50;
    
    private static final String START = "START";
    private static final String PAUSE = "PAUSE";
    private static final String STOP  = "STOP";
    
    
    
    public AnimationManager( NetworkDisplay nd, float width, float height )
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
        slider.setBounds( SLIDER_OFF_X, 20, width - SLIDER_OFF_X * 2, 10 );
        
        createButtonImages();
        
        start_btn = new JButton();
        start_btn.setName( START );
        start_btn.addActionListener( this );
        start_btn.setFocusable( false );
        start_btn.setIcon( start );
        c.weightx = 0.5;
        c.weighty = 1;
        c.gridx = 0;
        c.gridy = 2;
        add( start_btn, c );
        //add( button );
        
        stop_btn = new JButton(); 
        stop_btn.setName( STOP );
        stop_btn.addActionListener( this );
        stop_btn.setFocusable( false );
        stop_btn.setIcon( stop );
        c.weightx = 0.5;
        c.weighty = 1;
        c.gridx = 10;
        c.gridy = 2;
        add( stop_btn, c );
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
    
    public void setAnimationTime( long time ) {
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
    public void componentResized( ComponentEvent e )
    {
        slider.setBounds( SLIDER_OFF_X, 20, getWidth() - SLIDER_OFF_X * 2, 10 );
        repaint();
    }
    
    @Override
    public void componentMoved( ComponentEvent e ) {}
    @Override
    public void componentShown( ComponentEvent e ) {}
    @Override
    public void componentHidden( ComponentEvent e ) {}
    
    public void reset()
    {
        slider.setValue( 0 );
        start_btn.setName( START );
        start_btn.setIcon( start );
        animationStarted = false;
        repaint();
    }
    
    private void startAnimation()
    {
        nd.startAnimation();
        start_btn.setName( PAUSE );
        start_btn.setIcon( pause );
    }
    
    private void pauseAnimation()
    {
        nd.pauseAnimation();
        start_btn.setName( START );
        start_btn.setIcon( start );
    }
    
    @Override
    public void actionPerformed( ActionEvent e )
    {
        AbstractButton button = (AbstractButton) e.getSource();
        switch (button.getName()) {
            case( START ):
                startAnimation();
                animationStarted = true;
                break;
            case( PAUSE ):
                pauseAnimation();
                animationStarted = false;
                break;
            case( STOP ):
                nd.stopAnimation();
                reset();
                break;
        }
    }
    
    @Override
    public void mousePressed( MouseEvent e )
    {
        if (slider.mousePressed( e )) {
            pauseAnimation();
            nd.setTime( (long) slider.getValue() );
        }
        repaint();
    }
    
    @Override
    public void mouseReleased( MouseEvent e )
    {
        if (slider.isPressed() && animationStarted) {
            startAnimation();
        }
        slider.mouseReleased();
    }
    
    @Override
    public void mouseClicked( MouseEvent e ) {}
    @Override
    public void mouseEntered( MouseEvent e ) {}
    @Override
    public void mouseExited( MouseEvent e ) {}
    
    @Override
    public void mouseMoved( MouseEvent e )
    {
        slider.mouseMoved( e );
        repaint();
    }
    
    @Override
    public void mouseDragged( MouseEvent e )
    {
        if (slider.mouseMoved( e )) {
            nd.setTime( (long) slider.getValue() );
        }
        repaint();
    }
    
    @Override
    protected void paintComponent( Graphics g )
    {
        super.paintComponent( g );
        
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                              RenderingHints.VALUE_ANTIALIAS_ON );
        
        slider.draw( g2d );
        g2d.dispose();
    }
}
