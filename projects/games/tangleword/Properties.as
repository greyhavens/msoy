package
{

import flash.geom.Rectangle;


/**
   Constants which would normally be read from a config file;
   in this case, however, it seems we want them to be embedded in the SWF.
*/
public class Properties
{
    /**
       Game display is composed of the letter board, and various
       status windows TBD. This display size is the bounding box
       of all these elements.
    */
    public static const DISPLAY : Rectangle = new Rectangle (0, 0, 500, 500);

    /**
       The board contains a collection of letters arranged in a square.
       The letter matrix will fill the board completely.
    */
    public static const BOARD : Rectangle = new Rectangle (50, 50, 250, 250);

    /**
       Dimensions of a text box that displays currently selected word.
    */
    public static const WORDBOX : Rectangle = new Rectangle (350, 250, 100, 50);
    
    
    /** Each letter is a simple square - but we want to know how big to draw them.
        This is the width and height of each letter in pixels. */
    public static const LETTER_SIZE : int = 50;

    /** Letter display is arranged in a square; this number specifies the width
        and height of the letter matrix. */
    public static const LETTERS : int = 5;

    /** The total number of letters in the matrix. */
    public static const LETTER_COUNT : int = LETTERS * LETTERS;
}
    

    
} // package
