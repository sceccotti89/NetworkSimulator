/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.plotter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;

import simulator.graphics.plotter.Plotter.Line;
import simulator.graphics.plotter.Plotter.Plot;

public class PlotEditDialog extends JDialog implements ActionListener
{
    /** Generated serial ID */
    private static final long serialVersionUID = -2779232040438364524L;
    
    private final Plot plot;
    private final Plot plotClone;
    
    private JButton buttonLine;
    private JTextField fieldName;
    

    public PlotEditDialog( final Frame frame, final Plot plot )
    {
        super( frame, true );
        
        setTitle( "Plot" );
        
        this.plot = plot;
        plotClone = plot.clone();
        
        JPanel boxPanel = new JPanel();
        boxPanel.setLayout( new BoxLayout( boxPanel, BoxLayout.Y_AXIS ) );
        
        boxPanel.add( createTitlePanel( plot.title ) );
        boxPanel.add( createLinePanel(   ) );
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
    
    private JPanel createTitlePanel( final String value )
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
    
    private void drawLineOnButton( final JButton button )
    {
        final int BI_WIDTH  = 155;
        final int BI_HEIGHT =  15;
        BufferedImage lineImage = new BufferedImage( BI_WIDTH, BI_HEIGHT, BufferedImage.TYPE_INT_RGB );
        Graphics2D g = (Graphics2D) lineImage.createGraphics();
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        g.setColor( button.getBackground() );
        g.fillRect( 0, 0, BI_WIDTH, BI_HEIGHT );
        g.setColor( plotClone.color );
        g.setStroke( plotClone.stroke );
        g.drawLine( 2, BI_HEIGHT / 2, BI_WIDTH - 2, BI_HEIGHT / 2 );
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
        //final int BI_WIDTH  = buttonColor.getBounds().width;
        //final int BI_HEIGHT = buttonColor.getBounds().height;
        
        drawLineOnButton( buttonLine );
        
        buttonLine.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( final ActionEvent e ) {
                BasicStroke stroke = (BasicStroke) plotClone.stroke;
                if (stroke.getDashArray() != null) {
                    plotClone.line = Line.UNIFORM;
                    plotClone.stroke = new BasicStroke( 2f );
                } else { // Dashed.
                    plotClone.line = Line.DASHED;
                    plotClone.stroke = new BasicStroke( 2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{ 20f, 10f }, 0 );
                }
                drawLineOnButton( buttonLine );
            }
        } );
        
        return panel;
    }
    
    private void drawColorOnButton( final JButton button )
    {
        final int BI_WIDTH  = 155;
        final int BI_HEIGHT =  15;
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
    
    private JPanel createColorPanel( final Frame frame )
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
            public void actionPerformed( final ActionEvent e ) {
                ColorEditDialog dialog = new ColorEditDialog( frame, plotClone );
                dialog.setVisible( true );
                drawColorOnButton( buttonColor );
                drawLineOnButton( buttonLine );
            }
        } );
        
        return panel;
    }
    
    @Override
    public void actionPerformed( final ActionEvent e )
    {
        if (e.getActionCommand().equals( "Save" )) {
            // Save the inserted values.
            plot.title = fieldName.getText();
            plot.line  = plotClone.line;
            plot.color = plotClone.color;
            
            plot.updateValues();
        }
        
        dispose();
    }
}