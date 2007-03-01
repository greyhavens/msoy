package
{

import flash.display.Sprite;    
import flash.text.TextField;
import flash.text.TextFormat;


/**
   Logger class is an on-screen widget that takes lines of text and displays them.
   Pretty straightforward, no? :)
*/
public class Logger extends TextField
{
    /** Max number of lines displayed in the log window */
    public static const MAX_LINES : uint = 7;
    
    // Constructor, sets everything up
    public function Logger ()
    {
        var format : TextFormat = Resources.makeFormatForLogger ();
        this.selectable = false;
        this.defaultTextFormat = format;
        this.borderColor = Resources.defaultBorderColor;
        this.border = true;
        this.multiline = true;
    }


    /** Adds a line of text to the bottom of the logger */
    public function Log (message : String) : void
    {
        _lines.push (message);
        if (_lines.length > MAX_LINES)
        {
            _lines.shift ();
        }
        redraw ();
    }

    /** Clears the log */
    public function Clear () : void
    {
        _lines = new Array ();
        redraw ();
    }

    /** Redraws the text */
    private function redraw () : void
    {
        this.text = "";
        for each (var s: String in _lines)
        {
            appendText (s);
            appendText ("\n");
        }
    }


    // PRIVATE VARIABLES
    private var _lines : Array = new Array ();
    

}


}
