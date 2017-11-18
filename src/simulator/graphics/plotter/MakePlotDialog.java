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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import simulator.graphics.plotter.Plotter.Line;
import simulator.graphics.plotter.Plotter.Plot;
import simulator.graphics.plotter.evaluator.EvaluationException;
import simulator.graphics.plotter.evaluator.Evaluator;
import simulator.utils.Pair;

public class MakePlotDialog extends JDialog implements ActionListener
{
    /** Generated serial ID */
    private static final long serialVersionUID = -2779232040438364524L;
    
    private Plotter plotter;
    private final Plot plot;
    
    private JButton buttonLine;
    private JTextField fieldName;
    private JTextField functionField;
    private JTextField fromField, toField, jumpField;
    private JTextField lineWidth;
    
    private final MakePlotDialog DIALOG = this;
    

    public MakePlotDialog( Frame frame, Plotter plotter )
    {
        super( frame, true );
        
        setTitle( "Plot" );
        
        this.plotter = plotter;
        plot = new Plot( "", null, Color.GREEN, Line.UNIFORM, 2f, null );
        
        JPanel boxPanel = new JPanel();
        boxPanel.setLayout( new BoxLayout( boxPanel, BoxLayout.Y_AXIS ) );
        
        boxPanel.add( createTitlePanel() );
        boxPanel.add( createFunctionPanel() );
        boxPanel.add( createIntervalPanel() );
        
        boxPanel.add( createLinePanel() );
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
    
    private JPanel createTitlePanel()
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
        panel.add( fieldName );
        
        return panel;
    }
    
    private JPanel createFunctionPanel()
    {
        JPanel panel = new JPanel();
        JTextField fieldText = new JTextField( "Function", 6 );
        fieldText.setEditable( false );
        fieldText.setFocusable( false );
        fieldText.setHorizontalAlignment( JTextField.CENTER );
        fieldText.setBorder( null );
        panel.add( fieldText );
        functionField = new JTextField( 17 );
        functionField.setToolTipText( "Function used to draw the plot." );
        functionField.setHorizontalAlignment( JTextField.CENTER );
        functionField.setText( "x" );
        functionField.addKeyListener( new KeyAdapter() {
            @Override
            public void keyReleased( KeyEvent e ) {
                if (functionField.getText().isEmpty()) {
                    functionField.setText( "x" );
                }
            }
        } );
        functionField.addFocusListener( new FocusListener() {
            @Override
            public void focusLost( FocusEvent e ) {
                functionField.select( 0, 0 );
            }
            
            @Override
            public void focusGained( FocusEvent e ) {
                functionField.selectAll();
            }
        } );
        panel.add( functionField );
        
        return panel;
    }
    
    private JPanel createIntervalPanel()
    {
        JPanel panel = new JPanel();
        JTextField fieldText = new JTextField( "X range", 6 );
        fieldText.setEditable( false );
        fieldText.setHorizontalAlignment( JTextField.CENTER );
        fieldText.setBorder( null );
        panel.add( fieldText );
        
        fromField = new JTextField( 5 );
        fromField.setToolTipText( "Starting X range." );
        fromField.setHorizontalAlignment( JTextField.CENTER );
        fromField.setText( "0" );
        fromField.addKeyListener( new KeyAdapter() {
            @Override
            public void keyReleased( KeyEvent e ) {
                if (fromField.getText().isEmpty()) {
                    fromField.setText( "0" );
                }
            }
        } );
        fromField.addFocusListener( new FocusListener() {
            @Override
            public void focusLost( FocusEvent e ) {
                fromField.select( 0, 0 );
            }
            
            @Override
            public void focusGained( FocusEvent e ) {
                fromField.selectAll();
            }
        } );
        panel.add( fromField );
        
        toField = new JTextField( 5 );
        toField.setToolTipText( "Ending X range." );
        toField.setHorizontalAlignment( JTextField.CENTER );
        toField.setText( "0" );
        toField.addKeyListener( new KeyAdapter() {
            @Override
            public void keyReleased( KeyEvent e ) {
                if (toField.getText().isEmpty()) {
                    toField.setText( "0" );
                }
            }
        } );
        toField.addFocusListener( new FocusListener() {
            @Override
            public void focusLost( FocusEvent e ) {
                toField.select( 0, 0 );
            }
            
            @Override
            public void focusGained( FocusEvent e ) {
                toField.selectAll();
            }
        } );
        panel.add( toField );
        
        jumpField = new JTextField( 5 );
        jumpField.setToolTipText( "X jump value." );
        jumpField.setHorizontalAlignment( JTextField.CENTER );
        jumpField.setText( "0" );
        jumpField.addKeyListener( new KeyAdapter() {
            @Override
            public void keyReleased( KeyEvent e ) {
                if (jumpField.getText().isEmpty()) {
                    jumpField.setText( "0" );
                }
            }
        } );
        jumpField.addFocusListener( new FocusListener() {
            @Override
            public void focusLost( FocusEvent e ) {
                jumpField.select( 0, 0 );
            }
            
            @Override
            public void focusGained( FocusEvent e ) {
                jumpField.selectAll();
            }
        } );
        panel.add( jumpField );
        
        return panel;
    }
    
