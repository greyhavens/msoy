package
{

import mx.core.BitmapAsset;
import flash.filters.GlowFilter;    
import flash.text.TextFormat;
import flash.text.TextFormatAlign;


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

    /** Returns a new instance of the default button background bitmap resource */
    public static function makeButtonBackground () : BitmapAsset
    {
        return new _defaultButton ();
    }


    
    // FORMATS

    /** Returns a new instance of text style used for individual letters */
    public static function makeFormatForBoardLetters () : TextFormat
    {
        var format : TextFormat = new TextFormat();
        format.font = "Verdana";
        format.color = uint(0x77aabb);
        format.size = 42;

        return format;
    }

    /** Returns a new instance of text style used for game messages and UI */
    public static function makeFormatForUI () : TextFormat
    {
        var format : TextFormat = new TextFormat();
        format.font = "Verdana";
        format.color = uint(0x77aabb);
        format.size = 18;
        format.bold = true;
        return format;
    }

    /** Returns a new instance of a text style for the logging window */
    public static function makeFormatForLogger () : TextFormat
    {
        var format : TextFormat = new TextFormat ();
        format.font = "Verdana";
        format.color = uint(0x77aabb);
        format.size = 12;
        format.bold = false;
        return format;
    }

    /** Returns a new instance of a text style for the score window */
    public static function makeFormatForScore () : TextFormat
    {
        var format : TextFormat = new TextFormat ();
        format.font = "Verdana";
        format.color = uint(0x77aabb);
        format.size = 12;
        format.bold = true;
        return format;
    }

    /** Returns a new instance of a text style for the countdown timer */
    public static function makeFormatForCountdown () : TextFormat
    {
        var format : TextFormat = new TextFormat ();
        format.font = "Verdana";
        format.color = uint(0xaa6666);
        format.size = 18;
        format.bold = true;
        format.align = TextFormatAlign.CENTER;
        return format;
    }

    /** Returns a default border color */
    public static const defaultBorderColor : uint = uint (0xeeeeff);

    

    // FILTERS

    /** Returns a new instance of a filter suitable for a cursor */
    public static function makeCursorFilter () : GlowFilter
    {
        var filter : GlowFilter = new GlowFilter ();
        filter.color = uint(0xeeeeff);
        filter.inner = true;
        return filter;
    }

    /** Returns a new instance of a filter suitable for a selected letter */
    public static function makeSelectedFilter () : GlowFilter
    {
        var filter : GlowFilter = new GlowFilter ();
        filter.color = uint(0x446677);
        filter.inner = true;
        filter.blurX = filter.blurY = 32;
        return filter;
    }

    /** Returns a new instance of a filter for rolled-over button */
    public static function makeButtonOverFilter () : GlowFilter
    {
        var filter : GlowFilter = new GlowFilter ();
        filter.color = uint(0xeeeeff);
        filter.inner = true;
        filter.blurX = filter.blurY = 8;
        return filter;
    }


    
    // RESOURCE DEFINITIONS

    [Embed(source="rsrc/background.png")]
    private static const _defaultBackground : Class;

    [Embed(source="rsrc/square.png")]
    private static const _defaultSquare : Class;

    [Embed(source="rsrc/button.png")]
    private static const _defaultButton : Class;
}


} // package
