/**
 * @author Stefano Ceccotti
*/

package simulator.graphics.plotter;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.WindowConstants;

import simulator.utils.Pair;
import simulator.utils.Utils;
import simulator.utils.resources.ResourceLoader;

public class Plotter extends WindowAdapter implements ActionListener
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
    private PlotterMenuBar menuBar;
    private boolean creatingImage = false;
    
    private long lastUpdate = 0;

    private Theme theme = Theme.BLACK;
    
    
    private boolean XticksSettedByTheUser = false;
    private boolean YticksSettedByTheUser = false;
    // Max number of ticks drawn, unless specified by the user.
    private static final int MAX_TICKS = 30;
    
    
    public enum Axis{ X, Y };
    public enum Theme{ WHITE, BLACK };
    public enum Line { UNIFORM, DASHED };
    
    private PlotterSettings settings;
    
    
    // List of predefined color.
    protected static final List<Color> colors = new ArrayList<>( Arrays.asList(
        Color.GREEN,
        Color.RED,
        Color.BLUE,
        Color.ORANGE,
        Color.DARK_GRAY,
        Color.CYAN,
        Color.YELLOW,
        Color.MAGENTA,
        Color.WHITE,
        Color.BLACK,
        Color.GRAY,
        Color.LIGHT_GRAY
    ) );
    private Set<Color> colorsInUse;
    
    
    // All these values are expressed in pixels.
    private static final float distanceFromAxis = 15;
    private static final float offsetW = 50;
    private static final float offsetH = 50;
    private static final float lengthTick = 5;
    
    
    
    
    /**
     * Constructs a plotter with the default size (800 x 600).
    */
    public Plotter() {
        this( "Plotter", 800, 600 );
    }
    
    public Plotter( String title, int width, int height )
    {
        plotter = new GraphicPlotter();
        createAndShowGUI( title, width, height );
        colorsInUse = new HashSet<>();
    }
    
    public static List<Pair<Double,Double>> readPlot( String filePlotter ) throws IOException
    {
        InputStream stream = ResourceLoader.getResourceAsStream( filePlotter );
        BufferedReader reader = new BufferedReader( new InputStreamReader( stream ) );
        List<Pair<Double,Double>> points = new ArrayList<>();
        
        String line;
        while ((line = reader.readLine()) != null) {
            String[] values = line.split( "\\s+|\\t+" );
            Double x = Double.parseDouble( values[0] );
            Double y = Double.parseDouble( values[1] );
            points.add( new Pair<>( x, y ) );
        }
        
        reader.close();
        
        return points;
    }
    
    private void createAndShowGUI( String title, int width, int height )
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
        
        menuBar = new PlotterMenuBar( this );
        frame.setJMenuBar( menuBar );
        
        frame.setLayout( new BorderLayout() );
        frame.getContentPane().add( plotter );
        //frame.setLocationByPlatform( true );
        
        frame.pack();
        frame.setVisible( false );
        
        frame.addWindowListener( this );
        
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
     * just for the animation repainting.
     * 
     * @param FPS    frame per second
    */
    public void setTimer( int FPS )
    {
        ActionListener listener = this;
        setTimer( FPS, listener );
    }
    
    /**
     * Set the timer of the animation with a specific action listener.</br>
     * By default the FPS are fixed to 30, just for the animation repainting.
     * 
     * @param FPS         frame per second
     * @param listener    the associated event listener
    */
    public void setTimer( int FPS, ActionListener listener ) {
        timer = new Timer( 1000 / FPS, listener );
    }
    
    public void addPlot( String filePlotter, String title ) throws IOException {
        addPlot( readPlot( filePlotter ), null, Line.UNIFORM, title );
    }

    public void addPlot( String filePlotter, Color color, String title ) throws IOException {
        addPlot( readPlot( filePlotter ), color, Line.UNIFORM, title );
    }
    
    public void addPlot( String filePlotter, Line line, String title ) throws IOException {
        addPlot( readPlot( filePlotter ), null, line, title );
    }
    
    public void addPlot( String filePlotter, Color color, Line line, String title ) throws IOException {
        addPlot( readPlot( filePlotter ), color, line, title );
    }
    
    public void addPlot( List<Pair<Double,Double>> points, String title ) {
        addPlot( points, null, Line.UNIFORM, title );
    }
    
    public void addPlot( List<Pair<Double,Double>> points, Line line, String title ) {
        addPlot( points, null, line, title );
    }
    
    public void addPlot( List<Pair<Double,Double>> points, Color color, String title ) {
        addPlot( points, color, Line.UNIFORM, title );
    }
    
    public void addPlot( List<Pair<Double,Double>> points, Color color, Line line, String title ) {
        plotter.addPlot( points, color, line, title );
    }
    
    public void addPlot( Plot plot ) {
        plotter.addPlot( plot.points, plot.color,plot.line , plot.title );
    }
    
    public void savePlot( String dir, String file ) throws IOException {
        plotter.savePlot( dir, file );
    }

    public void setVisible( boolean visible )
    {
        menuBar.updateSelectedValue();
        plotter.setVisible( visible );
        frame.setVisible( visible );
        frame.requestFocusInWindow();
        if (visible) {
            timer.restart();
        } else {
            timer.stop();
        }
    }
    
    public Plotter setTicks( Axis axis, int ticks ) {
        return setTicks( axis, ticks, 1 );
    }
    
    public Plotter setTicks( Axis axis, int ticks, int interval )
    {
        if (axis == Axis.X) {
            settings._xNumTicks = ticks;
            settings.xTickInterval = interval;
            XticksSettedByTheUser = true;
        } else {
            settings._yNumTicks = ticks;
            settings.yTickInterval = interval;
            YticksSettedByTheUser = true;
        }
        return this;
    }
    
    /**
     * Sets the range of the plotter.
     * Useful when you have multiple lines to plot.
     * 
     * @param axis    
     * @param from    
     * @param to      
    */
    public void setRange( Axis axis, double from, double to )
    {
        if (to < from) {
            throw new IllegalArgumentException( "from (" + from + ") cannot be greater than to (" + to + ")." );
        }
        
        if (axis == Axis.X) {
            settings._settedXRangeByUser = true;
            settings._range.minX = from;
            settings._range.maxX = to;
        } else {
            settings._settedYRangeByUser = true;
            settings._range.minY = from;
            settings._range.maxY = to;
        }
    }
    
    public Plotter setTheme( Theme theme ) {
        this.theme = theme;
        return this;
    }
    
    public Theme getTheme() {
        return theme;
    }
    
    public Plotter showGrid( boolean show ) {
        showGrid = show;
        return this;
    }
    
    public boolean isShowingGrid() {
        return showGrid;
    }
    
    public Plotter showFPS( boolean show ) {
        showFPS = show;
        return this;
    }
    
    public boolean isShowingFPS() {
        return showFPS;
    }
    
    public Plotter setAxisName( String xName, String yName )
    {
        xAxisName = xName;
        yAxisName = yName;
        return this;
    }
    
    public Plotter setScaleX( double scale ) {
        settings.xScale = scale;
        return this;
    }
    
    public Plotter setScaleY( double scale ) {
        settings.yScale = scale;
        return this;
    }
    
    /**
     * Create an image specifying only the title.
     * 
     * @param fileName     name of the file
     * @param directory    name of the directory
    */
    public void createImage( String fileName, String directory ) throws IOException
    {
        int index = fileName.lastIndexOf( '.' );
        if (index >= 0) {
            String extension = fileName.substring( index + 1 );
            createImage( fileName.substring( 0, index ), extension, directory );
        } else {
            createImage( fileName, "png", directory );
        }
    }
    
    /**
     * Create an image specifying the title and its extension.
     * 
     * @param fileName         name of the file
     * @param fileExtension    extension of the file
     * @param directory        name of the directory
    */
    public void createImage( String fileName, String fileExtension, String directory ) throws IOException
    {
        Utils.checkDirectory( directory );
        
        Dimension size = plotter.getSize();
        BufferedImage image = new BufferedImage( size.width, size.height, BufferedImage.TYPE_INT_RGB );
        Graphics g = image.getGraphics();
        creatingImage = true;
        plotter.paintComponent( g );
        creatingImage = false;
        File outputfile = new File( directory + "\\" + fileName + "." + fileExtension );
        if (ImageIO.write( image, fileExtension, outputfile )) {
            System.out.println( "Image \"" + fileName + "." + fileExtension + "\" saved in \"" + directory + "\"." );
        }
    }
    
    public PlotterSettings getSettings() {
        return settings;
    }
    
    @Override
    public void actionPerformed( ActionEvent e ) {
        plotter.repaint();
    }
    
    @Override
    public void windowClosing( WindowEvent e )
    {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            dispose();
        }
    }
    
    private void dispose() {
        frame.dispose();
        timer.stop();
    }



    public static  class PlotterSettings
    {
        public boolean _settedXRangeByUser = false;
        public boolean _settedYRangeByUser = false;
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
        private int maxXPoints;
        private int maxYPoints;
        private double minX = Double.MAX_VALUE, maxX = Double.MIN_VALUE;
        private double minY = Double.MAX_VALUE, maxY = Double.MIN_VALUE;
        
        public Range() {
            // Empty constructor.
        }
        
        public Range( int maxXPoints, int maxYPoints,
                      double minX, double maxX,
                      double minY, double maxY )
        {
            this.maxXPoints = maxXPoints;
            this.maxYPoints = maxYPoints;
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
        }
        
        public void setMinX( double minX ) { this.minX = minX; }
        public void setMaxX( double maxX ) { this.maxX = maxX; }
        public void setMinY( double minY ) { this.minY = minY; }
        public void setMaxY( double maxY ) { this.maxY = maxY; }
        
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
        
        public boolean checkRange( double x, double y ) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY;
        }
    }
    
    public static class Plot
    {
        protected String title;
        protected List<Pair<Double,Double>> points;
        protected Color color;
        protected Line line;
        protected float lineWidth;
        protected Stroke stroke;
        protected JCheckBox box;
        
        public Plot( String title,
                     List<Pair<Double,Double>> points,
                     Color color, Line line,
                     float lineWidth, JCheckBox box )
        {
            this.title = title;
            this.points = points;
            this.color = color;
            this.line = line;
            this.lineWidth = lineWidth;
            this.box = box;
            
            updateValues();
        }
        
        public void setCheckBox( JCheckBox box ) {
            this.box = box;
        }
        
        public void updateValues()
        {
            if (box != null) {
                box.setText( title );
            }
            
            if (line == Line.UNIFORM) {
                stroke = new BasicStroke( lineWidth );
            } else { // Dashed.
                stroke = new BasicStroke( lineWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{ 20f, 10f }, 0 );
            }
        }
        
        @Override
        public Plot clone()
        {
            Color nColor = new Color( color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() );
            Plot plot = new Plot( title, points, nColor, line, lineWidth, box );
            return plot;
        }
    }
    
    protected class GraphicPlotter extends JPanel implements MouseMotionListener, ComponentListener
    {
        /** Generated serial ID. */
        private static final long serialVersionUID = -1157813231215489004L;
        private List<Plot> _plots;
        private Point mouse = new Point( 0, 0 );
        private JTextArea description;
        private Rectangle area;
        
        private long currentFPS = 0;
        private long timeElapsed = 0;
        
        private static final int MAX_TEXT_LENGTH = 170;
        private static final long SECOND = 1000L;
        private static final int pointRadius = 10;
        private static final double ROUNDNESS = 0.0001d;
        private static final int LEGEND_LINE_LENGTH = 70;
        
        private PlotsPanel _legend;
        
        private GraphicPlotter PLOTTER = this;
        
        
        public GraphicPlotter()
        {
            _plots = new ArrayList<>();
            
            setDoubleBuffered( true );
            addMouseMotionListener( this );
            addComponentListener( this );
            
            setLayout( null );
            
            description = new JTextArea();
            description.addMouseMotionListener( this );
            add( description );
            
            addMouseListener( new MouseListener() {
                @Override
                public void mouseClicked( MouseEvent e ) {
                    _legend.checkClicked( e );
                }
                @Override
                public void mouseEntered( MouseEvent e ) {}
                @Override
                public void mouseExited( MouseEvent e ) {}
                @Override
                public void mouseReleased( MouseEvent e ) {}
                @Override
                public void mousePressed( MouseEvent e ) {
                    PLOTTER.requestFocusInWindow();
                }
            } );
            
            _legend = new PlotsPanel( this, (int) (width - 370), 350 );
        }
        
        private String makeBoxTitle( String title )
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
        
        public void addPlot( List<Pair<Double,Double>> points,
                             Color color, Line line,
                             String title )
        {
            String text = makeBoxTitle( title );
            JCheckBox box = _legend.addPlot( this, text );
            _plots.add( new Plot( title, points, chooseColor( color ), line, 2f, box ) );
            add( box );
        }
        
        public void savePlot( String dir, String file ) throws IOException
        {
            FileWriter fw = new FileWriter( dir + "/" + file );
            for (Plot plot : _plots) {
                for (Pair<Double,Double> point : plot.points) {
                    fw.write( point.getFirst() + " " + point.getSecond() + "\n" );
                }
            }
            fw.close();
        }

        protected void removePlot( int index )
        {
            Plot plot = _plots.remove( index );
            colorsInUse.remove( plot.color );
            remove( plot.box );
        }
        
        protected List<Plot> getPlots() {
            return _plots;
        }

        private Color chooseColor( Color selected )
        {
            if (selected != null) {
                colorsInUse.add( selected );
                return selected;
            } else {
                // Choose a color from the not selected ones (if any).
                for (Color color : colors) {
                    if (!colorsInUse.contains( color )) {
                        colorsInUse.add( color );
                        return color;
                    }
                }
                
                // All colors are taken: create a random color.
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
        
        private void setPlotLocation( float x, float y )
        {
            plotLocation = new Point( (int) x, (int) y );
            xLength = width - offsetW - plotLocation.x;
            
            area = new Rectangle( plotLocation.x, (int) (plotLocation.y - yLength), (int) xLength, (int) yLength );
        }
        
        private void drawGrid( Axis axe, Point p, Graphics2D g )
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
        
        private String stringValue( double val, boolean round )
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
        
        protected int getWidth( String arg, Graphics g )
        {
            FontMetrics font = g.getFontMetrics();
            return (int) font.getStringBounds( arg, g ).getWidth();
        }
        
        protected int getHeight( String arg, Graphics g )
        {
            FontMetrics font = g.getFontMetrics();
            return (int) font.getStringBounds( arg, g ).getHeight();
        }
        
        private double round( double value ) {
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
        
        private void showFPS( Graphics2D g )
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
        
        private void addPointInfo( Pair<Double,Double> point, double centerX, double centerY, Graphics2D g )
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

        private boolean drawPlot( Graphics2D g, Range range, Plot plot, boolean selected )
        {
            g.setColor( (theme == Theme.BLACK) ? Color.WHITE : Color.BLACK );
            
            boolean drawCircle = true;
            int pointInfo = -1;
            Pair<Double,Double> point;
            Point p = new Point( plotLocation.x, plotLocation.y );
            if (plot.points.size() > 0) {
                point = plot.points.get( 0 );
                // Get the starting position.
                double x = plotLocation.getX() + ((point.getFirst() - range.minX) * (xLength / range.getXRange()));
                double y = plotLocation.getY() - ((point.getSecond() - range.minY) * (yLength / range.getYRange()));
                p.setLocation( x, y );
                if (!selected) {
                    Ellipse2D circle = new Ellipse2D.Double( x - pointRadius/2, y - pointRadius/2, pointRadius, pointRadius );
                    if (circle.contains( mouse.x, mouse.y )) {
                        drawCircle = false;
                        pointInfo = 0;
                    }
                }
            }
            
            // Set the area to draw the points.
            g.setClip( area );
            
            if (plot.points.size() == 1) {
                // Draw the point.
                g.setColor( plot.color );
                g.setStroke( plot.stroke );
                point = plot.points.get( 0 );
                double x = plotLocation.getX() + ((point.getFirst() - range.minX) * (xLength / range.getXRange()));
                double y = plotLocation.getY() - ((point.getSecond() - range.minY) * (yLength / range.getYRange()));
                g.drawLine( (int) p.getX(), (int) p.getY(), (int) x, (int) y );
            }
            
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
                g.setStroke( plot.stroke );
                
                g.drawLine( (int) p.getX(), (int) p.getY(), (int) x, (int) y );
                
                if (!selected && range.checkRange( point.getFirst(), point.getSecond() )) {
                    if (drawCircle) {
                        Ellipse2D circle = new Ellipse2D.Double( x - pointRadius/2, y - pointRadius/2, pointRadius, pointRadius );
                        if (circle.contains( mouse.x, mouse.y )) {
                            drawCircle = false;
                            pointInfo = i;
                        }
                    }
                }
                
                p.setLocation( x, y );
            }
            
            g.setClip( new Rectangle( 0, 0, getWidth(), getHeight() ) );
            
            // Draw the info associated with the selected point.
            if (pointInfo >= 0) {
                point = plot.points.get( pointInfo );
                double x = plotLocation.getX() + ((point.getFirst() - range.minX) * (xLength / range.getXRange()));
                double y = plotLocation.getY() - ((point.getSecond() - range.minY) * (yLength / range.getYRange()));
                Ellipse2D circle = new Ellipse2D.Double( x - pointRadius/2, y - pointRadius/2, pointRadius, pointRadius );
                g.setStroke( new BasicStroke( 2f ) );
                g.setColor( (theme == Theme.BLACK) ? Color.WHITE : Color.GRAY );
                g.drawOval( (int) circle.getX(), (int) circle.getY(), pointRadius, pointRadius );
                addPointInfo( point, circle.getCenterX(), circle.getCenterY(), g );
            }
            
            return !drawCircle;
        }
        
        private boolean isInsideRange( double value, double left, double right ) {
            return value >= left && value <= right;
        }
        
        private void drawTicks( Graphics2D g, Range range )
        {
            g.setStroke( new BasicStroke( 1f ) );
            g.setColor( (theme == Theme.BLACK) ? Color.WHITE : Color.BLACK );
            
            if (range.maxXPoints == 1) {
                range.maxXPoints = 2;
            }
            
            if (range.maxYPoints == 1) {
                range.maxYPoints = 2;
            }
            
            // Get the number of ticks.
            int xNumTicks = Math.min( range.maxXPoints, settings._xNumTicks );
            final float xTicksOffset = xLength / xNumTicks;
            float xTickPosition = xTicksOffset;
            
            int yNumTicks = Math.min( range.maxYPoints, settings._yNumTicks );
            final float yTicksOffset = yLength / yNumTicks;
            float yTickPosition = yTicksOffset;
            // Offset of the X axis from the grid.
            final float OFFSET_Y_XAXIS = 3;
            
            if (xNumTicks > 0) {
                float xTick = (float) plotLocation.getX();
                double xValue = range.minX;
                String xTickValue = stringValue( scaleX( xValue ), true );
                g.drawString( xTickValue, xTick - getWidth( xTickValue, g )/2, plotLocation.y + getHeight( xTickValue, g ) + OFFSET_Y_XAXIS );
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
                    g.drawString( xTickValue, xTick - getWidth( xTickValue, g )/2, plotLocation.y + getHeight( xTickValue, g ) + OFFSET_Y_XAXIS );
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
            
            if (!settings._settedXRangeByUser) {
                // Set the X range of the plotter.
                settings._range.minX = Math.min( settings._range.minX, range.minX );
                settings._range.maxX = Math.max( settings._range.maxX, range.maxX );
            }
            
            if (!settings._settedYRangeByUser) {
                // Set the Y range of the plotter.
                settings._range.minY = Math.min( settings._range.minY, range.minY );
                settings._range.maxY = Math.max( settings._range.maxY, range.maxY );
            }
        }

        private void drawAxis( Graphics2D g, Range range )
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
        
        private void drawLegend( Graphics2D g )
        {
            for (Plot plot : _plots) {
                JCheckBox box = plot.box;
                box.setVisible( _legend.isSelected() );
            }
            
            if (!_legend.isSelected()) {
                _legend.draw( this, g );
                return;
            }
            
            g.setStroke( new BasicStroke( 2f ) );
            
            // Draw the legend.
            int saveStartY = 0;
            for (Plot plot : _plots) {
                JCheckBox box = plot.box;
                Rectangle bounds = box.getBounds();
                box.setForeground( (theme == Theme.BLACK) ? Color.WHITE : Color.BLACK );
                box.setBounds( getWidth() - 300, bounds.y, bounds.width, bounds.height );
                
                // Draw the associated line.
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
                
                g.setColor( plot.color );
                g.setStroke( plot.stroke );
                g.drawLine( p.x + size.width, p.y + size.height/2 - 1,
                            p.x + size.width + LEGEND_LINE_LENGTH, p.y + size.height/2 - 1 );
            }
            
            _legend.draw( this, g );
        }
        
        /**
         * Gets the range for the X and Y values.
        */
        private Range getRange()
        {
            double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
            double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
            
            if (settings._settedXRangeByUser) {
                maxX = settings._range.maxX;
                minX = settings._range.minX;
            }
            if (settings._settedYRangeByUser) {
                maxY = settings._range.maxY;
                minY = settings._range.minY;
            }
            
            if (!settings._settedXRangeByUser || !settings._settedYRangeByUser) {
                for (Plot plot : _plots) {
                    try {
                        List<Pair<Double,Double>> points = plot.points;
                        if (points.size() > 0) {
                            if (points.size() == 1) {
                                if (!settings._settedXRangeByUser) {
                                    minX = points.get( 0 ).getFirst() - 0.1d;
                                    maxX = points.get( 0 ).getFirst() + 0.1d;
                                }
                                if (!settings._settedYRangeByUser) {
                                    minY = points.get( 0 ).getSecond() - 0.1d;
                                    maxY = points.get( 0 ).getSecond() + 0.1d;
                                }
                            } else {
                                if (!settings._settedXRangeByUser) {
                                    minX = Math.min( minX, points.get( 0 ).getFirst() );
                                    maxX = Math.max( maxX, points.get( points.size() - 1 ).getFirst() );
                                }
                                if (!settings._settedYRangeByUser) {
                                    for (Pair<Double,Double> point : points) {
                                        minY = Math.min( minY, point.getSecond() );
                                        maxY = Math.max( maxY, point.getSecond() );
                                    }
                                }
                            }
                        }
                    } catch ( ConcurrentModificationException e ) {
                        // Empty body.
                    }
                }
            }
            
            int maxXPoints = 0;
            int maxYPoints = 0;
            for (Plot plot : _plots) {
                List<Pair<Double,Double>> points = plot.points;
                int rangePoints = 0;
                try {
                    for (Pair<Double,Double> point : points) {
                        // Check if the point is inside the range.
                        if (isInsideRange( point.getFirst(), minX, maxX ) && 
                            isInsideRange( point.getSecond(), minY, maxY ) ) {
                            rangePoints++;
                        }
                    }
                } catch ( ConcurrentModificationException e ) {
                    // Empty body.
                }
                
                maxXPoints = Math.max( maxXPoints, rangePoints );
                maxYPoints = Math.max( maxYPoints, rangePoints );
            }
            
            if (!XticksSettedByTheUser) {
                maxXPoints = Math.min( maxXPoints, MAX_TICKS );
                settings._xNumTicks = maxXPoints;
            }
            if (!YticksSettedByTheUser) {
                maxYPoints = Math.min( maxYPoints, MAX_TICKS );
                settings._yNumTicks = maxYPoints;
            }
            
            if (minY == maxY) {
                if (minY != 0) {
                    minY = Math.max( 0, minY - 0.1d );
                }
                maxY += 0.1d;
            }
            
            return new Range( maxXPoints, maxYPoints, minX, maxX, minY, maxY );
        }
        
        @Override
        protected void paintComponent( Graphics g )
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
            boolean selectedPoint = false;
            for (Plot plot : _plots) {
                if (plot.box.isSelected()) {
                    selectedPoint |= drawPlot( g2d, range, plot, selectedPoint );
                }
            }
            
            if (!selectedPoint) {
                description.setVisible( false );
            }
            
            // Draw legend.
            drawLegend( g2d );
            
            g2d.dispose();
        }
        
        protected Frame getFrame() {
            return frame;
        }

        @Override
        public Dimension getPreferredSize() {
            return getSize();
        }

        @Override
        public void mouseDragged( MouseEvent event ) {
            // Empty body.
        }

        @Override
        public void mouseMoved( MouseEvent event ) {
            mouse = event.getPoint();
        }

        @Override
        public void componentHidden( ComponentEvent e ) {}
        @Override
        public void componentMoved( ComponentEvent e ) {}
        @Override
        public void componentShown( ComponentEvent e ) {}
        @Override
        public void componentResized( ComponentEvent e ) {
            _legend.setXPosition( (int) (getWidth() - 370) );
        }
    }
}
