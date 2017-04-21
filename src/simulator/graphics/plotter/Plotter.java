/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.plotter;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import simulator.utils.Pair;
import simulator.utils.Utils;

public class Plotter
{
    private JFrame frame;
    
    private float width  = 800;
    private float height = 600;
    
    private String xAxisName = "X";
    private String yAxisName = "Y";
    private Point plotLocation;
    private float xLength;
    private float yLength;
    
    private boolean showFPS = false;
    private boolean showGrid = false;
    
    private Timer timer;
    
    private GraphicPlotter plotter;
    private boolean creatingImage = false;
    
    private long lastUpdate = 0;

    private Theme theme = Theme.BLACK;
    
    
    public enum Axis{ X, Y };
    public enum Theme{ WHITE, BLACK };
    public enum Line { UNIFORM, DASHED };
    
    private PlotterSettings settings;
    
    
    // List of predefined color.
    private static final List<Color> colors = new ArrayList<>( Arrays.asList(
        Color.GREEN,
        Color.RED,
        Color.BLUE,
        Color.ORANGE,
        Color.BLUE,
        Color.CYAN,
        Color.YELLOW,
        Color.MAGENTA
    ) );
    private Set<Color> colorsInUse;
    
    
    // All these values are expressed in pixels.
    private static final float distanceFromAxis = 15;
    private static final float offsetW = 50;
    private static final float offsetH = 50;
    private static final float lengthTick = 5;
    
    
    
    
    public Plotter( final String title, final int width, final int height )
    {
        plotter = new GraphicPlotter();
        createAndShowGUI( title, width, height );
        colorsInUse = new HashSet<>();
    }
    
    public static List<Pair<Double,Double>> readPlot( final String filePlotter ) throws IOException
    {
        BufferedReader reader = new BufferedReader( new FileReader( filePlotter ) );
        List<Pair<Double,Double>> points = new ArrayList<>();
        
        String line;
        while ((line = reader.readLine()) != null) {
            String[] values = line.split( "\\s+|\\t+" );
            Double x = Double.parseDouble( values[0] );
            //Double x = new BigDecimal( values[0] ).doubleValue();
            Double y = Double.parseDouble( values[1] );
            points.add( new Pair<>( x, y ) );
        }
        
        reader.close();
        
        return points;
    }
    
    private void createAndShowGUI( final String title, final int width, final int height )
    {
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (dim.width  - width)/2;
        int y = (dim.height - height)/2;
        
        frame = new JFrame( title );
        frame.setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE );
        
        frame.setLocationRelativeTo( null );
        frame.setBounds( x, y, width, height );
        frame.setMinimumSize( new Dimension( 500, 300 ) );
        
        //frame.setIconImage( ResourceManager.getImage( "icon" ).getImage() );
        
        plotter.setLayout( null );
        plotter.setBounds( 0, 0, width, height );
        
        JMenuBar menuBar = new PlotterMenuBar( this );
        frame.setJMenuBar( menuBar );
        
        frame.setLayout( new BorderLayout() );
        frame.getContentPane().add( plotter );
        //frame.setLocationByPlatform( true );
        
        frame.pack();
        frame.setVisible( false );
        
        frame.addWindowListener( new WindowAdapter() {
            @Override
            public void windowClosing( final WindowEvent e ) {
                if (e.getID() == WindowEvent.WINDOW_CLOSING) {
                    dispose();
                }
            }
        });
        
        // By default a timer of 30 FPS is added.
        setTimer( 30 );
        
