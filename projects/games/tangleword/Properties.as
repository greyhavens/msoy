package
{

import flash.geom.Rectangle;


/**
   Storage of constants which would normally be read from a config file;
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
       This total dimension includes any margins between letters and the board border.
    */
    public static const BOARD : Rectangle = new Rectangle (50, 50, 250, 250);

    /**
       Player selects words by typing them into a text field (for now, at least).
       This is the position of the text field control.
    */
    public static const INPUT : Rectangle = new Rectangle (350, 50, 100, 100);

    
    /** Each letter is a simple square - but we want to know how big to draw them. :) */
    public static const LETTER_SIZE : int = 50;

    /** Letter display is arranged in a square; this number specifies how
        many letters per side. */
    public static const LETTER_COUNT_PER_SIDE : int = 5;

    /** The total number of letters on the board */
    public static const LETTER_COUNT_TOTAL : int = LETTER_COUNT_PER_SIDE * LETTER_COUNT_PER_SIDE;
}
    

    
} // package
