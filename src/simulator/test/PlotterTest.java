
package simulator.test;
 
import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import simulator.graphics.plotter.Plotter;
import simulator.graphics.plotter.Plotter.Axis;
import simulator.graphics.plotter.Plotter.Line;
import simulator.utils.Pair;
 
public class PlotterTest
{
    public static void main( String[] args ) throws IOException
    {
        Plotter plotter = new Plotter( "prova", 800, 600 );
        //plotter.addPlot( "/home/stefano/test.txt", Color.GREEN, Line.UNIFORM, PointType.TRIANGLE, "test" );
        /*plotter.addPlot( "C:/Users/Stefano/Desktop/test.txt", Color.GREEN, Line.UNIFORM, PointType.TRIANGLE, "UN_NOME_VERAMENTE_LUNGO" );
        plotter.addPlot( "C:/Users/Stefano/Desktop/test.txt", Color.RED,   Line.UNIFORM, PointType.TRIANGLE, "test2" );
        plotter.addPlot( "C:/Users/Stefano/Desktop/test.txt", Color.BLUE,  Line.UNIFORM, PointType.TRIANGLE, "test3" );
        plotter.setRange( Axis.X, -5, 15 );
        plotter.setRange( Axis.Y,  5, 25 );
        plotter.setTicks( Axis.X, 23, 2 );*/
        
        plotter.setRange( Axis.Y, 0, 1200000 );
        plotter.setTicks( Axis.Y, (int) (1200000 / 100000) );
        plotter.setScaleY( 1000d );
        
        plotter.setRange( Axis.X, 0, TimeUnit.HOURS.toMicros( 24 ) );
        plotter.setTicks( Axis.X, 23, 2 );
        plotter.setScaleX( TimeUnit.HOURS.toMicros( 1 ) );
        
        List<Pair<Double, Double>> tl_500ms  = new ArrayList<>( 2 );
        List<Pair<Double, Double>> tl_1000ms = new ArrayList<>( 2 );
        for(int i = 0; i <= 1; i++) {
            tl_500ms.add( new Pair<>( (double) (TimeUnit.HOURS.toMicros( i * 24 )), 500000d ) );
            tl_1000ms.add( new Pair<>( (double) (TimeUnit.HOURS.toMicros( i * 24 )), 1000000d ) );
        }
        plotter.addPlot( tl_500ms, Color.YELLOW, Line.DASHED, "Tail latency (" + 500 + "ms)" );
        plotter.addPlot( tl_1000ms, Color.LIGHT_GRAY, Line.DASHED, "Tail latency (" + 1000 + "ms)" );
        
        plotter.setVisible( true );
    }
}
