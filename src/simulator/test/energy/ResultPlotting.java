
package simulator.test.energy;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import simulator.graphics.plotter.Plotter;
import simulator.graphics.plotter.Plotter.Axis;
import simulator.graphics.plotter.Plotter.Line;
import simulator.test.energy.CPUEnergyModel.Mode;
import simulator.utils.Pair;

public class ResultPlotting
{
    public static void plotEnergy( final long time_budget, final String mode ) throws IOException
    {
        Plotter plotter = new Plotter( "Energy Consumption", 800, 600 );
        plotter.setAxisName( "Time (h)", "Energy (J)" );
        plotter.setTicks( Axis.Y, 10 );
        plotter.setTicks( Axis.X, 24, 2 );
        plotter.setRange( Axis.Y, 0, 7000 );
        plotter.setRange( Axis.X, 0, TimeUnit.HOURS.toMicros( 24 ) );
        plotter.setScaleX( 60d * 60d * 1000d * 1000d );
        plotter.addPlot( "Results/PESOS_" + mode + "_" + time_budget + "ms_Energy.log", "PESOS (" + mode + ", t = " + time_budget + "ms)" );
        plotter.addPlot( "Results/Perf_Energy.log", "perf" );
        plotter.setVisible( true );
    }
    
    private static List<Pair<Double,Double>> getPercentiles( final String fileName, final String saveFileName ) throws IOException
    {
        double percentile = 0.95d;
        double interval = TimeUnit.MINUTES.toMicros( 5 );
        
        FileReader fReader = new FileReader( fileName );
        BufferedReader reader = new BufferedReader( fReader );
        String line, results = "";
        double nextInterval = interval;
        Queue<Double> set = new PriorityQueue<>();
        List<Pair<Double,Double>> percentiles = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            String[] values = line.split( "\\s" );
            double time  = Double.parseDouble( values[0] );
            double value = Double.parseDouble( values[1] );
            if (time < nextInterval) {
                set.add( value );
            } else {
                // Change of interval.
                double index = percentile * set.size();
                double realIndex = Math.ceil( index );
                for (int i = 0; i < realIndex-1; i++) {
                    set.poll();
                }
                
                if (realIndex > index) {
                    // Not rounded.
                    results += nextInterval + " " + set.peek() + "\n";
                    percentiles.add( new Pair<>( nextInterval, set.poll() ) );
                } else {
                    // Rounded: mean of the two next values.
                    double first  = set.poll();
                    double second = set.poll();
                    results += nextInterval + " " + ((first+second)/2d) + "\n";
                    percentiles.add( new Pair<>( nextInterval, (first+second)/2d ) );
                }
                
                set.clear();
                set.add( value );
                nextInterval += interval;
            }
        }
        
        reader.close();
        
        if (saveFileName != null) {
            PrintWriter writer = new PrintWriter( saveFileName, "UTF-8" );
            writer.print( results );
            writer.close();
        }
        
