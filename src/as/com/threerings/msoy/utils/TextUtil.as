//
// $Id$

package com.threerings.msoy.utils {

import flash.text.TextFormat;
import flash.text.TextField;

import com.threerings.util.StringUtil;

import com.threerings.msoy.chat.client.ChatOverlay;

/**
 * Utility class for dealing with text formats and links embedded in text in msoy.
 */
public class TextUtil
{
   /**
    * Return an array of text strings, with any string needing special formatting preceeded by
    * that format.
    *
    * @param text The text to parse
    * @param defaultFormat The format to use for normal text.  If left null,
    *        ChatOverlay.createChatFormat() is used.
    * @param parseSpecial If true, parsing will look for special links, such as those we use in
    *                     translation messages and commands.
    * @param useDefaultColor If true, the color from the defaultFormat will be used for links,
    *                        otherwise links will be displayed in blue.
    */
    public static function parseLinks (
        text :String, defaultFormat :TextFormat = null, parseSpecial :Boolean = false,
        useDefaultColor :Boolean = false) :Array
    {
        defaultFormat = defaultFormat || ChatOverlay.createChatFormat();

        var bits :Array = parseSpecial
            ?  parseSpecialLinks(text, defaultFormat, useDefaultColor)
            : [ defaultFormat, text ];
        var retval :Array = [];
        var lastFmt :TextFormat;
        for (var ii :int = 0; ii < bits.length; ii++) {
            if (ii % 2 == 0) {
                lastFmt = TextFormat(bits[ii]);
            } else {
                var links :Array = StringUtil.parseURLs(String(bits[ii]));
                for (var jj :int = 0; jj < links.length; jj++) {
                    var str :String = links[jj];
                    var linkify :Boolean = (jj % 2 == 1)
                        // Avoid linkifying if we already have a URL
                        && lastFmt.url == ""
                        // Only linkify commands if parseSpecial is on
                        && (parseSpecial || !StringUtil.startsWith(str, "command://"));
                    retval.push(linkify
                        ? createLinkFormat(str, defaultFormat, useDefaultColor)
                        : lastFmt);
                    retval.push(str);
                }
            }
        }
        return retval;
    }

    /**
     * Populates a TextField with the specified formatted strings.
     *
     * @param txt The text field to format the text onto.
     * @param texts A mixed array of String and TextFormat objects, with each String being rendered
     *        in the TextFormat preceding it, or the default format if not preceded by a TextFormat
     * @param defaultFmt The format to use when none is provided for a String field.  If left null,
     *        ChatOverlay.createChatFormat() will be used.
     */
    public static function setText (
        txt :TextField, texts :Array, defaultFmt :TextFormat = null) :void
    {
        defaultFmt = defaultFmt || ChatOverlay.createChatFormat();
        var fmt :TextFormat = null;
        var length :int = 0;
        for each (var o :Object in texts) {
            if (o is TextFormat) {
                fmt = (o as TextFormat);

            } else {
                // Note: we should just be able to set the defaultFormat for the entire field and
                // then format the different stretches, but SURPRISE! It doesn't quite work right,
                // so we format every goddamn peice of the text by hand.
                var append :String = String(o);
                var newLength :int = length + append.length;
                txt.appendText(append);
                fmt = fmt || defaultFmt;
                if (length != newLength) {
                    txt.setTextFormat(fmt, length, newLength);
                }
                fmt = null;
                length = newLength;
            }
        }
    }

    /**
     * Parse any "special links" (in the format "\uFFFCtext\uFFFCurl\uFFFD") in the specified text.
     *
     * @return an array containing [ format, text, format, text, ... ].
     */
    protected static function parseSpecialLinks (
        text :String, defaultFormat :TextFormat, useDefaultColor :Boolean) :Array
    {
        var array :Array = [];

        var result :Object;
        do {
            result = SPECIAL_LINK_REGEXP.exec(text);
            if (result != null) {
                var index :int = int(result.index);
                array.push(defaultFormat, text.substring(0, index));
                array.push(
                    createLinkFormat(String(result[2]), defaultFormat, useDefaultColor),
                    String(result[1]));

                // and advance the text
                var match :String = String(result[0]);
                text = text.substring(index + match.length);

            } else {
                // it's just left-over text
                array.push(defaultFormat, text);
            }

        } while (result != null);

        return array;
    }

    /**
     * Create a link format for the specified link text.
     */
    protected static function createLinkFormat (
        url :String, defaultFormat :TextFormat, useDefaultColor :Boolean) :TextFormat
    {
        var fmt :TextFormat = new TextFormat();
        fmt.align = defaultFormat.align;
        fmt.font = FONT;
        fmt.size = defaultFormat.size;
        fmt.underline = true;
        fmt.color = useDefaultColor ? defaultFormat.color : 0x0093dd;
        fmt.bold = true;
        fmt.url = "event:" + url;
        return fmt;
    }

    /** Matches "special links", which are in the format "\uFFFCtext\uFFFCurl\uFFFD" */
    protected static const SPECIAL_LINK_REGEXP :RegExp =
        new RegExp("\\\uFFFC(.+?)\\\uFFFC(.+?)\\\uFFFD");
    protected static const FONT :String = "Arial";
}
}
