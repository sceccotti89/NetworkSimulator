
package simulator.test;
 
import java.io.IOException;

import simulator.graphics.plotter.Plotter;
import simulator.graphics.plotter.Plotter.Axis;
 
public class PlotterTest
{
    public static void main( String[] args ) throws IOException
    {
        Plotter plotter = new Plotter( "prova", 800, 600 );
        plotter.addPlot( "C:\\Users\\Stefano\\Desktop\\test2.txt", "test2.txt" );
        //plotter.showGrid( true );
        plotter.setTicks( Axis.X, 23, 2 );
        plotter.setVisible( true );
    }
}