    private void drawLineOnButton( JButton button )
    {
        final int BI_WIDTH  = 155;
        final int BI_HEIGHT =  15;
        BufferedImage lineImage = new BufferedImage( BI_WIDTH, BI_HEIGHT,
                                                     BufferedImage.TYPE_INT_RGB );
        Graphics2D g = (Graphics2D) lineImage.createGraphics();
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON );
        g.setColor( button.getBackground() );
        g.fillRect( 0, 0, BI_WIDTH, BI_HEIGHT );
        g.setColor( plot.color );
        g.setStroke( plot.stroke );
        g.drawLine( 2, BI_HEIGHT/2, BI_WIDTH - 2, BI_HEIGHT/2 );
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
            public void actionPerformed( ActionEvent e ) {
                BasicStroke stroke = (BasicStroke) plot.stroke;
                if (stroke.getDashArray() != null) {
                    plot.line = Line.UNIFORM;
                    plot.stroke = new BasicStroke( plot.lineWidth );
                } else { // Dashed.
                    plot.line = Line.DASHED;
                    plot.stroke = new BasicStroke( plot.lineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                                                   0, new float[]{ 20f, 10f }, 0 );
                }
                drawLineOnButton( buttonLine );
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
                    plot.lineWidth = value;
                    BasicStroke stroke = (BasicStroke) plot.stroke;
                    if (stroke.getDashArray() == null) {
                        plot.line = Line.UNIFORM;
                        plot.stroke = new BasicStroke( plot.lineWidth );
                    } else { // Dashed.
                        plot.line = Line.DASHED;
                        plot.stroke = new BasicStroke( plot.lineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                                                       0, new float[]{ 20f, 10f }, 0 );
                    }
                    drawLineOnButton( buttonLine );
                } catch ( NumberFormatException ex ) {}
                
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    try {
                        saveAndExit();
                    } catch (EvaluationException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        } );
        panel.add( lineWidth );
        
        return panel;
    }
    
    private void drawColorOnButton( JButton button )
    {
        final int BI_WIDTH  = 155;
        final int BI_HEIGHT =  15;
        BufferedImage lineImage = new BufferedImage( BI_WIDTH, BI_HEIGHT, BufferedImage.TYPE_INT_RGB );
        Graphics2D g = (Graphics2D) lineImage.createGraphics();
        g.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        g.setColor( plot.color );
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
                ColorEditDialog dialog = new ColorEditDialog( DIALOG, frame, plot );
                dialog.setVisible( true );
                drawColorOnButton( buttonColor );
                drawLineOnButton( buttonLine );
            }
        } );
        
        return panel;
    }
    
    private void showErrorDialog( String error ) {
        JOptionPane.showMessageDialog( this, error, "error", JOptionPane.ERROR_MESSAGE );
    }
    
    private void saveAndExit() throws EvaluationException
    {
        // Create the function using the expression in the text field.
        Evaluator evaluator = new Evaluator( functionField.getText() );
        
        double from = Double.parseDouble( fromField.getText() );
        double to   = Double.parseDouble( toField.getText() );
        double jump = Double.parseDouble( jumpField.getText() );
        
        if (to < from) {
            showErrorDialog( "Ending point cannot be less then the starting one." );
            return;
        }
        
        if (jump == 0) {
            showErrorDialog( "Jump value cannot be 0." );
            return;
        }
        
        plot.points = new ArrayList<>();
        for (double x = from; x <= to; x += jump) {
            evaluator.putVariable( "x", x );
            double y = evaluator.eval();
            plot.points.add( new Pair<>( x, y ) );
        }
        
        // Save the inserted values.
        plot.title = fieldName.getText();
        if (plot.title.isEmpty()) {
            plot.title = functionField.getText();
        }
        plot.lineWidth = Float.parseFloat( lineWidth.getText() );
        plot.updateValues();
        plotter.addPlot( plot );
        dispose();
    }
    
    @Override
    public void actionPerformed( ActionEvent e )
    {
        if (e.getActionCommand().equals( "Save" )) {
            try {
                saveAndExit();
                return;
            } catch (EvaluationException e1) {
                e1.printStackTrace();
            }
        }
        
        dispose();
    }
}
