/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.plotter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;

import simulator.graphics.plotter.Plotter.Line;
import simulator.graphics.plotter.Plotter.Plot;
import simulator.graphics.plotter.Plotter.PointType;

public class PlotEditDialog extends JDialog implements ActionListener
{
    /** Generated serial ID */
    private static final long serialVersionUID = -2779232040438364524L;
    
    private final Plot plot;
    private final Plot plotClone;
    
    private JButton buttonLine;
    private JButton pointTypeButton;
    private JTextField fieldName;
    private JTextField lineWidth;
    
    private int lineIndex;
    private int pointLineIndex;
    
    private final PlotEditDialog DIALOG = this;
    
    private static final int BI_WIDTH  = 155;
    private static final int BI_HEIGHT =  15;
    

    public PlotEditDialog( Frame frame, Plot plot )
    {
        super( frame, true );
        
        setTitle( "Plot" );
        
        this.plot = plot;
        plotClone = plot.clone();
        
        switch (plot.line) {
            case NOTHING : lineIndex = 0; break;
            case UNIFORM : lineIndex = 1; break;
            case DASHED  : lineIndex = 2; break;
        }
        
        switch (plot.pointer) {
            case NOTHING  : pointLineIndex = 0; break;
            case CIRCLE   : pointLineIndex = 1; break;
            case QUADRATE : pointLineIndex = 2; break;
            case TRIANGLE : pointLineIndex = 3; break;
            case CROSS    : pointLineIndex = 4; break;
        }
        
        JPanel boxPanel = new JPanel();
        boxPanel.setLayout( new BoxLayout( boxPanel, BoxLayout.Y_AXIS ) );
        
        boxPanel.add( createTitlePanel( plot.title ) );
        boxPanel.add( createLinePanel() );
        boxPanel.add( createPointTypePanel() );
        boxPanel.add( createLineWidthPanel( plot.lineWidth + "" ) );
        boxPanel.add( createColorPanel( frame ) );
        
        JPanel panel = new JPanel();
        panel.setLayout( new FlowLayout() );
        
        JButton saveButton = new JButton( "Save" );
        saveButton.setActionCommand( "Save" );
        saveButton.addActionListener( this );
        panel.add( saveButton);
        
        JButton cancelButton = new JButton( "Cancel" );
        cancelButton.setActionCommand( "Cancel" );
        cancelButton.addActionListener( this );
        panel.add( cancelButton);
        
        boxPanel.add( panel );
        
        add( boxPanel );
        
        pack();
        setLocationRelativeTo( frame );
    }
    
    private JPanel createTitlePanel( String value )
    {
        JPanel panel = new JPanel();
        JTextField fieldText = new JTextField( "Title", 6 );
        fieldText.setEditable( false );
        fieldText.setHorizontalAlignment( JTextField.CENTER );
        fieldText.setBorder( null );
        panel.add( fieldText );
        fieldName = new JTextField( 17 );
        fieldName.setToolTipText( "Title name" );
        fieldName.setHorizontalAlignment( JTextField.CENTER );
        fieldName.setText( value );
        panel.add( fieldName );
        
        return panel;
    }
    
    private void drawLineOnButton( JButton button )
    {
        BufferedImage lineImage = new BufferedImage( BI_WIDTH, BI_HEIGHT, BufferedImage.TYPE_INT_RGB );
        Graphics2D g = (Graphics2D) lineImage.createGraphics();
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        g.setColor( button.getBackground() );
        g.fillRect( 0, 0, BI_WIDTH, BI_HEIGHT );
        if (plotClone.line == Line.NOTHING) {
            g.setColor( button.getBackground() );
        } else {
            g.setColor( plotClone.color );
            g.setStroke( plotClone.stroke );
        }
        g.drawLine( 2, BI_HEIGHT/2, BI_WIDTH - 2, BI_HEIGHT/2 );
        button.setIcon( new ImageIcon( lineImage ) );
        g.dispose();
    }
    
