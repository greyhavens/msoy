package
{
import flash.events.KeyboardEvent;    

public class KeyboardController {

    /** Handle a keyUp event by recording it's state in the table **/
    public function keyUp (event:KeyboardEvent) :void
    {
        _down[event.keyCode] = false;        
    }

    /** Handle a keyDown event by recording the state in the table,
      * and calling the handler function if we have one
      */
    public function keyDown (event:KeyboardEvent) :void
    {
        _down[event.keyCode] = true;
                
        if (_action[event.keyCode] is Function)
        {
            _action[event.keyCode]();
        }
    }
    
    public function set eventSource (source:Object) :void
    {
        if (_eventSource != null) 
        {
            _eventSource.removeEventListener(KeyboardEvent.KEY_DOWN, keyDown);
            _eventSource.removeEventListener(KeyboardEvent.KEY_UP, keyUp);  
        }
        
        _eventSource = source;    
        
        if (_eventSource !== null)
        {
            _eventSource.addEventListener(KeyboardEvent.KEY_DOWN, keyDown);
            _eventSource.addEventListener(KeyboardEvent.KEY_UP, keyUp);  
        }  
        
        // release all the keys
        _down.length = 0;
    }

    protected function get down() :Array
    {
        return _down;
    }
    
    protected function get action() :Array
    {
        return _action;
    }

    private const _down:Array = new Array();
    
    private const _action:Array = new Array();
    
    private var _eventSource:Object;
    
    protected static const KEY_Q:uint = 81;
    
    protected static const KEY_W:uint = 87;
    
    protected static const KEY_O:uint = 79;
    
    protected static const KEY_P:uint = 80;
    
    protected static const KEY_SPACE:uint = 32;
    
    protected static const KEY_One:uint = 49;

    protected static const KEY_Return:uint = 13;
}
}
