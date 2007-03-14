//
// $Id$

package {

import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;

import flash.geom.Rectangle;

/**
 * Defines skinnable content. TODO: use the right format whenever Ray finally finalizes content
 * packs.
 */
public class Content
{
    /** Defines the dictionary we use for word validation and letter frequency. */
    public static const LOCALE :String = "en-us";

    /** The number of letters along one side of the board. This must be an odd number. */
    public static const BOARD_SIZE :int = 9;

    /** The font used for the letters. */
    public static const FONT_NAME :String = "Verdana";

    /** The point size of the letters when rendered (TODO: scale?). */
    public static const FONT_SIZE :int = 12;

    /** The foreground color of the letters. */
    public static const FONT_COLOR :uint = uint(0x336600);

    /** The highlighted color of the letters. */
    public static const HIGH_FONT_COLOR :uint = uint(0xFF0000);

    /** The pixels size of the letter tiles (TODO: scale?). */
    public static const TILE_SIZE :int = 15;

    /** The background color of the letter tiles. */
    public static const TILE_COLOR :uint = uint(0xCCFF99);

    /** The background color of the center letter tile. */
    public static const CENTER_TILE_COLOR :uint = uint(0xFF0033);

    /** The pixels size of the shooter. */
    public static const SHOOTER_SIZE :int = 50;

    /** The color of the shooters. */
    public static const SHOOTER_COLOR :Array =
        [ uint(0x6699CC), uint(0xCC6600), uint(0x996699), uint(0xCC6666) ];

    /** The location and dimensions of the input field. */
    public static const INPUT_RECT :Rectangle = new Rectangle(100, 470, 250, 20);
}

}