    private JPanel createPointTypePanel()
    {
        JPanel panel = new JPanel();
        JTextField typeField = new JTextField( "Points", 6 );
        typeField.setEditable( false );
        typeField.setHorizontalAlignment( JTextField.CENTER );
        typeField.setBorder( null );
        panel.add( typeField );
        
        pointTypeButton = new JButton();
        pointTypeButton.setBackground( Color.GRAY );
        pointTypeButton.setFocusable( false );
        panel.add( pointTypeButton );
        
        drawPointType( pointTypeButton );
        
        pointTypeButton.addActionListener( new ActionListener() {
            private static final int MAX_TYPES = 5;
            
            @Override
            public void actionPerformed( ActionEvent e )
            {
                pointLineIndex = (pointLineIndex + 1) % MAX_TYPES;
                if (pointLineIndex == 0 && plotClone.line == Line.NOTHING) {
                    pointLineIndex = 1;
                }
                drawPointType( pointTypeButton );
                drawLineOnButton( buttonLine );
            }
        } );
        
        return panel;
    }
    
    private void drawPointType( JButton button )
    {
        BufferedImage lineImage = new BufferedImage( BI_WIDTH, BI_HEIGHT, BufferedImage.TYPE_INT_RGB );
        Graphics2D g = (Graphics2D) lineImage.createGraphics();
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        g.setColor( button.getBackground() );
        g.fillRect( 0, 0, BI_WIDTH, BI_HEIGHT );
        g.setColor( plotClone.color );
        g.setStroke( plotClone.stroke );
        
        if (plotClone.line != Line.NOTHING) {
            g.drawLine( 2, BI_HEIGHT/2, BI_WIDTH - 2, BI_HEIGHT/2 );
        }
        
        final int WIDTH = Plot.WIDTH;
        final int HEIGHT = Plot.HEIGHT;
        
        Shape shape = null;
        switch (pointLineIndex) {
            case( 0 ): plotClone.pointer = PointType.NOTHING;
                       shape = new Rectangle(); break;
            case( 1 ): plotClone.pointer = PointType.CIRCLE;
                       shape = new Ellipse2D.Double( BI_WIDTH/2 - WIDTH/2, BI_HEIGHT/2 - HEIGHT/2, WIDTH, HEIGHT ); break;
            case( 2 ): plotClone.pointer = PointType.QUADRATE;
                       shape = new Rectangle( BI_WIDTH/2 - WIDTH/2, BI_HEIGHT/2 - HEIGHT/2, WIDTH, HEIGHT ); break;
            case( 3 ): plotClone.pointer = PointType.TRIANGLE;
                       shape = new Polygon( new int[]{ BI_WIDTH/2 - WIDTH/2, BI_WIDTH/2 + WIDTH/2, BI_WIDTH/2 },
                                            new int[]{ BI_HEIGHT/2 + HEIGHT/2, BI_HEIGHT/2 + HEIGHT/2, BI_HEIGHT/2 - HEIGHT/2 }, 3 );
                       break;
            case( 4 ): plotClone.pointer = PointType.CROSS;
                       g.drawLine( BI_WIDTH/2 - WIDTH/2, BI_HEIGHT/2 - HEIGHT/2, BI_WIDTH/2 + WIDTH/2, BI_HEIGHT/2 + WIDTH/2 );
                       g.drawLine( BI_WIDTH/2 - WIDTH/2, BI_HEIGHT/2 + HEIGHT/2, BI_WIDTH/2 + WIDTH/2, BI_HEIGHT/2 - HEIGHT/2 );
                       break;
        }
        
        if (shape != null)
            g.fill( shape );
        
        button.setIcon( new ImageIcon( lineImage ) );
        g.dispose();
    }
    
