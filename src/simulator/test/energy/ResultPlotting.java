
package simulator.test.energy;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import simulator.graphics.plotter.Plotter;
import simulator.graphics.plotter.Plotter.Axis;
import simulator.graphics.plotter.Plotter.Line;
import simulator.graphics.plotter.Plotter.Theme;
import simulator.test.energy.CPUModel.Mode;
import simulator.utils.Pair;

public class ResultPlotting
{
    private static final int PERCENTILE = 95;
    
    public static void plotEnergy( long time_budget, String mode ) throws IOException
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
    
    private static List<Pair<Double,Double>> getPercentiles( String fileName, String saveFileName ) throws IOException
    {
        double percentile = PERCENTILE * 0.01d;
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
    
    public static void plotTailLatency( long time_budget, String mode ) throws IOException
    {
        List<Pair<Double, Double>> points = new ArrayList<>();
        for(int i = 0; i <= 1; i++) {
            points.add( new Pair<>( (double) (TimeUnit.HOURS.toMicros( i * 24 )), time_budget * 1000d ) );
        }
        
        // Using NULL the percentiles will not be saved on file.
        //List<Pair<Double,Double>> percentiles = getPercentiles( "Results/Distributed_Latencies.txt",
        //                                                        "Results/Distributed_Tail_Latency_" + PERCENTILE + "th_Percentile.txt" );
        List<Pair<Double,Double>> pesosPercentiles = getPercentiles( "Results/PESOS_" + mode + "_" + time_budget + "ms_Tail_Latency.log",
                                                                     "Results/PESOS_" + mode + "_" + time_budget + "ms_Tail_Latency_" + PERCENTILE + "th_Percentile.txt" );
        //List<Pair<Double,Double>> perfPercentiles = getPercentiles( "Results/Perf_Tail_Latency.log",
        //                                                            "Results/Perf_Tail_Latency_" + PERCENTILE + "th_Percentile.txt" );
        //List<Pair<Double,Double>> consPercentiles = getPercentiles( "Results/CONS_Tail_Latency.log",
        //                                                            "Results/CONS_Latency_" + PERCENTILE + "th_Percentile.txt" );
        //List<Pair<Double,Double>> loadSensitivePercentiles = getPercentiles( "Results/LOAD_SENSITIVE_" + mode + "_" + time_budget + "ms_Tail_Latency.log",
        //                                                                     "Results/LOAD_SENSITIVE_" + mode + "_" + time_budget + "ms_Tail_Latency_" + PERCENTILE + "th_Percentile.txt" );
        List<Pair<Double,Double>> myPercentiles = getPercentiles( "Results/MY_Model_" + mode + "_" + time_budget + "ms_Tail_Latency.log",
                                                                  "Results/MY_Model_" + mode + "_" + time_budget + "ms_Tail_Latency_" + PERCENTILE + "th_Percentile.txt" );
        
        Plotter plotter = new Plotter( "Tail Latency " + PERCENTILE + "-th Percentile", 800, 600 );
        plotter.setAxisName( "Time (h)", PERCENTILE + "th-tile response time (ms)" );
        double yRange = time_budget * 1000d + 200000d;
        plotter.setRange( Axis.Y, 0, yRange );
        plotter.setTicks( Axis.Y, (int) (yRange / 100000) );
        plotter.setScaleY( 1000d );
        
        plotter.setRange( Axis.X, 0, TimeUnit.HOURS.toMicros( 24 ) );
        plotter.setTicks( Axis.X, 24, 2 );
        plotter.setScaleX( 60d * 60d * 1000d * 1000d );
        
        //plotter.addPlot( percentiles, Line.UNIFORM, "PESOS (" + mode + ", t=" + time_budget + "ms)" );
        plotter.addPlot( pesosPercentiles, Line.UNIFORM, "PESOS (" + mode + ", t=" + time_budget + "ms)" );
        //plotter.addPlot( perfPercentiles, Line.UNIFORM, "Perf" );
        //plotter.addPlot( consPercentiles, Line.UNIFORM, "CONS" );
        //plotter.addPlot( loadSensitivePercentiles, Line.UNIFORM, "LS (" + mode + ", t=" + time_budget + "ms)" );
        plotter.addPlot( myPercentiles, Line.UNIFORM, "MY Model (" + mode + ", t=" + time_budget + "ms)" );
        plotter.addPlot( points, Color.YELLOW, Line.DASHED, "Tail latency (" + time_budget + "ms)" );
        plotter.setVisible( true );
    }
    
    public static void plotDistributedTailLatency( long time_budget, String mode ) throws IOException
    {
        Plotter plotter = new Plotter( "DISTRIBUTED Tail Latency 95-th Percentile", 800, 600 );
        plotter.setAxisName( "Time (h)", "95th-tile response time (ms)" );
        plotter.setScaleY( 1000d );
        
        plotter.setTheme( Theme.WHITE );
        
        plotter.setRange( Axis.X, 0, TimeUnit.HOURS.toMicros( 24 ) );
        plotter.setTicks( Axis.X, 23, 2 );
        plotter.setScaleX( TimeUnit.HOURS.toMicros( 1 ) );
        
        final String tau = new String( ("\u03C4").getBytes(), Charset.defaultCharset() );
        final String folder = "/home/stefano/SIMULATOR RESULTS/DISTRIBUTED/PESOS_NO_CONTROLLER/";
        plotter.addPlot( folder + "PESOS_TC_500ms_Tail_Latency_95th_Percentile.txt", Line.UNIFORM, "PESOS TC (" + tau + " = 500 ms)" );
        plotter.addPlot( folder + "PESOS_EC_500ms_Tail_Latency_95th_Percentile.txt", Line.UNIFORM, "PESOS EC (" + tau + " = 500 ms)" );
        plotter.addPlot( folder + "PESOS_TC_1000ms_Tail_Latency_95th_Percentile.txt", Line.UNIFORM, "PESOS TC (" + tau + " = 1000 ms)" );
        plotter.addPlot( folder + "PESOS_EC_1000ms_Tail_Latency_95th_Percentile.txt", Line.UNIFORM, "PESOS EC (" + tau + " = 1000 ms)" );
        plotter.addPlot( folder + "PEGASUS_500ms_Tail_Latency_95th_Percentile.txt", Line.UNIFORM, "PEGASUS (" + tau + " = 500 ms)" );
        plotter.addPlot( folder + "PEGASUS_1000ms_Tail_Latency_95th_Percentile.txt", Line.UNIFORM, "PEGASUS (" + tau + " = 1000 ms)" );
        
        List<Pair<Double, Double>> tl_500ms  = new ArrayList<>( 2 );
        List<Pair<Double, Double>> tl_1000ms = new ArrayList<>( 2 );
        for(int i = 0; i <= 1; i++) {
            tl_500ms.add( new Pair<>( (double) (TimeUnit.HOURS.toMicros( i * 24 )), 500000d ) );
            tl_1000ms.add( new Pair<>( (double) (TimeUnit.HOURS.toMicros( i * 24 )), 1000000d ) );
        }
        plotter.addPlot( tl_500ms, Color.BLACK, Line.DASHED, "Tail latency (" + 500 + "ms)" );
        plotter.addPlot( tl_1000ms, Color.BLACK, Line.DASHED, "Tail latency (" + 1000 + "ms)" );
        
        double yRange = 1600000d;
        plotter.setRange( Axis.Y, 0, yRange );
        plotter.setTicks( Axis.Y, 15, 2 );
        
        plotter.setVisible( true );
    }
    
    public static void plotDistributedReplicaTailLatency( long time_budget, String mode,
                                                          double lambda, String type,
                                                          int latencyType ) throws IOException
    {
        Plotter plotter = new Plotter( "PESOS (" + mode + ", t=" + time_budget + "ms, Lambda=" + lambda + ", Type=" + type + ") - Tail Latency " + PERCENTILE + "-th Percentile", 800, 600 );
        plotter.setAxisName( "Time (h)", PERCENTILE + "th-tile response time (ms)" );
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
        
        List<Pair<Double,Double>> percentiles = getPercentiles( "Results/Distributed_Replica_" + lambda + "_" + type + "_L" + latencyType + "_PESOS_" + mode + "_" + time_budget + "ms_Tail_Latency.txt",
                                                                "Results/Distributed_Replica_" + lambda + "_" + type + "_L" + latencyType + "_PESOS_" + mode + "_" + time_budget + "ms_Tail_Latency_" + PERCENTILE + "th_Percentile.txt" );
        plotter.addPlot( percentiles, "PESOS (" + mode + ", t=" + time_budget + "ms, Lambda=" + lambda + ", Type=" + type + ")" );
        
        plotter.setVisible( true );
    }
    
    public static void plotMeanCompletionTime() throws IOException
    {
        Plotter plotter = new Plotter( "Mean Completion Time", 800, 600 );
        plotter.setAxisName( "Time (h)", "Mean completion time (ms)" );
        plotter.setRange( Axis.Y, 0, 200000 );
        plotter.setScaleY( 1000d );
        
        plotter.setRange( Axis.X, 0, TimeUnit.HOURS.toMicros( 24 ) );
        plotter.setTicks( Axis.X, 24, 2 );
        plotter.setScaleX( 60d * 60d * 1000d * 1000d );
        
        plotter.addPlot( "Results/MeanCompletionTime.log", "Mean Completion Time" );
        
        plotter.setVisible( true );
    }
    
    public static void plotMeanArrivalTime() throws IOException
    {
        Plotter plotter = new Plotter( "Query Per Time Slot", 800, 600 );
        plotter.setAxisName( "Time (h)", "Query Per Time Slot (15min)" );
        plotter.setRange( Axis.Y, 0, 12000 );
        
        plotter.setRange( Axis.X, 0, TimeUnit.HOURS.toMicros( 24 ) );
        plotter.setTicks( Axis.X, 24, 2 );
        plotter.setScaleX( 60d * 60d * 1000d * 1000d );
        
        plotter.addPlot( "Results/QueryPerTimeSlot.log", "Query Per Time Slot" );
        
        plotter.setVisible( true );
    }
    
    public static void main( String argv[] ) throws IOException
    {
        final long time_budget = 500;
        final Mode mode        = Mode.TIME_CONSERVATIVE;
        
        //plotEnergy( time_budget, mode.toString() );
        //plotTailLatency( time_budget, mode.toString() );
        plotDistributedTailLatency( time_budget, mode.toString() );
        
        //plotDistributedReplicaTailLatency( time_budget, mode.toString(), 0.75, "LongTerm", 2 );
        
        //plotMeanCompletionTime();
        //plotMeanArrivalTime();
    }
}
