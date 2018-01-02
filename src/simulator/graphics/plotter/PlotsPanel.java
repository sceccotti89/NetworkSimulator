/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.plotter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import simulator.graphics.plotter.Plotter.GraphicPlotter;
import simulator.graphics.plotter.Plotter.Plot;

public class PlotsPanel implements MouseListener, FocusListener
{
    private GraphicPlotter plotter;
    private Font font;
    private boolean selected;
    private Rectangle target;
    private Polygon targetOpen;
    private Polygon targetClose;
    private int x;
    private int startY = PAD + POLY_SIZE + 2;
    
    private Dimension size;
    private Color background;
    
    private static final String TEXT = "Plots";
    
    private static final int W_PLOT_OFFSET = 30;
    
    private static final int OFFSET = 30, PAD = 5;
    private static final int POLY_SIZE = 20;
    
    
    
    public PlotsPanel( GraphicPlotter plotter, int width )
    {
        this.plotter = plotter;
        font = new Font( "sans-serif", Font.PLAIN, 12 );
        selected = false;
        background = new Color( 200, 200, 220 );
        this.x = width/* - RIGHT_DISTANCE*/;
        size = new Dimension( 300, getHeight() );
        
        int[] xPoly = { x + PAD + 3, x + PAD + 9, x + PAD + 15 };
        int[] yPoly = { PAD + 4, PAD + 16, PAD + 4 };
        targetOpen = new Polygon( xPoly, yPoly, 3 );
        
        xPoly = new int[]{ x + PAD + 3, x + PAD + 13, x + PAD + 3 };
        yPoly = new int[]{ PAD + 4, PAD + 10, PAD + 16 };
        targetClose = new Polygon( xPoly, yPoly, 3 );
        
        target = new Rectangle( x + PAD + 3, PAD + 4, 12, 12 );
    }
    
    public int getStartXPlot() {
        return x + W_PLOT_OFFSET;
    }
    
    public void setXPosition( int width )
    {
        int x = width - size.width - W_PLOT_OFFSET/* - RIGHT_DISTANCE*/;
        int offset = x - this.x;
        this.x = x;
        target.translate( offset, 0 );
        targetOpen.translate( offset, 0 );
        targetClose.translate( offset, 0 );
    }
    
