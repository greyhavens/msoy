package
{

import flash.text.TextField;
import flash.text.TextFormat;    
import flash.text.TextFieldType;
import flash.display.Sprite;
import flash.display.SimpleButton;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;


/**
   Encapsulates the text entry field, and its display.
*/
public class InputField extends Sprite
{
    public function InputField (game : TangleWord) : void
    {
        _game = game;

        // Create text format
        var format : TextFormat = new TextFormat();
        format.font = "Verdana";
        format.color = uint(0x888899);
        format.size = 24;
        format.bold = true;

        // Initialize an input box
        _input = new TextField ();
        _input.border = true;
        _input.borderColor = uint(0xaaaabb);
        _input.type = TextFieldType.INPUT;
        _input.addEventListener (KeyboardEvent.KEY_UP, keypressHandler);
        _input.defaultTextFormat = format;
        _input.text = "TEST";

        _input.x = Properties.INPUT.x;
        _input.y = Properties.INPUT.y;
        _input.width = Properties.INPUT.width;
        _input.height = _input.textHeight; // note: this must happen after we set the text property
        _input.text = "";
        _input.setSelection (0, 0);
        addChild (_input);

        // Initialize a label
        _label = new TextField ();
        _label.defaultTextFormat = format;
        _label.text = "OK";
        _input.border = true;
        _input.borderColor = uint(0xaaaabb);
        _label.x = Properties.INPUT.x;
        _label.y = _input.y + _input.height + 10;
        _label.width = Properties.INPUT.width;
        _label.height = _label.textHeight;
        _label.addEventListener (MouseEvent.CLICK, buttonClickHandler);
        addChild (_label);

    }

    // Testing: adds a letter to the input box
    public function addLetter (letter : String) : void
    {
        _input.appendText (letter);
    }

    // Testing: set input
    public function setText (text : String) : void
    {
        _input.text = text;
    }

    // PRIVATE METHODS

    private function keypressHandler (event : KeyboardEvent) : void
    {
        var keyCode : int = event.keyCode;
        if (keyCode == 13) // ENTER
        {
            processEntry ();
        }
    }

    private function buttonClickHandler (event : MouseEvent) : void
    {
        processEntry ();
    }
    

    private function processEntry () : void
    {
        var letters : Array = _input.text.toUpperCase().split ("");
        _game.checkBoard (letters);
    }
    

    // PRIVATE MEMBER VARIABLES

    /** Stores our copy of the game object */
    private var _game : TangleWord;

    /** Input field */
    private var _input : TextField;

    /** Label */
    private var _label : TextField;

}

}
