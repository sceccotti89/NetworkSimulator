/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.plotter;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;

import simulator.graphics.plotter.Plotter.Plot;

public class ColorEditDialog extends JDialog implements ActionListener
{
    /** Generated serial ID */
    private static final long serialVersionUID = -2779232040438364524L;
    
    private final List<JTextField> fields;
    private final Plot plot;
    private final Plot plotClone;
    
    private JButton colorButton;
    

    public ColorEditDialog( JDialog parent, Frame frameLocation, Plot plot )
    {
        super( parent, true );
        
        setTitle( "Color" );
        
        this.plot = plot;
        plotClone = plot.clone();
        fields = new ArrayList<>( 10 );
        
        JPanel boxPanel = new JPanel();
        boxPanel.setLayout( new BoxLayout( boxPanel, BoxLayout.Y_AXIS ) );
        
        boxPanel.add( createColorTable( 0,  4 ) );
        boxPanel.add( createColorTable( 4,  8 ) );
        boxPanel.add( createColorTable( 8, 12 ) );
        boxPanel.add( createColorValuePanel( "Red", Color.RED, plot.color.getRed() ) );
        boxPanel.add( createColorValuePanel( "Green", Color.GREEN, plot.color.getGreen() ) );
        boxPanel.add( createColorValuePanel( "Blue", Color.BLUE, plot.color.getBlue() ) );
        boxPanel.add( createColorValuePanel( "Alpha", Color.BLACK, plot.color.getAlpha() ) );
        boxPanel.add( createColorButton() );
        
        
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
        setLocationRelativeTo( frameLocation );
    }
    
    private JPanel createColorTable( int from, int to )
    {
        JPanel panel = new JPanel();
        for (int i = from; i < to; i++) {
            Color color = Plotter.colors.get( i );
            JButton button = new ColorButton( this, color );
            drawColorOnButton( button, color, 25, 25 );
            panel.add( button );
        }
        
        return panel;
    }
    
    private void setColorValue( int red, int green, int blue, int alpha )
    {
        plotClone.color = new Color( red, green, blue, alpha );
        drawColorOnButton( colorButton, plotClone.color, 220, 15 );
        fields.get( 0 ).setText( red + "" );
        fields.get( 1 ).setText( green + "" );
        fields.get( 2 ).setText( blue + "" );
        fields.get( 3 ).setText( alpha + "" );
    }
    
    private JPanel createColorValuePanel( String name, Color nameColor, int value )
    {
        JPanel panel = new JPanel();
        JTextField colorField = new JTextField( name, 6 );
        colorField.setEditable( false );
        colorField.setHorizontalAlignment( JTextField.CENTER );
        colorField.setBorder( null );
        colorField.setForeground( nameColor );
        panel.add( colorField );
        
        JTextField field = new JTextField( 17 );
        field.setToolTipText( "Color component" );
        field.setHorizontalAlignment( JTextField.CENTER );
        field.setText( value + "" );
        panel.add( field );
        field.addKeyListener( new KeyListener() {
            @Override
            public void keyTyped( KeyEvent e ) {
                if (!Character.isDigit( e.getKeyChar() )) {
                    e.consume();
                }
            }
            
            @Override
            public void keyReleased( KeyEvent e ) {
                if (!field.getText().isEmpty()) {
                    Integer value = Integer.parseInt( field.getText() );
                    if (value > 255) {
                        field.setText( "255" );
                    }
                }
                
                int red = 0, green = 0, blue = 0, alpha = 0;
                if (!fields.get( 0 ).getText().isEmpty()) red   = Integer.parseInt( fields.get( 0 ).getText() );
                if (!fields.get( 1 ).getText().isEmpty()) green = Integer.parseInt( fields.get( 1 ).getText() );
                if (!fields.get( 2 ).getText().isEmpty()) blue  = Integer.parseInt( fields.get( 2 ).getText() );
                if (!fields.get( 3 ).getText().isEmpty()) alpha = Integer.parseInt( fields.get( 3 ).getText() );
                setColorValue( red, green, blue, alpha );
            }
            @Override
            public void keyPressed( KeyEvent e ) {
                /*if (e.getID() != KeyEvent.VK_DELETE &&
                    e.getID() != KeyEvent.VK_CANCEL && 
                    field.getText().length() >= 3) {
                    System.out.println( "SONO QUI" );
                    e.consume();
                }*/
            }
        } );
        fields.add( field );
        
        return panel;
    }
    
    private void drawColorOnButton( JButton button, Color color, int width, int height )
    {
        BufferedImage lineImage = new BufferedImage( width, height, BufferedImage.TYPE_INT_RGB );
        Graphics2D g = (Graphics2D) lineImage.createGraphics();
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        g.setColor( color );
        g.fillRect( 0, 0, width, height );
        g.setColor( Color.BLACK );
        g.drawRect( 0, 0, width, height );
        button.setIcon( new ImageIcon( lineImage ) );
        g.dispose();
    }

    private JPanel createColorButton()
    {
        JPanel panel = new JPanel();
        colorButton = new JButton();
        drawColorOnButton( colorButton, plotClone.color, 220, 15 );
        panel.add( colorButton );
        
        return panel;
    }
    
    @Override
    public void actionPerformed( ActionEvent e )
    {
        if (e.getActionCommand().equals( "Save" )) {
            // Save the inserted values.
            plot.color = plotClone.color;
            plot.updateValues();
        }
        
        dispose();
    }
    
    @Override
    public void setVisible( boolean visible ) {
        colorButton.requestFocusInWindow();
        super.setVisible( visible );
    }
    
    private static class ColorButton extends JButton implements FocusListener
    {
        /** Generated serial ID. */
        private static final long serialVersionUID = 5648900962203337154L;
        private final ColorEditDialog dialog;
        private final Color color;
        
        public ColorButton( ColorEditDialog dialog, Color color )
        {
            this.dialog = dialog;
            this.color = color;
            addFocusListener( this );
        }
        
        @Override
        public void focusGained( FocusEvent e ) {
            dialog.setColorValue( color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() );
        }

        @Override
        public void focusLost( FocusEvent e ) {}
    }
}
