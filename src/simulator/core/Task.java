
package simulator.core;

import java.util.HashMap;
import java.util.Map;

import simulator.events.Packet;

public class Task
{
    private Map<String,Object> _contents;
    
    public Task() {
        this( null );
    }
    
    public Task( final Packet p )
    {
        if (p == null ) {
            _contents = new HashMap<>();
        } else {
            _contents = new HashMap<>( p.getContents() );
        }
    }
    
    public void addContent( final String field, final Object value ) {
        _contents.put( field, value );
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getContent( final String field ) {
        return (T) _contents.get( field );
    }
}