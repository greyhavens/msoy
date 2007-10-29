//
// $Id$

package tutorial {

import flash.display.*;
import flash.text.*;
import flash.geom.*;
import flash.events.*;
import flash.filters.*;
import flash.net.*;
import flash.ui.*;
import flash.utils.*;

import com.threerings.util.EmbeddedSwfLoader;
import com.threerings.flash.SimpleTextButton;

public class Content
{
    [Embed(source="../../rsrc/whatsnext.swf", mimeType="application/octet-stream")]
    public static const SWIRL :Class;

    // how far into the clip the true origin of the swirl lies
    public static const SWIRL_OFFSET :Point = new Point(275, 225);

    // how far into the clip the true origin of the box lies
    public static const BOX_OFFSET :Point = new Point(75, 115);
    // how much unusable yet measured space surrounds the usable
    public static const BOX_PADDING :int = 15;
    // how high is the flourish on top, which also appears in the clip's height
    public static const BOX_HAT :int = 50;

    [Embed(source="../../rsrc/SunnySide.ttf", fontName="SunnySide",
           unicodeRange="U+0020-U+007E,U+2022")]
    protected static const FONT_SUNNYSIDE :Class;

    [Embed(source="../../rsrc/Goudy.ttf", fontName="Goudy",
           unicodeRange="U+0020-U+007E,U+2022")]
    protected static const FONT_GOUDY :Class;

     [Embed(source="../../rsrc/GoudyB.ttf", fontName="Goudy", fontWeight="bold",
             unicodeRange="U+0020-U+007E,U+2022")]
    protected static const FONT_GOUDY_BOLD :Class;

    [Embed(source="../../rsrc/GoudyI.ttf", fontName="Goudy", fontStyle="italic",
            unicodeRange="U+0020-U+007E,U+2022")]
    protected static const FONT_GOUDY_ITALIC :Class;

    [Embed(source="../../rsrc/GoudyBI.ttf", fontName="Goudy", fontWeight="bold",
           fontStyle="italic", unicodeRange="U+0020-U+007E,U+2022")]
    protected static const FONT_GOUDY_BOLD_ITALIC :Class;

}
}