    private JPanel createLinePanel()
    {
        JPanel panel = new JPanel();
        JTextField fieldName = new JTextField( "Line", 6 );
        fieldName.setEditable( false );
        fieldName.setHorizontalAlignment( JTextField.CENTER );
        fieldName.setBorder( null );
        panel.add( fieldName );
        
        buttonLine = new JButton();
        buttonLine.setBackground( Color.GRAY );
        buttonLine.setFocusable( false );
        panel.add( buttonLine );
        
        drawLineOnButton( buttonLine );
        
        buttonLine.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e )
            {
                lineIndex = (lineIndex + 1) % 3;
                if (lineIndex == 0 && plotClone.pointer == PointType.NOTHING) {
                    lineIndex = 1;
                }
                switch (lineIndex) {
                    case( 0 ):
                        plotClone.line = Line.NOTHING;
                        break;
                    case( 1 ):
                        plotClone.line = Line.UNIFORM;
                        plotClone.stroke = new BasicStroke( plotClone.lineWidth );
                        break;
                    case( 2 ):
                        plotClone.line = Line.DASHED;
                        plotClone.stroke = new BasicStroke( plotClone.lineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                                                            0, new float[]{ 20f, 10f }, 0 );
                        break;
                }
                drawLineOnButton( buttonLine );
                drawPointType( pointTypeButton );
            }
        } );
        
        return panel;
    }
    
    private JPanel createLineWidthPanel( String value )
    {
        JPanel panel = new JPanel();
        JTextField fieldText = new JTextField( "Line width", 6 );
        fieldText.setEditable( false );
        fieldText.setHorizontalAlignment( JTextField.CENTER );
        fieldText.setBorder( null );
        panel.add( fieldText );
        lineWidth = new JTextField( 17 );
        lineWidth.setToolTipText( "Width" );
        lineWidth.setHorizontalAlignment( JTextField.CENTER );
        lineWidth.setText( value );
        lineWidth.addKeyListener( new KeyAdapter() {
            @Override
            public void keyReleased( KeyEvent e ) {
                try {
                    float value = Float.parseFloat( lineWidth.getText() );
                    plotClone.lineWidth = value;
                    switch (lineIndex) {
                        case( 1 ):
                            plotClone.stroke = new BasicStroke( plotClone.lineWidth );
                            break;
                        case( 2 ):
                            plotClone.stroke = new BasicStroke( plotClone.lineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                                                                0, new float[]{ 20f, 10f }, 0 );
                            break;
                    }
                    drawLineOnButton( buttonLine );
                    drawPointType( pointTypeButton );
                } catch ( NumberFormatException ex ) {}
                
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    saveAndExit();
                }
            }
        } );
        panel.add( lineWidth );
        
        return panel;
    }
    
    private void drawColorOnButton( JButton button )
    {
        BufferedImage lineImage = new BufferedImage( BI_WIDTH, BI_HEIGHT, BufferedImage.TYPE_INT_RGB );
        Graphics2D g = (Graphics2D) lineImage.createGraphics();
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        g.setColor( plotClone.color );
        g.fillRect( 0, 0, BI_WIDTH, BI_HEIGHT );
        g.setColor( Color.BLACK );
        g.drawRect( 0, 0, BI_WIDTH, BI_HEIGHT );
        button.setIcon( new ImageIcon( lineImage ) );
        g.dispose();
    }
    
    private JPanel createColorPanel( Frame frame )
    {
        JPanel panel = new JPanel();
        JTextField fieldName = new JTextField( "Color", 6 );
        fieldName.setEditable( false );
        fieldName.setHorizontalAlignment( JTextField.CENTER );
        fieldName.setBorder( null );
        panel.add( fieldName );
        
        JButton buttonColor = new JButton();
        buttonColor.setFocusable( false );
        panel.add( buttonColor );
        //final int BI_WIDTH  = buttonColor.getBounds().width;
        //final int BI_HEIGHT = buttonColor.getBounds().height;
        
        drawColorOnButton( buttonColor );
        
        buttonColor.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e ) {
                ColorEditDialog dialog = new ColorEditDialog( DIALOG, frame, plotClone );
                dialog.setVisible( true );
                drawPointType( pointTypeButton );
                drawColorOnButton( buttonColor );
                drawLineOnButton( buttonLine );
            }
        } );
        
        return panel;
    }
    
    private void saveAndExit()
    {
        // Save the inserted values.
        plot.title     = fieldName.getText();
        plot.line      = plotClone.line;
        plot.pointer   = plotClone.pointer;
        plot.lineWidth = Float.parseFloat( lineWidth.getText() );
        plot.color     = plotClone.color;
        
        plot.updateValues();
        dispose();
    }
    
    @Override
    public void actionPerformed( ActionEvent e )
    {
        if (e.getActionCommand().equals( "Save" )) {
            saveAndExit();
            return;
        }
        
        dispose();
    }
}
