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
        appendText (message);
        appendText ("\n"); 
    }

    /** Clears the log */
    public function Clear () : void
    {
        text = "";
    }

}


}
