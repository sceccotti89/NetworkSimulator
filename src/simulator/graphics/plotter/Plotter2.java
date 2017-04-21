
package simulator.graphics.plotter;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.newdawn.slick.AppGameContainer;
import org.newdawn.slick.BasicGame;
import org.newdawn.slick.Color;
import org.newdawn.slick.Font;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.geom.Point;

import simulator.utils.Pair;

public class Plotter2<T extends Number> extends BasicGame
{
    private List<Pair<Long,T>> _points;
    private String xAxisName = "X";
    private String yAxisName = "Y";
    private Point plotLocation;
    private final Plotter2<T> plotter = this;
    private float xLength;
    private float yLength;
    
    private int _xNumTicks = -1;
    private int _yNumTicks = -1;
    
    private boolean showGrid = false;
    
    private Color lineColor = Color.green;
    private Theme theme = Theme.BLACK;
    
    
    public enum Axis{ X, Y };
    public enum Theme{ WHITE, BLACK };
    
    
    // All these values are expressed in pixels.
    private static final float distanceFromAxis = 15;
    private static final float offsetW = 50;
    private static final float offsetH = 50;
    private static final float lengthTick = 5;
    
    
    
    public Plotter2( final String title )
    {
        super( title );
    }
    
    public Plotter2( final String title, final String filePlotter ) throws IOException
    {
        this( title, readPlot( filePlotter ) );
    }
    
    public Plotter2( final String title, final List<Pair<Long,T>> points )
    {
        super( title );
        _points = points;
    }
    
    @SuppressWarnings("unchecked")
    private static <T extends Number> List<Pair<Long,T>> readPlot( final String filePlotter ) throws IOException
    {
        BufferedReader reader = new BufferedReader( new FileReader( filePlotter ) );
        List<Pair<Long,T>> points = new ArrayList<>();
        
        String line;
        while ((line = reader.readLine()) != null) {
            String[] values = line.split( "\\s+ | \\t+" );
            Long x = Long.parseLong( values[0] );
            Double y = Double.parseDouble( values[1] );
            points.add( new Pair<>( x, (T) y ) );
        }
        
        reader.close();
        
        return points;
    }
    
    public Plotter2<T> setTicks( final Axis axis, final int ticks )
    {
        if (axis == Axis.X) {
            _xNumTicks = ticks;
        } else {
            _yNumTicks = ticks;
        }
        return this;
    }
    
    public Plotter2<T> setTheme( final Theme theme ) {
        this.theme = theme;
        return this;
    }
    
    public Plotter2<T> setLineColor( final Color color ) {
        lineColor = color;
        return this;
    }
    
    public Plotter2<T> showGrid( final boolean show ) {
        showGrid = show;
        return this;
    }
    
    public Plotter2<T> setAxisName( final String xName, final String yName )
    {
        xAxisName = xName;
        yAxisName = yName;
        return this;
    }
    
    /**
     * 
    */
    public void launch( final int displayWidth, final int displayHeight )
    {
        // TODO se lo volessi lanciare stand-alone utilizzare un file condiviso.
        // Launch the plotter in a new application.
        /*String javaHome = System.getProperty( "java.home" );
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        ProcessBuilder builder = new ProcessBuilder( javaBin, "process.parent_child.ChildProc" );
        try {
            builder.start();
        } catch ( IOException e1 ) {
            e1.printStackTrace();
        }*/
        
        new Thread()
        {
            @Override
            public void run()
            {
                try {
                    AppGameContainer app = new AppGameContainer( plotter );
                    
                    app.setTargetFrameRate( 30 );
                    app.setDisplayMode( displayWidth, displayHeight, false );
                    
                    app.setForceExit( false );
                    app.start();
                } catch( SlickException e ) {
                    e.printStackTrace();
                }
            }
        }.start();
    }
    
    private void setPlotLocation( final GameContainer gc, final float x, final float y )
    {
        plotLocation = new Point( x, y );
        xLength = gc.getWidth() - offsetW - plotLocation.getX();
    }
    
    @Override
    public void init( final GameContainer gc ) throws SlickException
    {
        setPlotLocation( gc, offsetW + lengthTick, gc.getHeight() - offsetH );
        yLength = gc.getHeight() - offsetH * 2;
    }
    
    @Override
    public void update( final GameContainer gc, final int delta ) throws SlickException {
    	// Empty body.
    }
    
    private void drawGrid( final Axis axe, final Point p, final Graphics g )
    {
        final float lines = 25;
        final float spaces = lines * 2 - 1;
        if (axe == Axis.Y) {
            final float dashLength = xLength / spaces;
            float x = (float) p.getX();
            for(int i = 0; i < lines; i++) {
                final float maxX = Math.min( p.getX() + xLength, x + dashLength );
                g.drawLine( x, p.getY(), maxX, p.getY() );
                x += dashLength * 2;
            }
        } else {
            final float dashLength = yLength / spaces;
            float y = (float) p.getY();
            for(int i = 0; i < lines; i++) {
                final float maxY = Math.max( p.getY() - yLength, y - dashLength );
                g.drawLine( p.getX(), y, p.getX(), maxY );
                y -= dashLength * 2;
            }
        }
    }
    
