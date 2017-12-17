
package simulator.test;
 
import java.awt.Color;
import java.io.IOException;

import simulator.graphics.plotter.Plotter;
import simulator.graphics.plotter.Plotter.Axis;
import simulator.graphics.plotter.Plotter.Line;
import simulator.graphics.plotter.Plotter.PointType;
 
public class PlotterTest
{
    public static void main( String[] args ) throws IOException
    {
        Plotter plotter = new Plotter( "prova", 800, 600 );
        //plotter.addPlot( "/home/stefano/test.txt", Color.GREEN, Line.UNIFORM, PointType.TRIANGLE, "test" );
        plotter.addPlot( "C:/Users/Stefano/Desktop/test.txt", Color.GREEN, Line.UNIFORM, PointType.TRIANGLE, "UN_NOME_VERAMENTE_LUNGO" );
        plotter.addPlot( "C:/Users/Stefano/Desktop/test.txt", Color.RED,   Line.UNIFORM, PointType.TRIANGLE, "test2" );
        plotter.addPlot( "C:/Users/Stefano/Desktop/test.txt", Color.BLUE,  Line.UNIFORM, PointType.TRIANGLE, "test3" );
        plotter.setRange( Axis.X, -5, 15 );
        plotter.setRange( Axis.Y,  5, 25 );
        plotter.setTicks( Axis.X, 23, 2 );
        plotter.setVisible( true );
    }
}
