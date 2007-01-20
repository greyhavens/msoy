//
// $Id$

package com.threerings.msoy.chat.client {

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

public class SubtitleGlyph extends ChatGlyph
{
    /**
     * @param texts A mixed array of String and TextFormat objects, with
     * each String being rendered in the TextFormat preceding it, or the
     * default format if not preceded by a TextFormat.
     */
    public function SubtitleGlyph (
        overlay :ChatOverlay, type :int, lifetime :int, defaultFmt :TextFormat,
        texts :Array)
    {
        super(overlay, type, lifetime);

        var txt :TextField = new TextField();

        // set it up to be as wide as it can, and to wrap around if it wants
        txt.multiline = true;
        txt.wordWrap = true;
        txt.width = overlay.getTargetWidth();;
        txt.autoSize = TextFieldAutoSize.LEFT;
        txt.selectable = true; // enable copy/paste

        // then set the text
        var fmt :TextFormat = null;
        var length :int = 0;
        for each (var o :Object in texts) {
            if (o is TextFormat) {
                fmt = (o as TextFormat);

            } else {
                // Note: we should just be able to set the defaultFormat
                // for the entire field and then format the different
                // stretches, but SURPRISE! It doesn't quite work right,
                // so we format every goddamn piece of the text by hand.
                var append :String = String(o);
                var newLength :int = length + append.length;
                txt.appendText(append);
                if (fmt == null) {
                    fmt = defaultFmt;
                }
                if (length != newLength) {
                    txt.setTextFormat(fmt, length, newLength);
                }
                fmt = null;
                length = newLength;
            }
        }

        addChild(txt);

        overlay.drawSubtitleShape(graphics, type, txt.textWidth, txt.textHeight);
    }
}
}
