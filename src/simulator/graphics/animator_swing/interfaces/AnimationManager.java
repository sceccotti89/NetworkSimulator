/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.animator_swing.interfaces;

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
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.metal.MetalSliderUI;

import simulator.graphics.animation_swing.AnimationNetwork;
import simulator.graphics.animation_swing.elements.TimeSlider;

public class AnimationManager extends JPanel implements ChangeListener, ComponentListener, ActionListener, MouseListener
{
    /** Generated Serial ID. */
    private static final long serialVersionUID = 7463830294028112320L;
    
    private NetworkDisplay nd;
    
    private JSlider time;
    
    private List<AbstractButton> buttons;
    
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
        
        // TODO utilizzare posizioni assolute, forse mi conviene...
        setLayout( new GridBagLayout() );
        GridBagConstraints c = new GridBagConstraints();
        
        addComponentListener( this );
        
        this.nd = nd;
        nd.setAnimationManager( this );
        
        slider = new TimeSlider( TimeSlider.HORIZONTAL, 0, 200, 0 );
        slider.setBounds( 50, 10, width - 100, 10 );
        
        addTimeBar();
        
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
        c.gridy = 1;
        buttons.add( button );
        add( button, c );
        
        buttons.add( button );
        button = new JButton(); 
        button.setName( STOP );
        button.addActionListener( this );
        button.setFocusable( false );
        button.setIcon( stop );
        c.weightx = 0.5;
        c.weighty = 1;
        c.gridx = 10;
        c.gridy = 1;
        add( button );
        buttons.add( button );
    }
    
    private void createButtonImages()
    {
        final Color background = new JButton().getBackground();
        final int WIDTH  = 30;
        final int HEIGHT = 25;
        
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
    
    private void addTimeBar()
    {
        time = new JSlider( JSlider.HORIZONTAL, 0, 200, 0 );
        time.setPreferredSize( new Dimension( getWidth() - 10, getHeight() ) );
        time.setForeground( Color.RED );
        time.setMajorTickSpacing( 1 );
        time.setMinorTickSpacing( 1 );
        time.addChangeListener( this );
        //time.setPaintTicks( true );
        //time.setPaintLabels( true );
        add( time );
        
        time.setUI( new MetalSliderUI() {
            @Override
            protected void scrollDueToClickInTrack( final int direction )
            {
                int value = slider.getValue(); 
                if (slider.getOrientation() == JSlider.HORIZONTAL) {
                    value = valueForXPosition( slider.getMousePosition().x );
                } else if (slider.getOrientation() == JSlider.VERTICAL) {
                    value = valueForYPosition( slider.getMousePosition().y );
                }
                slider.setValue( value );
            }
        } );
    }
    
    @Override
    public void stateChanged( final ChangeEvent e )
    {
        double value = time.getValue();
        long timer = (long) (AnimationNetwork.timeSimulation * (value / time.getMaximum()));
        nd.setTime( timer );
    }
    
    public void update()
    {
        if (!nd.isPauseAnimation()) {
            long timer = nd.getTime();
            slider.setValue( (int) (slider.getMaximum() * (timer / (double) AnimationNetwork.timeSimulation)) );
            //time.setValue( (int) (time.getMaximum() * (timer / (double) AnimationNetwork.timeSimulation)));
            repaint();
        }
    }
    
    @Override
    public void componentResized( final ComponentEvent e )
    {
        remove( time );
        addTimeBar();
    }
    
    @Override
    public void componentMoved( final ComponentEvent e ) {}
    @Override
    public void componentShown( final ComponentEvent e ) {}
    @Override
    public void componentHidden( final ComponentEvent e ) {}
    
    
    public void reset()
    {
        time.setValue( 0 );
        buttons.get( 0 ).setName( START );
        buttons.get( 0 ).setIcon( start );
        //buttons.get( 0 ).setText( START );
    }

    @Override
    public void actionPerformed( final ActionEvent e )
    {
        AbstractButton button = (AbstractButton) e.getSource();
        switch (button.getName()) {
            case( START ):
                nd.startAnimation();
                button.setName( PAUSE );
                button.setIcon( pause );
                //button.setText( PAUSE );
                break;
            case( PAUSE ):
                nd.pauseAnimation();
                button.setName( START );
                button.setIcon( start );
                //button.setText( START );
                break;
            case( STOP ):
                nd.stopAnimation();
                reset();
                break;
        }
    }

    @Override
    public void mouseClicked( final MouseEvent e ) {}

    @Override
    public void mousePressed( final MouseEvent e )
    {
        if (slider.checkMouseClick( e.getPoint() )) {
            // TODO implementare
            //double value = time.getValue();
            //long timer = (long) (AnimationNetwork.timeSimulation * (value / time.getMaximum()));
            //nd.setTime( timer );
            repaint();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // TODO Auto-generated method stub
        
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