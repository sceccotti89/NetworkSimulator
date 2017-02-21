/**
 * @author Stefano Ceccotti
*/

package simulator;

import simulator.coordinator.EventGenerator;

public abstract class Agent
{
    private long _id;
    private EventGenerator _evGenerator;
    
    public Agent( final long id )
    {
        _id = id;
    }
    
    public Agent( final long id, final EventGenerator evGenerator )
    {
        _id = id;
        _evGenerator = evGenerator;
    }
    
    /***/
    public void addGenerator( final EventGenerator evGenerator )
    {
        _evGenerator = evGenerator;
    }
    
    //TODO public abstract void body();
    
    public long getId() {
        return _id;
    }
}