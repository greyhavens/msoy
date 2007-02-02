package 
{

import flash.display.Shape;
import flash.display.Sprite;
import flash.geom.Point;
import flash.text.TextField;
import flash.text.TextFormat;
import flash.text.TextFieldAutoSize;
import flash.events.MouseEvent;
import flash.events.Event;
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
    public function Letter (board : Board, game : TangleWord, str : String,
                            x : int, y : int, px : int, py : int)
    {
        this.x = x;
        this.y = y;

        _p = new Point (px, py);

        // set background bitmap
        _board = board;
        _game = game;
        _background = Resources.makeDefaultEmptySquare ();
        addChild (_background);

        // Make a new text label on top of the bitmap.
        _text = makeNewLabel ();
        addChild (_text);
        setText (str);

        // Set up event listeners
        addEventListener (MouseEvent.CLICK, clickHandler);
        addEventListener (Event.ENTER_FRAME, tickHandler);
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
    public function setStatus (status : uint) : void
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
        var top : Point = null;
        
        switch (_status)
        {

        case Status.NORMAL:

            // If the player clicks on an unused letter, check if it's
            // next to the latest one, and add it to the stack. Unless it's
            // the first one, in which case always accept.
            top = _board.selection.peek ();
            var isNeighborOfTop : Boolean =
                top != null && Math.abs(top.x - _p.x) <= 1 && Math.abs(top.y - _p.y) <= 1

            if (top == null || isNeighborOfTop)
            {
                _status = Status.SELECTED;
                _board.selection.push (new Point (_p.x, _p.y));
                _board.updateDisplay ();
            }
            break;
                
        case Status.SELECTED:
            
            // If the player clicks on a selected letter, if it's the
            // last one they selected, remove it.
            top = _board.selection.peek ();
            if (top != null &&
                top.x == _p.x && top.y == _p.y)
            {
                _board.selection.pop ();
                _status = Status.NORMAL;
                _board.updateDisplay ();
            }
            break;
        
        }
        
    }

    private function tickHandler (event : Event) : void
    {
        var targetColor : uint = uint(_status);
        if (_text.textColor != targetColor)
        {
            _text.textColor = targetColor;
        }
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
    private var _status : uint = Status.NORMAL;

    /** Current letter color */
    private var _color : uint;

    /** Current position on the letter board */
    private var _p : Point;

}


}
