/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.plotter;

import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import simulator.graphics.plotter.Plotter.PlotterSettings;
import simulator.graphics.plotter.Plotter.Range;

public class SettingsDialog extends JDialog implements ActionListener, KeyListener
{
    /** Generated serial ID */
    private static final long serialVersionUID = -2779232040438364524L;
    
    private final List<JTextField> fields;
    private final PlotterSettings settings;
    
    private JButton saveButton;
    

    public SettingsDialog( Frame frame, PlotterSettings settings )
    {
        super( frame, true );
        
        setTitle( "Settings" );
        
        this.settings = settings;
        fields = new ArrayList<>( 10 );
        
        JPanel boxPanel = new JPanel();
        boxPanel.setLayout( new BoxLayout( boxPanel, BoxLayout.Y_AXIS ) );
        
        int xNumTicks = (settings._xNumTicks == Integer.MAX_VALUE) ? 1 : settings._xNumTicks;
        int yNumTicks = (settings._yNumTicks == Integer.MAX_VALUE) ? 1 : settings._yNumTicks;
        boxPanel.add( createPanel( "X ticks", "Value", xNumTicks + "", "Interval", settings.xTickInterval + "" ) );
        boxPanel.add( createPanel( "Y ticks", "Value", yNumTicks + "", "Interval", settings.yTickInterval + "" ) );
        Range range = settings._range;
        boxPanel.add( createPanel( "X range", "From", getValue( range.getMinX() ), "To", getValue( range.getMaxX() ) ) );
        boxPanel.add( createPanel( "Y range", "From", getValue( range.getMinY() ), "To", getValue( range.getMaxY() ) ) );
        
        boxPanel.add( createScalePanel( "X scale", getValue( settings.xScale ) ) );
        boxPanel.add( createScalePanel( "Y scale", getValue( settings.yScale ) ) );
        
        JPanel panel = new JPanel();
        panel.setLayout( new FlowLayout() );
        
        saveButton = new JButton( "Save" );
        saveButton.setActionCommand( "Save" );
        saveButton.addActionListener( this );
        saveButton.addKeyListener( this );
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
    
    @Override
    public void setVisible( boolean visible )
    {
        if (visible) {
            saveButton.requestFocus( false );
            saveButton.requestFocusInWindow();
        }
        
        super.setVisible( visible );
    }
    
    private String getValue( double value )
    {
        if (value == Double.POSITIVE_INFINITY || value == Double.NEGATIVE_INFINITY) {
            return "";
        } else {
            return value + "";
        }
    }
    
    private JPanel createPanel( String name, String tooltip1, String value1,
                                                   String tooltip2, String value2 )
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
        field1.addKeyListener( this );
        panel.add( field1 );
        fields.add( field1 );
        
        JTextField field2 = new JTextField( columns );
        field2.setToolTipText( tooltip2 );
        field2.setHorizontalAlignment( JTextField.CENTER );
        field2.setText( value2 );
        field2.addKeyListener( this );
        panel.add( field2 );
        fields.add( field2 );
        
        return panel;
    }
    
    private JPanel createScalePanel( String name, String value )
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
        field1.addKeyListener( this );
        panel.add( field1 );
        fields.add( field1 );
        