        return percentiles;
    }
    
    public static void plotTailLatency( final long time_budget, final String mode ) throws IOException
    {
        List<Pair<Double, Double>> points = new ArrayList<>();
        for(int i = 0; i <= 1; i++) {
            points.add( new Pair<>( (double) (TimeUnit.HOURS.toMicros( i * 24 )), time_budget * 1000d ) );
        }
        
        // Using NULL the percentiles will not be saved on file.
        //List<Pair<Double,Double>> percentiles = getPercentiles( "Results/Distributed_Latencies.txt",
        //                                                        "Results/Distributed_Tail_Latency_95th_Percentile.txt" );
        //List<Pair<Double,Double>> pesosPercentiles = getPercentiles( "Results/PESOS_" + mode + "_" + time_budget + "ms_Tail_Latency.log",
        //                                                             "Results/PESOS_" + mode + "_" + time_budget + "ms_Tail_Latency_95th_Percentile.txt" );
        //List<Pair<Double,Double>> perfPercentiles = getPercentiles( "Results/Perf_Tail_Latency.log",
        //                                                            "Results/Perf_Tail_Latency_95th_Percentile.txt" );
        //List<Pair<Double,Double>> consPercentiles = getPercentiles( "Results/CONS_Tail_Latency.log",
        //                                                            "Results/CONS_Latency_95th_Percentile.txt" );
        
        List<Pair<Double,Double>> myPercentiles = getPercentiles( "Results/MY_Model_" + time_budget + "_Tail_Latency.log",
                                                                  "Results/MY_Model_" + time_budget + "_Tail_Latency_95th_Percentile.txt" );
        
        Plotter plotter = new Plotter( "Tail Latency 95-th Percentile", 800, 600 );
        plotter.setAxisName( "Time (h)", "95th-tile response time (ms)" );
        double yRange = time_budget * 1000d + 200000d;
        plotter.setRange( Axis.Y, 0, yRange );
        plotter.setTicks( Axis.Y, (int) (yRange / 100000) );
        plotter.setScaleY( 1000d );
        
        plotter.setRange( Axis.X, 0, TimeUnit.HOURS.toMicros( 24 ) );
        plotter.setTicks( Axis.X, 24, 2 );
        plotter.setScaleX( 60d * 60d * 1000d * 1000d );
        
        //plotter.addPlot( percentiles, Color.GREEN, Line.UNIFORM, "PESOS (" + mode + ", t=" + time_budget + "ms)" );
        //plotter.addPlot( pesosPercentiles, Color.GREEN, Line.UNIFORM, "PESOS (" + mode + ", t=" + time_budget + "ms)" );
        //plotter.addPlot( perfPercentiles, Color.RED, Line.UNIFORM, "Perf" );
        //plotter.addPlot( consPercentiles, Color.GREEN, Line.UNIFORM, "CONS" );
        plotter.addPlot( myPercentiles, Color.RED, Line.UNIFORM, "MY Model" );
        plotter.addPlot( points, Color.YELLOW, Line.DASHED, "Tail latency (" + time_budget + "ms)" );
        plotter.setVisible( true );
    }
    
    public static void plotDistributedTailLatency( final long time_budget, final String mode ) throws IOException
    {
        Plotter plotter = new Plotter( "Tail Latency 95-th Percentile", 800, 600 );
        plotter.setAxisName( "Time (h)", "95th-tile response time (ms)" );
        double yRange = time_budget * 1000d + 200000d;
        plotter.setRange( Axis.Y, 0, yRange );
        plotter.setTicks( Axis.Y, (int) (yRange / 100000) );
        plotter.setScaleY( 1000d );
        
        plotter.setRange( Axis.X, 0, TimeUnit.HOURS.toMicros( 24 ) );
        plotter.setTicks( Axis.X, 24, 2 );
        plotter.setScaleX( 60d * 60d * 1000d * 1000d );
        
        List<Pair<Double, Double>> points = new ArrayList<>();
        for(int i = 0; i <= 1; i++) {
            points.add( new Pair<>( (double) (TimeUnit.HOURS.toMicros( i * 24 )), time_budget * 1000d ) );
        }
        plotter.addPlot( points, Color.YELLOW, Line.DASHED, "Tail latency (" + time_budget + "ms)" );
        
        List<Pair<Double,Double>> percentiles = getPercentiles( "Results/PESOS_" + mode + "_" + time_budget + "ms_Tail_Latency.log",
                                                                "Results/PESOS_" + mode + "_" + time_budget + "ms_Tail_Latency_95th_Percentile.txt" );
        plotter.addPlot( percentiles, "MONOLITHIC - PESOS (" + mode + ", t=" + time_budget + "ms)" );
        
        for (int i = 1; i <= EnergyTestDIST2.NODES; i++) {
            percentiles = getPercentiles( "Results/PESOS_" + mode + "_" + time_budget + "ms_Node" + i + "_Tail_Latency.log",
                                          "Results/PESOS_" + mode + "_" + time_budget + "ms_Node" + i + "_Tail_Latency_95th_Percentile.txt" );
            plotter.addPlot( percentiles, "Node " + i + " - PESOS (" + mode + ", t=" + time_budget + "ms)" );
        }
        
        plotter.setVisible( true );
    }
    
    public static void main( final String argv[] ) throws IOException
    {
        final long time_budget = 1000;
        final Mode mode        = Mode.PESOS_TIME_CONSERVATIVE;
        
        //plotEnergy( time_budget, mode.toString() );
        plotTailLatency( time_budget, mode.toString() );
        //plotDistributedTailLatency( time_budget, mode.toString() );
    }
}