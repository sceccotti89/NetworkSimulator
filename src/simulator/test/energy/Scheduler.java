/**
 * @author Stefano Ceccotti
*/

package simulator.test.energy;

import java.util.Collection;

import simulator.test.energy.CPU.Core;
import simulator.test.energy.CPUModel.QueryInfo;
import simulator.test.energy.EnergyCPU.PESOScore;
import simulator.utils.Time;

public interface Scheduler<IN extends Iterable<?>,OUT,T>
{
    /**
     * Schedule an incoming task to the correct object.
     * 
     * @param time      time of evaluation.
     * @param object    object into which schedule the next task.
     *                  It must implement the {@linkplain Iterable} interface,
     *                  such as a {@linkplain java.util.List List} or a {@linkplain Collection}.
     * @param task      the task object.
     * 
     * @return an object representing the scheduling result.
     *         It could be an integer, or a long, used to identify
     *         the selected object.
    */
    public OUT schedule( Time time, IN object, T task );
}

class FirstLeastLoaded implements Scheduler<Iterable<Core>,Long,QueryInfo>
{
    @Override
    public Long schedule( Time time, Iterable<Core> cores, QueryInfo q )
    {
        Core core = null;
        double utilization = Integer.MAX_VALUE;
        for (Core c : cores) {
            double coreUtilization = c.getUtilization( time );
            if (coreUtilization < utilization) {
                core = c;
                utilization = coreUtilization;
            }
        }
        
        return core.getId();
    }
}

class TieLeastLoaded implements Scheduler<Iterable<Core>,Long,QueryInfo>
{
    @Override
    public Long schedule( Time time, Iterable<Core> cores, QueryInfo q )
    {
        Core core = null;
        double utilization = Integer.MAX_VALUE;
        long tiedSelection = Long.MAX_VALUE;
        boolean tieSituation = false;
        for (Core c : cores) {
            double coreUtilization = c.getUtilization( time );
            if (coreUtilization < utilization) {
                core = c;
                utilization = coreUtilization;
                tiedSelection = c.tieSelected;
                tieSituation = false;
                // TODO per testare l'abbassamento di frequenze di PESOS dovrei commentare questa parte
            } else if (coreUtilization == utilization) {
                if (c.tieSelected < tiedSelection) {
                    core = c;
                    utilization = coreUtilization;
                    tiedSelection = c.tieSelected;
                }
                tieSituation = true;
            }
        }
        
        if (tieSituation) {
            core.tieSelected++;
        }
        
        return core.getId();
    }
}

class MinFrequency implements Scheduler<Iterable<Core>,Long,QueryInfo>
{
    @Override
    public Long schedule( Time time, Iterable<Core> cores, QueryInfo q )
    {
        Core core = null;
        long minFrequency = Long.MAX_VALUE;
        long tiedSelection = Long.MAX_VALUE;
        boolean tieSituation = false;
        for (Core c : cores) {
            long frequency = c.getFrequency();
            c.addQuery( q, false );
            if (c.getFrequency() < minFrequency) {
                core = c;
                minFrequency = c.getFrequency();
                tiedSelection = c.tieSelected;
                tieSituation = false;
            } else if (c.getFrequency() == minFrequency) {
                if (c.tieSelected < tiedSelection) {
                    core = c;
                    minFrequency = c.getFrequency();
                    tiedSelection = c.tieSelected;
                }
                tieSituation = true;
            }
            c.removeQuery( time, c.getQueue().size() - 1, false );
            c.setFrequency( frequency );
        }
        
        if (tieSituation) {
            core.tieSelected++;
        }
        
        return core.getId();
    }
}

class EarliestCompletionTime implements Scheduler<Iterable<Core>,Long,QueryInfo>
{
    @Override
    public Long schedule( Time time, Iterable<Core> cores, QueryInfo q )
    {
        Core core = null;
        long minExecutionTime = Long.MAX_VALUE;
        long tiedSelection = Long.MAX_VALUE;
        boolean tieSituation = false;
        for (Core c : cores) {
            long executionTime = ((PESOScore) c).getQueryExecutionTime( time );
            if (executionTime < minExecutionTime) {
                core = c;
                minExecutionTime = executionTime;
                tiedSelection = c.tieSelected;
                tieSituation = false;
            } else if (executionTime == minExecutionTime) {
                if (c.tieSelected < tiedSelection) {
                    core = c;
                    minExecutionTime = executionTime;
                    tiedSelection = c.tieSelected;
                }
                tieSituation = true;
            }
        }
        
        if (tieSituation) {
            core.tieSelected++;
        }
        
        return core.getId();
    }
}