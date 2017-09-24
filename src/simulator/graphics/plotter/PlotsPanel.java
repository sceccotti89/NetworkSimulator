/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.plotter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.image.BufferedImage;
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
    private String text;
    private Font font;
    private boolean selected;
    private BufferedImage open, closed;
    private Rectangle target;
    private int x;
    private int startY = PAD + POLY_SIZE + 2;
    
    private Dimension size;
    private Color background;
    
    private static final int OFFSET = 30, PAD = 5;
    private static final int POLY_SIZE = 20;
    
    public PlotsPanel( final GraphicPlotter plotter, final int x, final int width, final int height )
    {
        this.plotter = plotter;
        text = "Plots";
        font = new Font( "sans-serif", Font.PLAIN, 12 );
        selected = true;
        background = new Color( 200, 200, 220 );
        this.x = x;
        size = new Dimension( width, height );
        createImages();
    }
    
    public void setXPosition( final int x ) {
        this.x = x;
    }
    
    public boolean isSelected() {
        return selected;
    }
    
    private int getHeight() {
        return POLY_SIZE;
    }
    
    private void createImages()
    {
        int h = POLY_SIZE;
        target = new Rectangle( x + PAD, PAD, 20, getHeight() );
        open = new BufferedImage( size.width, h, BufferedImage.TYPE_INT_RGB );
        Graphics2D g2 = open.createGraphics();
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        g2.setPaint( background );
        g2.fillRect( 0, 0, size.width, h );
        int[] x = { 3, 9, 15 };
        int[] y = { 4, 14, 4 };
        Polygon p = new Polygon( x, y, 3 );
        g2.setPaint( Color.green.brighter() );
        g2.fill( p );
        g2.setPaint( Color.blue.brighter() );
        g2.draw( p );
        g2.dispose();
        
        closed = new BufferedImage( size.width, h, BufferedImage.TYPE_INT_RGB );
        g2 = closed.createGraphics();
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        g2.setPaint( background );
        g2.fillRect( 0, 0, size.width, h );
        x = new int[]{ 3, 13, 3};
        y = new int[]{ 4, 10, 16 };
        p = new Polygon( x, y, 3 );
        g2.setPaint( Color.red );
        g2.fill( p );
        g2.setPaint( Color.blue.brighter() );
        g2.draw( p );
        g2.dispose();
    }
    
    public JCheckBox addPlot( final GraphicPlotter plotter, final String title )
    {
        int maxWidth = 0;
        int y = startY;
        for (Plot plot : plotter.getPlots()) {
            JCheckBox box = plot.box;
            y        = (int) box.getBounds().getMaxY();
            maxWidth = (int) Math.max( maxWidth, box.getBounds().getWidth() );
        }
        
        JCheckBox box = new JCheckBox( title, true );
        box.setBackground( new Color( 0, 0, 0, 0 ) );
        box.setBounds( 0, y,
                       Math.max( maxWidth, plotter.getWidth( title, plotter.getGraphics() ) + 40 ),
                       plotter.getHeight( title, plotter.getGraphics() ) + 10 );
        size.height = (int) box.getBounds().getMaxY();
        box.addFocusListener( this );
        box.addMouseListener( this );
        return box;
    }
    
    public void checkClicked( final MouseEvent e )
    {
        if (target.contains( e.getPoint() )) {
            selected = !selected;
        }
    }
    
    private void updatePosition( final List<Plot> plots )
    {
        int y = startY;
        for (Plot plot : plots) {
            JCheckBox box = plot.box;
            box.setLocation( box.getX(), y );
            y = (int) box.getBounds().getMaxY();
        }
        
        if (plots.size() == 0) {
            size.height = POLY_SIZE;
        } else {
            size.height = y;
        }
    }
    
    @Override
    public void mouseClicked( final MouseEvent e )
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
                public void actionPerformed( final ActionEvent e ) {
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
                public void actionPerformed( final ActionEvent e ) {
                    PlotEditDialog dialog = new PlotEditDialog( plotter.getFrame(), plotter.getPlots().get( index ) );
                    dialog.setVisible( true );
                }
            } );
            menu.add( modify );
            
            JMenuItem remove = new JMenuItem( "Remove" );
            remove.addActionListener( new ActionListener() {
                @Override
                public void actionPerformed( final ActionEvent e ) {
                    plotter.removePlot( index );
                    updatePosition( plotter.getPlots() );
                }
            } );
            menu.add( remove );
            
            menu.show( e.getComponent(), e.getX(), e.getY() );
            box.requestFocusInWindow();
        }
    }
    
    @Override
    public void mouseReleased( final MouseEvent e ) {}
    @Override
    public void mousePressed( final MouseEvent e ) {}
    @Override
    public void mouseExited( final MouseEvent e ) {}
    @Override
    public void mouseEntered( final MouseEvent e ) {}
    
    @Override
    public void focusLost( final FocusEvent e )
    {
        JCheckBox box = (JCheckBox) e.getSource();
        box.setBackground( new Color( 0, 0, 0, 0 ) );
    }
    
    @Override
    public void focusGained( final FocusEvent e )
    {
        JCheckBox box = (JCheckBox) e.getSource();
        box.setBackground( new Color( 100, 100, 100, 120 ) );
    }

    public void draw( final GraphicPlotter plotter, final Graphics2D g )
    {
        final int xArea = x + PAD;
        final int yArea = PAD;
        
        g.setColor( Color.LIGHT_GRAY );
        
        int h = POLY_SIZE;
        if (selected) {
            g.drawImage( open, xArea, yArea, plotter );
            g.drawRect( xArea, yArea, size.width, size.height );
        } else {
            g.drawImage( closed, xArea, yArea, plotter );
            g.drawRect( xArea, yArea, size.width, closed.getHeight() );
        }
        
        g.setFont( font );
        FontRenderContext frc = g.getFontRenderContext();
        LineMetrics lm = font.getLineMetrics( text, frc );
        float height = lm.getAscent() + lm.getDescent();
        float x = OFFSET;
        float y = (h + height) / 2 - lm.getDescent();
        g.setColor( Color.BLACK );
        g.drawString( text, this.x + x, PAD + y );
    }
}