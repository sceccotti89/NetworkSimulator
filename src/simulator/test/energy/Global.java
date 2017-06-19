
package simulator.test.energy;

import java.io.PrintWriter;

public class Global
{
    /** Fields used for testing. */
    public static final String QUERY_ID = "queryID";
    public static final String EVENT_ID = "eventID";
    
    /** Samplings ID. */
    public static final String ENERGY_SAMPLING = "EnergyConsumption";
    public static final String IDLE_ENERGY_SAMPLING = "IdleEnergy";
    public static final String TAIL_LATENCY_SAMPLING = "TailLatency";
    
    public static PrintWriter eventWriter;
}