    private <S> String stringValue( S val )
    {
        String value = val + "";
        if (value.contains( "." )) {
            value = value.substring( 0, Math.min( value.indexOf( '.' ) + 2, value.length() ) );
        }
        return value;
    }
    
    @Override
    public void render( final GameContainer gc, final Graphics g ) throws SlickException
    {
        Font font = g.getFont();
        
        g.setBackground( (theme == Theme.BLACK) ? Color.black : Color.white );
        
        g.setLineWidth( 1.f );
        g.setColor( (theme == Theme.BLACK) ? Color.white : Color.black );
        
        Pair<Long,T> point;
        // Get the max X and Y values.
        float maxY = 0, maxX = 0;
        for (int i = 0; i < _points.size(); i++) {
            try {
                point = _points.get( i );
            } catch( IndexOutOfBoundsException e ){
                continue;
            }
            maxY = Math.max( point.getSecond().floatValue(), maxY );
            maxX = Math.max( point.getFirst(), maxX );
        }
        //System.out.println( "MAX_Y: " + maxY );
        int maxWidth = font.getWidth( stringValue( maxY ) );
        if (plotLocation.getX() - maxWidth - lengthTick - offsetW < 0) {
            setPlotLocation( gc, offsetW + maxWidth + lengthTick, gc.getHeight() - offsetH );
        }
        
        // Draw x and y axis.
        g.drawLine( plotLocation.getX(), plotLocation.getY(), plotLocation.getX() + xLength, plotLocation.getY() );
        g.drawLine( plotLocation.getX(), plotLocation.getY(), plotLocation.getX(), offsetH );
        
        g.drawString( xAxisName, plotLocation.getX() + xLength/2 - font.getWidth( xAxisName )/2,
                                 plotLocation.getY() + offsetH / 2 - font.getHeight( xAxisName ) / 2 );
        g.rotate( offsetW, plotLocation.getY() - yLength / 2 - font.getHeight( yAxisName ), -90f );
        g.drawString( yAxisName, offsetW - font.getWidth( yAxisName )/2,
                                 plotLocation.getY() - yLength / 2 - font.getHeight( yAxisName ) * 2 - distanceFromAxis );
        g.resetTransform();
        
        // Draw the tick lines and values.
        g.drawString( "0", plotLocation.getX() - font.getWidth( "0" )/2, plotLocation.getY() );
        
        int xNumTicks = (_xNumTicks == -1) ? _points.size() : _xNumTicks;
        final float xTicksOffset = xLength / xNumTicks;
        float xTickPosition = xTicksOffset;
        
        int yNumTicks = (_yNumTicks == -1) ? _points.size() : _yNumTicks;
        final float yTicksOffset = yLength / yNumTicks;
        float yTickPosition = yTicksOffset;
        
        int xIndexTick = 1, yIndexTick = 1;
        Point p = new Point( plotLocation.getX(), plotLocation.getY() );
        for (int i = 0; i < _points.size(); i++) {
            try {
                point = _points.get( i );
            } catch( IndexOutOfBoundsException e ){
                continue;
            }
            // Draw the tick line.
            g.setColor( (theme == Theme.BLACK) ? Color.white : Color.black );
            g.setLineWidth( 1.f );
            float xTick = plotLocation.getX() + xTickPosition;
            if (showGrid) {
                drawGrid( Axis.X, new Point( xTick, plotLocation.getY() ), g );
            }
            g.drawLine( xTick, plotLocation.getY(), xTick, plotLocation.getY() - lengthTick );
            xTickPosition += xTicksOffset;
            
            float yTick = plotLocation.getY() - yTickPosition;
            if (showGrid) {
                drawGrid( Axis.Y, new Point( plotLocation.getX(), yTick ), g );
            }
            g.drawLine( plotLocation.getX(), yTick, plotLocation.getX() + lengthTick, yTick );
            yTickPosition += yTicksOffset;
            
            // Draw the X and Y values of the tick.
            String xTickValue = stringValue( (maxX / xNumTicks) * xIndexTick++ );
            g.drawString( xTickValue, xTick - font.getWidth( xTickValue )/2, plotLocation.getY() );
            
            String yTickValue = stringValue( (maxY / yNumTicks) * yIndexTick++ );
            g.drawString( yTickValue, plotLocation.getX() - font.getWidth( yTickValue ) - lengthTick, yTick - font.getHeight( yTickValue )/2 );
            
            // Draw the point.
            float x = plotLocation.getX() + ((point.getFirst() / maxX) * xLength);
            float y = plotLocation.getY() - ((point.getSecond().floatValue() / maxY) * yLength);
            
            g.setColor( lineColor );
            g.setLineWidth( 2.f );
            g.drawLine( p.getX(), p.getY(), x, y );
            
            p.setLocation( x, y );
        }
    }
}