        settings = new PlotterSettings();
    }
    
    public JFrame getFrame() {
        return frame;
    }
    
    public String getTitle() {
        return frame.getTitle();
    }
    
    /**
     * Set the timer of the animation.</br>
     * By default this animator makes use of a timer of 30 FPS,
     * only for the animation repainting.
     * 
     * @param FPS    frame per second
    */
    public void setTimer( final int FPS )
    {
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed( final ActionEvent e ) {
                plotter.repaint();
            }
        };
        setTimer( FPS, listener );
    }
    
    public void addPlot( final String filePlotter, final Color color, final String title ) throws IOException {
        addPlot( readPlot( filePlotter ), color, Line.UNIFORM, title );
    }
    
    public void addPlot( final String filePlotter, final Color color, final Line line, final String title ) throws IOException {
        addPlot( readPlot( filePlotter ), color, line, title );
    }
    
    public void addPlot( final List<Pair<Double,Double>> points, final Color color, final String title ) {
        addPlot( points, color, Line.UNIFORM, title );
    }
    
    public void addPlot( final List<Pair<Double,Double>> points, final Color color, final Line line, final String title ) {
        plotter.addPlot( points, color, line, title );
    }
    
    /**
     * Set the timer of the animation with a specific action listener.</br>
     * By default the FPS are fixed to 30, just for the animation repainting.
     * 
     * @param FPS         frame per second
     * @param listener    the associated event listener
    */
    public void setTimer( final int FPS, final ActionListener listener ) {
        timer = new Timer( 1000 / FPS, listener );
    }
    
    public void setVisible( final boolean visible )
    {
        plotter.setVisible( visible );
        frame.setVisible( visible );
        if (visible) {
            timer.restart();
        } else {
            timer.stop();
        }
    }
    
    public Plotter setTicks( final Axis axis, final int ticks ) {
        return setTicks( axis, ticks, 1 );
    }
    
    public Plotter setTicks( final Axis axis, final int ticks, final int interval )
    {
        if (axis == Axis.X) {
            settings._xNumTicks = ticks;
            settings.xTickInterval = interval;
        } else {
            settings._yNumTicks = ticks;
            settings.yTickInterval = interval;
        }
        return this;
    }
    
    /**
     * Set the range of the plotter.
     * Useful when you have multiple lines to plot.
     * 
     * @param axis    
     * @param from    
     * @param to      
    */
    public void setRange( final Axis axis, final double from, final double to )
    {
        if (to < from) {
            throw new IllegalArgumentException( "from (" + from + ") cannot be greater than to (" + to + ")." );
        }
        
        if (axis == Axis.X) {
            settings._range.minX = from;
            settings._range.maxX = to;
        } else {
            settings._range.minY = from;
            settings._range.maxY = to;
        }
    }
    
    public Plotter setTheme( final Theme theme ) {
        this.theme = theme;
        return this;
    }
    
    public Theme getTheme() {
        return theme;
    }
    
    public Plotter showGrid( final boolean show ) {
        showGrid = show;
        return this;
    }
    
    public boolean isShowingGrid() {
        return showGrid;
    }
    
    public Plotter showFPS( final boolean show ) {
        showFPS = show;
        return this;
    }
    
    public boolean isShowingFPS() {
        return showFPS;
    }
    
    public Plotter setAxisName( final String xName, final String yName )
    {
        xAxisName = xName;
        yAxisName = yName;
        return this;
    }
    
    public Plotter setScaleX( final double scale ) {
        settings.xScale = scale;
        return this;
    }
    
    public Plotter setScaleY( final double scale ) {
        settings.yScale = scale;
        return this;
    }
    
    /**
     * Create an image specifying only the title.
    */
    public void createImage( final String fileName ) throws IOException
    {
        int index = fileName.lastIndexOf( '.' );
        if (index >= 0) {
            String extension = fileName.substring( index + 1 );
            createImage( fileName.substring( 0, index ), extension );
        } else {
            createImage( fileName, ".png" );
        }
    }
    
    /**
     * Create an image specifying the title and its extension.
    */
    public void createImage( final String fileName, final String fileExtension ) throws IOException
    {
        Utils.checkDirectory( Utils.IMAGES_DIR );
        
        Dimension size = plotter.getSize();
        BufferedImage image = new BufferedImage( size.width, size.height, BufferedImage.TYPE_INT_RGB );
        Graphics g = image.getGraphics();
        creatingImage = true;
        plotter.paintComponent( g );
        creatingImage = false;
        File outputfile = new File( Utils.IMAGES_DIR + fileName + "." + fileExtension );
        ImageIO.write( image, fileExtension, outputfile );
        
        System.out.println( "Image \"" + fileName + "." + fileExtension + "\" saved in \"" + Utils.IMAGES_DIR + "\"." );
    }
    
    public PlotterSettings getsettings() {
        return settings;
    }
    
    
    
    public static  class PlotterSettings
    {
        public int xTickInterval = 1;
        public int yTickInterval = 1;
        public int _xNumTicks = Integer.MAX_VALUE;
        public int _yNumTicks = Integer.MAX_VALUE;
        public double xScale = 1d;
        public double yScale = 1d;
        public Range _range = new Range();
    }
    
    public static class Range
    {
        private int maxPoints;
        private double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        private double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        
        public Range() {
            // Empty constructor.
        }
        
        public Range( final int maxPoints,
                      final double minX, final double maxX,
                      final double minY, final double maxY )
        {
            this.maxPoints = maxPoints;
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
        }
        
        public void setMinX( final double minX ) { this.minX = minX; }
        public void setMaxX( final double maxX ) { this.maxX = maxX; }
        public void setMinY( final double minY ) { this.minY = minY; }
        public void setMaxY( final double maxY ) { this.maxY = maxY; }
        
        public double getMinX() { return minX; }
        public double getMaxX() { return maxX; }
        public double getMinY() { return minY; }
        public double getMaxY() { return maxY; }
        
        public double getXRange() {
            return maxX - minX;
        }
        
        public double getYRange() {
            return maxY - minY;
        }
        
        public boolean checkRange( final double x, final double y ) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY;
        }
    }
    
    private static class Plot
    {
        private List<Pair<Double,Double>> points;
        private Color color;
        private Line line;
        
        public Plot( final List<Pair<Double,Double>> points,
                     final Color color, final Line line ) {
            this.points = points;
            this.color = color;
            this.line = line;
        }
    }
    
    private class GraphicPlotter extends JPanel implements MouseMotionListener
    {
        /** Generated serial ID. */
        private static final long serialVersionUID = -1157813231215489004L;
        private List<Plot> _plots;
        private Point mouse = new Point( 0, 0 );
        private JTextArea description;
        private List<JCheckBox> legendBox;
        private Rectangle area;
        
        private long currentFPS = 0;
        private long timeElapsed = 0;
        
        private static final int MAX_TEXT_LENGTH = 170;
        private static final long SECOND = 1000L;
        private static final int pointRadius = 10;
        private static final double ROUNDNESS = 0.0001d;
        
        
        
        
        public GraphicPlotter()
        {
            _plots = new ArrayList<>();
            legendBox = new ArrayList<>();
            
            setDoubleBuffered( true );
            addMouseMotionListener( this );
            
            description = new JTextArea();
            description.addMouseMotionListener( this );
            add( description );
        }
        
        private String makeBoxTitle( final String title )
        {
            final Graphics g = getGraphics();
            final int pointsLength = getWidth( "...", g );
            String text = title;
            if (getWidth( title, getGraphics() ) > MAX_TEXT_LENGTH) {
                for (int i = title.length() - 1; i >= 0; i--) {
                    text = text.substring( 0, text.length() - 1 );
                    if (getWidth( text, g ) + pointsLength <= MAX_TEXT_LENGTH) {
                        return text + "...";
                    }
                }
            }
            return text;
        }
        
        public void addPlot( final List<Pair<Double,Double>> points,
                             final Color color, final Line line,
                             final String title )
        {
            _plots.add( new Plot( points, chooseColor( color ), line ) );
            
            int maxWidth = 0;
            int startY = 100;
            for (JCheckBox box : legendBox) {
                startY   = (int) box.getBounds().getMaxY();
                maxWidth = (int) Math.max( maxWidth, box.getBounds().getWidth() );
            }
            
            String text = makeBoxTitle( title );
            JCheckBox box = new JCheckBox( text, true );
            box.setBackground( new Color( 0,0,0,0 ) );
            box.setFocusable( false );
            box.setBounds( getWidth() - 200, startY,
                           Math.max( maxWidth, getWidth( text, getGraphics() ) + 40 ),
                           getHeight( text, getGraphics() ) + 10 );
            legendBox.add( box );
            add( box );
        }
        
        private Color chooseColor( final Color selected )
        {
            if (selected != null) {
                // Remove the selected color.
                for (int i = 0; i < colors.size(); i++) {
                    if (colors.get( i ).equals( selected )) {
                        colors.remove( i );
                        break;
                    }
                }
                colorsInUse.add( selected );
                return selected;
            } else {
                if (!colors.isEmpty()) {
                    colorsInUse.add( colors.get( 0 ) );
                    return colors.remove( 0 );
                } else {
                    // Create a random color.
                    while (true) {
                        Color color = new Color( (int) (Math.random() * 256),
                                                 (int) (Math.random() * 256),
                                                 (int) (Math.random() * 256) );
                        if (!colorsInUse.contains( color )) {
                            colorsInUse.add( color );
                            return color;
                        }
                    }
                }
            }
        }
        
        private void setPlotLocation( final float x, final float y )
        {
            plotLocation = new Point( (int) x, (int) y );
            xLength = width - offsetW - plotLocation.x;
            
            area = new Rectangle( plotLocation.x, (int) (plotLocation.y - yLength), (int) xLength, (int) yLength );
        }
        
        private void drawGrid( final Axis axe, final Point p, final Graphics2D g )
        {
            final float lines = 25;
            final float spaces = lines * 2 - 1;
            if (axe == Axis.Y) {
                final float dashLength = xLength / spaces;
                Stroke stroke = new BasicStroke( 1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{ dashLength, dashLength }, 0 );
                g.setStroke( stroke );
                g.drawLine( p.x, (int) p.getY(), (int) (p.x + xLength), (int) p.getY() );
            } else {
                final float dashLength = yLength / spaces;
                Stroke stroke = new BasicStroke( 1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{ dashLength, dashLength }, 0 );
                g.setStroke( stroke );
                g.drawLine( p.x, (int) p.getY(), p.x, (int) (p.y - yLength) );
            }
        }
        
        private String stringValue( final double val, final boolean round )
        {
            String value = String.format( "%.12f", val );
            if (value.contains( "," )) {
                int index = value.indexOf( ',' );
                if (round) {
                    // Return the integer part of the number
                    // only if the first 5 numbers of the mantissa are all 0.
                    boolean allZeros = true;
                    for (int i = index+1; i < Math.min( 5, value.length() ); i++) {
                        if (value.charAt( i ) != '0') {
                            allZeros = false;
                            break;
                        }
                    }
                    if (allZeros) {
                        return value.substring( 0, index );
                    }
                }
                value = value.substring( 0, Math.min( value.indexOf( ',' ) + 3, value.length() ) );
            }
            
            return value;
        }
        
        private int getWidth( final String arg, final Graphics g )
        {
            FontMetrics font = g.getFontMetrics();
            return (int) font.getStringBounds( arg, g ).getWidth();
        }
        
        private int getHeight( final String arg, final Graphics g )
        {
            FontMetrics font = g.getFontMetrics();
            return (int) font.getStringBounds( arg, g ).getHeight();
        }
        
        private double round( final double value ) {
            if (Math.ceil( value ) - value < ROUNDNESS) {
                return Math.ceil( value );
            } else {
                return value;
            }
        }
        
        private double scaleX( double value ) {
            return round( value / settings.xScale );
        }
        
        private double scaleY( double value ) {
            return round( value / settings.yScale );
        }
        
        private void showFPS( final Graphics2D g )
        {
            // Update the FPS every second.
            long now = System.currentTimeMillis();
            if (lastUpdate != 0 && now - lastUpdate > 0) {
                long delta = now - lastUpdate;
                timeElapsed += delta;
                if (timeElapsed >= SECOND) {
                    timeElapsed = timeElapsed - SECOND;
                    currentFPS = 1000 / delta;
                }
                
                if (showFPS) {
                    g.setColor( (theme == Theme.WHITE) ? Color.BLACK : Color.WHITE );
                    g.drawString( "FPS: " + currentFPS, 15, 15 );
                }
            }
            lastUpdate = now;
        }
        
        private void addPointInfo( final Pair<Double,Double> point, final double centerX, final double centerY, final Graphics2D g )
        {
            description.setBackground( Color.YELLOW );
            description.setVisible( true );
            String info = "  X: " + stringValue( scaleX( point.getFirst() ), true ) +
                          "  Y: " + stringValue( scaleY( point.getSecond() ), false ) + "  ";
            description.setText( info );
            int dWidth  = getWidth( info, g );
            int dHeight = getHeight( info, g );
            double startX = centerX - dWidth / 2;
            if (startX + dWidth > getWidth()) {
                // If the label is too large change the initial position.
                startX = getWidth() - dWidth;
            }
            description.setBounds( (int) startX, (int) centerY - pointRadius/2 - dHeight, dWidth, dHeight );
        }

        private boolean drawPlot( final Graphics2D g, final Range range, final Plot plot )
        {
            Stroke stroke;
            if (plot.line == Line.UNIFORM) {
                stroke = new BasicStroke( 2f );
            } else { // Dashed.
                stroke = new BasicStroke( 2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{ 20f, 10f }, 0 );
            }
            g.setColor( (theme == Theme.BLACK) ? Color.WHITE : Color.BLACK );
            
            boolean drawCircle = true;
            Pair<Double,Double> point;
            Point p = new Point( plotLocation.x, plotLocation.y );
            if(plot.points.size() > 0) {
                // Get the starting position.
                double x = plotLocation.getX() + ((plot.points.get( 0 ).getFirst() - range.minX) * (xLength / range.getXRange()));
                double y = plotLocation.getY() - ((plot.points.get( 0 ).getSecond() - range.minY) * (yLength / range.getYRange()));
                p.setLocation( x, y );
            }
            
            // Set the area to draw the points.
            g.setClip( area );
            
            for (int i = 1; i < plot.points.size(); i++) {
                try {
                    point = plot.points.get( i );
                } catch( IndexOutOfBoundsException e ){
                    continue;
                }
                
                double x = plotLocation.getX() + ((point.getFirst() - range.minX) * (xLength / range.getXRange()));
                double y = plotLocation.getY() - ((point.getSecond() - range.minY) * (yLength / range.getYRange()));
                
                // Draw the point.
                g.setColor( plot.color );
                g.setStroke( stroke );
                
                g.drawLine( (int) p.getX(), (int) p.getY(), (int) x, (int) y );
                
                //System.out.println( "SECOND: " + point.getSecond() );
                if (range.checkRange( point.getFirst(), point.getSecond() )) {
                    if (drawCircle) {
                        Ellipse2D circle = new Ellipse2D.Double( x - pointRadius/2, y - pointRadius/2, pointRadius, pointRadius );
                        if (circle.contains( mouse.x, mouse.y )) {
                            drawCircle = false;
                            g.setColor( (theme == Theme.BLACK) ? Color.WHITE : Color.GRAY );
                            g.drawOval( (int) circle.getX(), (int) circle.getY(), pointRadius, pointRadius );
                            addPointInfo( point, circle.getCenterX(), circle.getCenterY(), g );
                        }
                    }
                }
                
                p.setLocation( x, y );
            }
            
            return !drawCircle;
        }
        
        private boolean isInsideRange( final double value, final double left, final double right ) {
            return value >= left && value <= right;
        }
        
        private void drawTicks( final Graphics2D g, final Range range )
        {
            g.setStroke( new BasicStroke( 1f ) );
            g.setColor( (theme == Theme.BLACK) ? Color.WHITE : Color.BLACK );
            
            // Get the number of ticks.
            int xNumTicks = Math.min( range.maxPoints, settings._xNumTicks );
            final float xTicksOffset = xLength / xNumTicks;
            float xTickPosition = xTicksOffset;
            
            int yNumTicks = Math.min( range.maxPoints, settings._yNumTicks );
            final float yTicksOffset = yLength / yNumTicks;
            float yTickPosition = yTicksOffset;
            
            if (xNumTicks > 0) {
                float xTick = (float) plotLocation.getX();
                double xValue = range.minX;
                String xTickValue = stringValue( scaleX( xValue ), true );
                g.drawString( xTickValue, xTick - getWidth( xTickValue, g )/2, plotLocation.y + getHeight( xTickValue, g ) );
            }
            
            // Draw the lines with the respective X and Y values of the tick.
            for (int i = 0; i < xNumTicks; i++) {
                if ((i+1) % settings.xTickInterval == 0) {
                    float xTick = (float) plotLocation.getX() + xTickPosition;
                    if (i < xNumTicks - 1) {
                        if (showGrid) {
                            drawGrid( Axis.X, new Point( (int) xTick, plotLocation.y ), g );
                        }
                        g.drawLine( (int) xTick, plotLocation.y, (int) xTick, (int) (plotLocation.getY() - lengthTick) );
                    }
                    
                    double xValue = range.minX + (range.getXRange() / xNumTicks) * (i+1);
                    String xTickValue = stringValue( scaleX( xValue ), true );
                    g.drawString( xTickValue, xTick - getWidth( xTickValue, g )/2, plotLocation.y + getHeight( xTickValue, g ) );
                }
                
                xTickPosition += xTicksOffset;
            }
            
            if (yNumTicks > 0) {
                float yTick = (float) plotLocation.getY();
                double yValue = range.minY;
                String yTickValue = stringValue( scaleY( yValue ), false );
                g.drawString( yTickValue, plotLocation.x - getWidth( yTickValue, g ) - lengthTick, yTick + getHeight( yTickValue, g )/2 - 2f );
            }
            
            for (int i = 0; i < yNumTicks; i++) {
                if ((i+1) % settings.yTickInterval == 0) {
                    float yTick = (float) plotLocation.getY() - yTickPosition;
                    if (i < yNumTicks - 1) {
                        if (showGrid) {
                            drawGrid( Axis.Y, new Point( plotLocation.x, (int) yTick ), g );
                        }
                        g.drawLine( plotLocation.x, (int) yTick, (int) (plotLocation.getX() + lengthTick), (int) yTick );
                    }
                    
                    double yValue = range.minY + (range.getYRange() / yNumTicks) * (i+1);
                    String yTickValue = stringValue( scaleY( yValue ), false );
                    g.drawString( yTickValue, plotLocation.x - getWidth( yTickValue, g ) - lengthTick, yTick + getHeight( yTickValue, g )/2 - 2f );
                }
                
                yTickPosition += yTicksOffset;
            }
        }

        private void drawAxis( final Graphics2D g, final Range range )
        {
            g.setStroke( new BasicStroke( 1f ) );
            g.setColor( (theme == Theme.BLACK) ? Color.WHITE : Color.BLACK );
            
            int maxWidth = getWidth( stringValue( range.maxY, true ), g );
            if (plotLocation.getX() - maxWidth - lengthTick - offsetW < 0) {
                setPlotLocation( offsetW + maxWidth + lengthTick, height - offsetH );
            }
            
            // Draw X and Y axis with their names.
            g.drawRect( plotLocation.x, (int) (plotLocation.y - yLength), (int) xLength, (int) yLength );
            g.drawString( xAxisName, (int) (plotLocation.getX() + xLength/2 - getWidth( xAxisName, g )/2),
                                       (int) (plotLocation.getY() + offsetH / 2 + getHeight( xAxisName, g )) );
            AffineTransform backupTransform = g.getTransform();
            g.rotate( -(Math.PI / 2), offsetW, plotLocation.getY() - yLength / 2 );
            g.drawString( yAxisName, (int) (offsetW - getWidth( yAxisName, g )/2),
                                     (int) (plotLocation.getY() - yLength / 2 - distanceFromAxis*2) );
            g.setTransform( backupTransform );
            
            // Draw ticks.
            drawTicks( g, range );
        }
        
        private void drawLegend( final Graphics2D g )
        {
            g.setStroke( new BasicStroke( 2f ) );
            
            // Draw the legend.
            int startY = 0;
            int saveStartY = 0;
            int index = 0;
            final int length = 70;
            for (Plot plot : _plots) {
                JCheckBox box = legendBox.get( index++ );
                Rectangle bounds = box.getBounds();
                box.setForeground( (theme == Theme.BLACK) ? Color.WHITE : Color.BLACK );
                box.setBounds( getWidth() - 300, startY, bounds.width, bounds.height );
                
                // Draw the associated line.
                startY = (int) bounds.getMaxY();
                Point p = box.getLocation();
                Dimension size = box.getSize();
                
                if (box.isSelected()) {
                    if (creatingImage) {
                        if (saveStartY == 0) {
                            saveStartY = p.y;
                        }
                        g.setColor( (theme == Theme.WHITE) ? Color.BLACK : Color.WHITE );
                        String title = box.getText();
                        int width  = getWidth( title, g );
                        int height = getHeight( title, g );
                        g.drawString( box.getText(), p.x + size.width - width - 10, saveStartY + height );
                    }
                    saveStartY += size.height;
                }
                
                // FIXME non mostra la linea del plot.
                g.setColor( plot.color );
                g.drawLine( p.x + size.width, p.y + size.height/2 - 1,
                            p.x + size.width + length, p.y + size.height/2 - 1 );
            }
        }
        
        /**
         * Gets the range for the X and Y values.
        */
        private Range getRange()
        {
            double maxX = settings._range.maxX, maxY = settings._range.maxY;
            double minX = settings._range.minX, minY = settings._range.minY;
            boolean getMinX = minX == Double.MAX_VALUE;
            boolean getMaxX = maxX == Double.MIN_VALUE;
            boolean getMinY = minY == Double.MAX_VALUE;
            boolean getMaxY = maxY == Double.MIN_VALUE;
            
            for (Plot plot : _plots) {
                List<Pair<Double,Double>> points = plot.points;
                if (points.size() > 0) {
                    if (getMinX)  minX = Math.min( minX, points.get( 0 ).getFirst() );
                    if (getMaxX)  maxX = Math.max( maxX, points.get( points.size() - 1 ).getFirst() );
                    for (Pair<Double,Double> point : points) {
                        if (getMinY)  minY = Math.min( minY, point.getSecond() );
                        if (getMaxY)  maxY = Math.max( maxY, point.getSecond() );
                    }
                }
            }
            
            //System.out.println( "MIN_X: " + minX + ", MAX_X: " + maxX );
            //System.out.println( "MIN_Y: " + minY + ", MAX_Y: " + maxY );
            
            int maxPoints = 0;
            for (Plot plot : _plots) {
                List<Pair<Double,Double>> points = plot.points;
                int rangePoints = 0;
                for (Pair<Double,Double> point : points) {
                    // Check if the point is inside the range.
                    if (isInsideRange( point.getFirst(), minX, maxX ) && 
                        isInsideRange( point.getSecond(), minY, maxY ) ) {
                        rangePoints++;
                    }
                }
                
                maxPoints = Math.max( maxPoints, rangePoints );
            }
            
            return new Range( maxPoints, minX, maxX, minY, maxY );
        }

        @Override
        protected void paintComponent( final Graphics g )
        {
            super.paintComponent( g );
            
            Graphics2D g2d = (Graphics2D) g.create();
            
            if (!creatingImage) {
                showFPS( g2d );
            }
            
            width = getSize().width;
            height = getSize().height;
            setPlotLocation( offsetW + lengthTick, height - offsetH );
            yLength = height - offsetH * 2;
            
            g2d.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                                  RenderingHints.VALUE_ANTIALIAS_ON );
            
            setBackground( (theme == Theme.BLACK) ? Color.black : Color.white );
            
            Range range = getRange();
            drawAxis( g2d, range );
            
            // Draw the plots.
            int index = 0;
            boolean selectedPoint = false;
            for (Plot plot : _plots) {
                if (legendBox.get( index++ ).isSelected()) {
                    selectedPoint |= drawPlot( g2d, range, plot );
                }
            }
            
            if (!selectedPoint) {
                description.setVisible( false );
            }
            
            // Draw legend.
            drawLegend( g2d );
            
            g2d.dispose();
        }
        
        @Override
        public Dimension getPreferredSize() {
            return getSize();
        }

        @Override
        public void mouseDragged( final MouseEvent event ) {
            // Empty body.
        }

        @Override
        public void mouseMoved( final MouseEvent event ) {
            mouse = event.getPoint();
        }
    }
    
    public void dispose() {
        frame.dispose();
    }
}