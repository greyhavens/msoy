package 
{

import flash.display.Shape;
import flash.display.Sprite;
import flash.text.TextField;
import flash.text.TextFormat;
import flash.text.TextFieldAutoSize;
import flash.events.MouseEvent;
import mx.core.BitmapAsset;



/**
   Letter is a graphical element that sits on the board. Note that the 'letter'
   is actually an arbitrary string, not just a character - this is because
   we may find that, in certain languages, it might make more sense to treat
   digraphs as basic units in addition to single letters.
*/
public class Letter extends Sprite
{
    
    /** 
        Constructor takes the string to be displayed on the piece,
        and piece location (in board-local coordinates).
     */
    public function Letter (board : Board, game : TangleWord, str : String, x : int, y : int)
    {
        this.x = x;
        this.y = y;

        // set background bitmap
        _board = board;
        _game = game;
        _background = Resources.makeDefaultEmptySquare ();
        addChild (_background);

        // Make a new text label on top of the bitmap.
        _text = makeNewLabel ();
        addChild (_text);
        setText (str);

        // Set up mouse handler
        addEventListener (MouseEvent.CLICK, clickHandler);
    }

    /** Set the letter on this label. We figure out the new position based on mask size. */
    public function setText (str : String) : void
    {
        Assert.True (function () : Boolean { return _background != null && _text != null },
                     "I expected background and text to be initialized by now.");
                    
        _text.text = str;

        var topMargin : Number = (_background.height - _text.height) / 2;
        _text.y = topMargin;
        _text.x = 0;
        _text.width = _background.width;
    }

    /** Retrieves current text */
    public function getText () : String
    {
        return _text.text;
    }

    /** Change letter status */
    public function setStatus (status : Status) : void
    {
        _status = status;
    }
    

    // PRIVATE HELPER FUNCTIONS

    private function makeNewLabel () : TextField
    {
        // Create text format
        var format : TextFormat = new TextFormat();
        format.font = "Verdana";
        format.color = uint(0x888899);
        format.size = 42;
        format.bold = true;

        // Create text field
        var t : TextField = new TextField ();
        t.autoSize = TextFieldAutoSize.CENTER;
        t.selectable = false;
        t.defaultTextFormat = format;

        t.width = Properties.LETTER_SIZE;
        t.height = Properties.LETTER_SIZE;
        
        return t;
    }

    private function clickHandler (event : MouseEvent) : void
    {
        _game.addLetter (_text.text);
    }

        

    // PRIVATE MEMBERS
    
    /** Text label in front */
    private var _text : TextField;

    /** Pointer to the board piece resource */
    private var _background : BitmapAsset;

    /** Pointer back to the board */
    private var _board : Board;

    /** Pointer back to the game object */
    private var _game : TangleWord;

    /** Current letter status */
    private var _status : Status;

    /** Current letter color */
    private var _color : uint;

}


}
