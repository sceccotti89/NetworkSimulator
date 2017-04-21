/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.plotter;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;

import simulator.graphics.plotter.Plotter.PlotterSettings;
import simulator.graphics.plotter.Plotter.Range;

public class SettingsDialog extends JDialog implements ActionListener
{
    /** Generated serial ID */
    private static final long serialVersionUID = -2779232040438364524L;
    
    private final List<JTextField> fields;
    private final PlotterSettings settings;
    

    public SettingsDialog( final Frame frame, final PlotterSettings settings )
    {
        super( frame, true );
        
        setTitle( "Settings" );
        
        this.settings = settings;
        fields = new ArrayList<>( 10 );
        
        JPanel boxPanel = new JPanel();
        boxPanel.setLayout( new BoxLayout( boxPanel, BoxLayout.Y_AXIS ) );
        
        boxPanel.add( createPanel( "X ticks", "Value", settings._xNumTicks + "", "Interval", settings.xTickInterval + "" ) );
        boxPanel.add( createPanel( "Y ticks", "Value", settings._yNumTicks + "", "Interval", settings.yTickInterval + "" ) );
        Range range = settings._range;
        boxPanel.add( createPanel( "X range", "From", getValue( range.getMinX() ), "To", getValue( range.getMaxX() ) ) );
        boxPanel.add( createPanel( "Y range", "From", getValue( range.getMinY() ), "To", getValue( range.getMaxY() ) ) );
        
        boxPanel.add( createScalePanel( "X scale", getValue( settings.xScale ) ) );
        boxPanel.add( createScalePanel( "Y scale", getValue( settings.yScale ) ) );
        
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
    
    private String getValue( final double value )
    {
        if (value == Double.MAX_VALUE || value == Double.MIN_VALUE) {
            return "";
        } else {
            return value + "";
        }
    }
    
    private JPanel createPanel( final String name, final String tooltip1, final String value1,
                                                   final String tooltip2, final String value2 )
    {
        final int columns = 8;
        JPanel panel = new JPanel();
        JTextField fieldName = new JTextField( name, columns );
        fieldName.setEditable( false );
        fieldName.setHorizontalAlignment( JTextField.CENTER );
        fieldName.setBorder( null );
        panel.add( fieldName );
        JTextField field1 = new JTextField( columns );
        field1.setToolTipText( tooltip1 );
        field1.setHorizontalAlignment( JTextField.CENTER );
        field1.setText( value1 );
        field1.addKeyListener( new KeyAdapter() {
            @Override
            public void keyTyped( final KeyEvent e ) {
                if (!Character.isDigit( e.getKeyChar() )) {
                    e.consume(); // ignore event
                }
            }
        });
        panel.add( field1 );
        fields.add( field1 );
        
        JTextField field2 = new JTextField( columns );
        field2.setToolTipText( tooltip2 );
        field2.setHorizontalAlignment( JTextField.CENTER );
        field2.setText( value2 );
        field2.addKeyListener( new KeyAdapter() {
            @Override
            public void keyTyped( final KeyEvent e ) {
                if (!Character.isDigit( e.getKeyChar() )) {
                    e.consume(); // ignore event
                }
            }
        });
        panel.add( field2 );
        fields.add( field2 );
        
        return panel;
    }
    
    private JPanel createScalePanel( final String name, final String value )
    {
        final int columns = 17;
        JPanel panel = new JPanel();
        JTextField fieldName = new JTextField( name, 8 );
        fieldName.setEditable( false );
        fieldName.setHorizontalAlignment( JTextField.CENTER );
        fieldName.setBorder( null );
        panel.add( fieldName );
        JTextField field1 = new JTextField( columns );
        field1.setToolTipText( "Value" );
        field1.setHorizontalAlignment( JTextField.CENTER );
        field1.setText( value );
        field1.addKeyListener( new KeyAdapter() {
            @Override
            public void keyTyped( final KeyEvent e ) {
                if (!Character.isDigit( e.getKeyChar() )) {
                    e.consume(); // ignore event
                }
            }
        });
        panel.add( field1 );
        fields.add( field1 );
        
        return panel;
    }
    
    @Override
    public void actionPerformed( final ActionEvent e )
    {
        if (e.getActionCommand().equals( "Save" )) {
            // Save the inserted values.
            int index = 0;
            for (JTextField field : fields) {
                switch (index) {
                    case( 0 ): settings._xNumTicks = Integer.parseInt( field.getText() ); break;
                    case( 1 ): settings.xTickInterval = Integer.parseInt( field.getText() ); break;
                    case( 2 ): settings._yNumTicks = Integer.parseInt( field.getText() ); break;
                    case( 3 ): settings.yTickInterval = Integer.parseInt( field.getText() ); break;
                    case( 4 ): settings._range.setMinX( Double.parseDouble( field.getText() ) ); break;
                    case( 5 ): settings._range.setMaxX( Double.parseDouble( field.getText() ) ); break;
                    case( 6 ): settings._range.setMinY( Double.parseDouble( field.getText() ) ); break;
                    case( 7 ): settings._range.setMaxY( Double.parseDouble( field.getText() ) ); break;
                    case( 8 ): settings.xScale = Double.parseDouble( field.getText() ); break;
                    case( 9 ): settings.yScale = Double.parseDouble( field.getText() ); break;
                }
                
                index++;
            }
        }
        
        dispose();
    }
}