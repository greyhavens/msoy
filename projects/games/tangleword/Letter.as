package 
{

import flash.display.Shape;
import flash.display.Sprite;
import flash.filters.GlowFilter;
import flash.geom.Point;
import flash.text.TextField;
import flash.text.TextFormat;
import flash.text.TextFieldAutoSize;
import mx.core.BitmapAsset;



/**
   Letter is a graphical element that sits on the board. Note that the 'letter'
   is actually an arbitrary string, not just a character - this is because
   we may find that, in certain languages, it might make more sense to treat
   digraphs as basic units in addition to single letters.
*/
public class Letter extends Sprite
{
    /** Constructor */
    public function Letter (display : Display)
    {
        _display = display;

        // set background bitmap
        _background = Resources.makeSquare ();
        addChild (_background);

        // Make a new text label on top of the bitmap.
        _label = makeNewLabel ();
        addChild (_label);
        setText ("?");

        // Load resources
        makeFilters ();
    }

    /** Set the letter on this label. We set its position based on text height. */
    public function setText (str : String) : void
    {
        Assert.True (_background != null && _label != null,
                     "I expected background and text to be initialized by now.");
                    
        _label.text = str.toUpperCase();

        var topMargin : Number = (Properties.LETTER_SIZE - _label.height) / 2;
        _label.y = topMargin;
        _label.x = 0;
        _label.width = _background.width;
    }

    /** Retrieves current text */
    public function getText () : String
    {
        return _label.text;
    }

    /** Retrieves cursor highlight value */
    public function get isCursorEnabled () : Boolean
    {
        return _cursorEnabled;
    }

    /** Sets or clears cursor highlight value */
    public function set isCursorEnabled (newValue : Boolean) : void
    {
        Assert.NotNull (_cursorFilter, "Letter filters failed to initialize");
        if (newValue != _cursorEnabled)
        {
            // do visual updates!
            var filters : Array = new Array ();
            if (newValue)
            {
                filters.push (_cursorFilter);
            }
            _background.filters = filters;

            // finally...
            _cursorEnabled = newValue;
        }
    }

    /** Is this letter displaying in an enabled state? */
    public function get isLetterEnabled () : Boolean
    {
        return _letterEnabled;
    }

    /** Sets whether the letter should be displayed in an enabled state */
    public function set isLetterEnabled (newValue : Boolean) : void
    {
        _letterEnabled = _background.visible = newValue;
    }
    

    /** Sets the letter's selection value */
    public function setSelection (value : Boolean) : void
    {
        var filters : Array = new Array ();
        if (value)
        {
            filters.push (_selectedFilter);
        }
        _label.filters = filters;
    }


    // PRIVATE HELPER FUNCTIONS

    private function makeNewLabel () : TextField
    {
        // Create text field
        var format : TextFormat = Resources.makeFormatForBoardLetters ();
        var t : TextField = new TextField ();
        t.autoSize = TextFieldAutoSize.CENTER;
        t.selectable = false;
        t.defaultTextFormat = format;
        t.width = Properties.LETTER_SIZE;
        t.height = Properties.LETTER_SIZE;
        
        return t;
    }

    private function makeFilters () : void
    {
        _cursorFilter = Resources.makeCursorFilter ();
        _selectedFilter = Resources.makeSelectedFilter ();
    }
        
    

    // PRIVATE MEMBERS
    
    /** Text label in front */
    private var _label : TextField;

    /** Pointer to the board piece resource */
    private var _background : BitmapAsset;

    /** Pointer back to the board */
    private var _display : Display;

    /** Is this letter displaying in an enabled or disabled state? */
    private var _letterEnabled : Boolean = true;
    
    /** Is this letter being overlayed with a cursor? */
    private var _cursorEnabled : Boolean = false;


    // STORAGE

    /** Cursor filter */
    private var _cursorFilter : GlowFilter;

    /** Letter filter */
    private var _selectedFilter : GlowFilter;

}


}
