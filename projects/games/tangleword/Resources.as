package
{

import mx.core.BitmapAsset;
import flash.filters.GlowFilter;    
import flash.text.TextFormat;



/** 
  Storage class for embedded resources; 
  later it may take over dynamic resources as well. 
*/

public class Resources
{

    // RESOURCE ACCESSORS


    // BITMAPS
    
    /** Returns a new instance of the default bitmap to serve as the background. */
    public static function makeGameBackground () : BitmapAsset 
    {
        return new _defaultBackground ();
    }

    /** Returns a new instance of the default empty square bitmap resource */
    public static function makeSquare () : BitmapAsset 
    {
        return new _defaultSquare ();
    }

    // FORMATS

    /** Returns a new instance of text style used for individual letters */
    public static function makeFormatForBoardLetters () : TextFormat
    {
        var format : TextFormat = new TextFormat();
        format.font = "Verdana";
        format.color = uint(0x888899);
        format.size = 42;
        format.bold = true;

        return format;
    }

    /** Returns a new instance of text style used for game messages and UI */
    public static function makeFormatForUI () : TextFormat
    {
        var format : TextFormat = new TextFormat();
        format.font = "Verdana";
        format.color = uint(0x777788);
        format.size = 18;

        return format;
    }

    // FILTERS

    /** Returns a new instance of a filter suitable for a cursor */
    public static function makeCursorFilter () : GlowFilter
    {
        var filter : GlowFilter = new GlowFilter ();
        filter.color = uint(0x8888cc);
        filter.inner = true;
        return filter;
    }


    
    // RESOURCE DEFINITIONS

    [Embed(source="rsrc/background.png")]
    private static const _defaultBackground : Class;

    [Embed(source="rsrc/square.png")]
    private static const _defaultSquare : Class;

}


} // package