    public void setWidth( int width )
    {
        size.width = width + W_PLOT_OFFSET;
        setXPosition( plotter.getWidth() );
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    private int getHeight() {
        return POLY_SIZE;
    }
    
    public JCheckBox addPlot( GraphicPlotter plotter, String title )
    {
        int y = startY;
        for (Plot plot : plotter.getPlots()) {
            JCheckBox box = plot.box;
            y = (int) box.getBounds().getMaxY();
        }
        
        int width  = plotter.getWidth( title, plotter.getGraphics() ) + 40;
        int height = plotter.getHeight( title, plotter.getGraphics() ) + 10;
        
        JCheckBox box = new JCheckBox( title, true );
        box.setBackground( new Color( 0, 0, 0, 0 ) );
        box.setBounds( 0, y, width, height );
        size.height = (int) box.getBounds().getMaxY();
        box.addFocusListener( this );
        box.addMouseListener( this );
        selected = true;
        return box;
    }
    
    public void checkClicked( MouseEvent e )
    {
        if (target.contains( e.getPoint() )) {
            selected = !selected;
        }
    }
    
    private void updatePosition( List<Plot> plots )
    {
        int y = startY;
        for (Plot plot : plots) {
            JCheckBox box = plot.box;
            if (plot.visible) {
                box.setLocation( box.getX(), y );
                y = (int) box.getBounds().getMaxY();
            }
        }
        
        if (plots.size() == 0) {
            selected = false;
            size.height = POLY_SIZE;
        } else {
            size.height = y;
        }
    }
    
    @Override
    public void mouseClicked( MouseEvent e )
    {
        if (SwingUtilities.isRightMouseButton( e ) || e.isControlDown()) {
            JCheckBox box = (JCheckBox) e.getSource();
            int idx = 0;
            for (Plot plot : plotter.getPlots()) {
                JCheckBox jBox = plot.box;
                if (jBox == box) {
                    break;
                }
                idx++;
            }
            final int index = idx;
            
            JPopupMenu menu = new JPopupMenu( "Menu Plot" );
            
            JMenuItem new_file = new JMenuItem( "Save as file" );
            new_file.addActionListener( new ActionListener() {
                @Override
                public void actionPerformed( ActionEvent e ) {
                    JFileChooser f = new JFileChooser() {
                        // Generated serial ID.
                        private static final long serialVersionUID = 768969466487289338L;
                        
                        @Override
                        public void approveSelection()
                        {
                            File f = getSelectedFile();
                            if (f.exists() && getDialogType() == SAVE_DIALOG) {
                                int result = JOptionPane.showConfirmDialog( this, "The file exists, overwrite?",
                                                                                  "Existing file", JOptionPane.YES_NO_OPTION );
                                switch (result) {
                                    case JOptionPane.YES_OPTION:
                                        super.approveSelection();
                                        return;
                                    case JOptionPane.NO_OPTION:
                                        return;
                                    case JOptionPane.CLOSED_OPTION:
                                        return;
                                }
                            }
                            super.approveSelection();
                        }        
                    };
                    
                    f.setSelectedFile( new File( box.getText() ) );
                
                    if (f.showSaveDialog( plotter.getFrame() ) == JFileChooser.APPROVE_OPTION) {
                        String dir  = f.getCurrentDirectory().getAbsolutePath();
                        String file = f.getSelectedFile().getName();
                        try {
                            plotter.savePlot( dir, file );
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            } );
            menu.add( new_file );
            
            JMenuItem modify = new JMenuItem( "Modify" );
            modify.addActionListener( new ActionListener() {
                @Override
                public void actionPerformed( ActionEvent e ) {
                    PlotEditDialog dialog = new PlotEditDialog( plotter.getFrame(), plotter.getPlots().get( index ) );
                    dialog.setVisible( true );
                }
            } );
            menu.add( modify );
            
            JMenuItem remove = new JMenuItem( "Remove" );
            remove.addActionListener( new ActionListener() {
                @Override
                public void actionPerformed( ActionEvent e ) {
                    plotter.removePlot( index );
                    updatePosition( plotter.getPlots() );
                }
            } );
            menu.add( remove );
            
            JMenuItem hide = new JMenuItem( "Hide" );
            hide.addActionListener( new ActionListener() {
                @Override
                public void actionPerformed( ActionEvent e ) {
                    plotter.hidePlot( index );
                    updatePosition( plotter.getPlots() );
                }
            } );
            menu.add( hide );
            
            menu.show( e.getComponent(), e.getX(), e.getY() );
            box.requestFocusInWindow();
        }
    }
    
    @Override
    public void mouseReleased( MouseEvent e ) {}
    @Override
    public void mousePressed( MouseEvent e ) {}
    @Override
    public void mouseExited( MouseEvent e ) {}
    @Override
    public void mouseEntered( MouseEvent e ) {}
    
    @Override
    public void focusLost( FocusEvent e )
    {
        JCheckBox box = (JCheckBox) e.getSource();
        box.setBackground( new Color( 0, 0, 0, 0 ) );
    }
    
    @Override
    public void focusGained( FocusEvent e )
    {
        JCheckBox box = (JCheckBox) e.getSource();
        box.setBackground( new Color( 100, 100, 100, 120 ) );
    }

    public void draw( GraphicPlotter plotter, Graphics2D g )
    {
        final int xArea = x + PAD;
        final int yArea = PAD;
        
        g.setColor( background );
        g.fillRect( xArea, yArea, size.width, getHeight() );
        
        g.setStroke( new BasicStroke( 1f ) );
        int h = POLY_SIZE;
        if (selected) {
            g.setColor( Color.GREEN );
            g.fill( targetOpen );
            g.setColor( Color.BLACK );
            g.draw( targetOpen );
            g.setColor( Color.BLACK );
            g.setColor( Color.LIGHT_GRAY );
            g.drawRect( xArea, yArea, size.width, size.height );
        } else {
            g.setColor( Color.RED );
            g.fill( targetClose );
            g.setColor( Color.BLACK );
            g.draw( targetClose );
            g.setColor( Color.LIGHT_GRAY );
        }
        
        g.drawRect( xArea, yArea, size.width, getHeight() );
        
        g.setFont( font );
        FontRenderContext frc = g.getFontRenderContext();
        LineMetrics lm = font.getLineMetrics( TEXT, frc );
        float height = lm.getAscent() + lm.getDescent();
        float x = OFFSET;
        float y = (h + height) / 2 - lm.getDescent();
        g.setColor( Color.BLACK );
        g.drawString( TEXT, this.x + x, PAD + y );
    }
}