        return panel;
    }
    
    private void showErrorDialog( String error ) {
        JOptionPane.showMessageDialog( this, error, "error", JOptionPane.ERROR_MESSAGE );
    }
    
    private void saveValues()
    {
        // TODO prima di salvare i valori dovrei prima controllare la loro correttezza??
        // Save the inserted values.
        int index = 0;
        double value;
        double min = 0;
        double oldMinX = settings._range.getMinX();
        double oldMinY = settings._range.getMinY();
        for (JTextField field : fields) {
            if (!field.getText().isEmpty()) {
                value = Double.parseDouble( field.getText() );
                switch (index) {
                    case( 0 ):
                        if (value <= 0) { showErrorDialog( "Number of X ticks must be greater then 0." ); return; }
                        settings._xNumTicks = (int) value;
                        break;
                    case( 1 ):
                        if (value <= 0) { showErrorDialog( "X intervals must be greater then 0." ); return; }
                        settings.xTickInterval = (int) value;
                        break;
                    case( 2 ):
                        if (value <= 0) { showErrorDialog( "Number of Y ticks must be greater then 0." ); return; }
                        settings._yNumTicks = (int) value;
                        break;
                    case( 3 ):
                        if (value <= 0) { showErrorDialog( "Y intervals must be greater then 0." ); return; }
                        settings.yTickInterval = (int) value;
                        break;
                    case( 4 ):
                        settings._range.setMinX( min = value );
                        settings._settedXRangeByUser = true;
                        break;
                    case( 5 ):
                        if (value < 0) { showErrorDialog( "X maximum range cannot be less then 0." ); return; }
                        if (value < min) { settings._range.setMinX( oldMinX ); showErrorDialog( "X maximum range cannot be less then the minimum." ); return; }
                        settings._range.setMaxX( value );
                        settings._settedXRangeByUser = true;
                        break;
                    case( 6 ):
                        settings._range.setMinY( min = value );
                        settings._settedYRangeByUser = true;
                        break;
                    case( 7 ):
                        if (value < 0) { showErrorDialog( "Y maximum range cannot be less then 0." ); return; }
                        if (value < min) { settings._range.setMinY( oldMinY ); showErrorDialog( "Y maximum range cannot be less then the minimum." ); return; }
                        settings._range.setMaxY( value );
                        settings._settedYRangeByUser = true;
                        break;
                    case( 8 ):
                        if (value <= 0) { showErrorDialog( "X scale cannot be less or equal then 0." ); return; }
                        settings.xScale = value;
                        break;
                    case( 9 ):
                        if (value <= 0) { showErrorDialog( "Y scale cannot be less or equal then 0." ); return; }
                        settings.yScale = value;
                        break;
                }
            } else {
                switch (index) {
                    case( 0 ): showErrorDialog( "X ticks value cannot be empty." ); return;
                    case( 1 ): showErrorDialog( "X ticks interval cannot be empty." ); return;
                    case( 2 ): showErrorDialog( "Y ticks value cannot be empty." ); return;
                    case( 3 ): showErrorDialog( "Y ticks interval cannot be empty." ); return;
                    case( 4 ): showErrorDialog( "Min X range cannot be empty." ); return;
                    case( 5 ): showErrorDialog( "Max X range cannot be empty." ); return;
                    case( 6 ): showErrorDialog( "Min Y range cannot be empty." ); return;
                    case( 7 ): showErrorDialog( "Max Y range cannot be empty." ); return;
                    case( 8 ): showErrorDialog( "X scale cannot be empty." ); return;
                    case( 9 ): showErrorDialog( "Y scale cannot be empty." ); return;
                }
            }
            
            index++;
        }
        
        dispose();
        setVisible( false );
    }
    
    @Override
    public void actionPerformed( ActionEvent e )
    {
        if (e.getActionCommand().equals( "Save" )) {
            saveValues();
            return;
        }
        
        dispose();
        setVisible( false );
    }

    @Override
    public void keyTyped( KeyEvent e ) {}
    
    @Override
    public void keyPressed( KeyEvent e )
    {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            saveValues();
        }
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            dispose();
        }
        
        if (!Character.isDigit( e.getKeyChar() ) &&
            e.getKeyCode() != KeyEvent.VK_BACK_SPACE &&
            e.getKeyCode() != KeyEvent.VK_DELETE &&
            e.getKeyCode() != KeyEvent.VK_UP &&
            e.getKeyCode() != KeyEvent.VK_DOWN &&
            e.getKeyCode() != KeyEvent.VK_RIGHT &&
            e.getKeyCode() != KeyEvent.VK_LEFT) {
            e.consume(); // ignore event
        }
    }
    
    @Override
    public void keyReleased( KeyEvent e ) {}
}
