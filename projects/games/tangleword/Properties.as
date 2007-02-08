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
    public static const DISPLAY : Rectangle = new Rectangle (0, 0, 600, 500);

    /**
       The board contains a collection of letters arranged in a square.
       The letter matrix will fill the board completely.
    */
    public static const BOARD : Rectangle = new Rectangle (50, 50, 250, 250);

    /**
       Position of a text box that displays currently selected word.
    */
    public static const WORDFIELD : Rectangle = new Rectangle (50, 312, 190, 28);

    /**
       Position of the OK button
    */
    public static const OKBUTTON : Rectangle = new Rectangle (250, 312, 50, 28);

    /**
       Position of the log text field
    */
    public static const LOGFIELD : Rectangle = new Rectangle (320, 170, 200, 160);

    /**
       Position of the score box
    */
    public static const SCOREFIELD : Rectangle = new Rectangle (320, 50, 200, 100);
    
    
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
