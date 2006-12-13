//
// $Id$

package com.threerings.msoy.chat.client {

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

public class SubtitleGlyph extends ChatGlyph
{
    /**
     * @param expireDuration the time at which the glyph expires,
     * or int.MAX_VALUE.
     */
    public function SubtitleGlyph (
        overlay :ChatOverlay, type :int, text :String, expireDuration :int)
    {
        super(overlay, type, expireDuration);

        var fmt :TextFormat = new TextFormat();
        fmt.size = 16;

        var txt :TextField = new TextField();
        // set it up to be as wide as it can, and to wrap around if it wants
        txt.defaultTextFormat = fmt;
        txt.multiline = true;
        txt.wordWrap = true;
        txt.width = overlay.getTargetWidth();;
        txt.autoSize = TextFieldAutoSize.LEFT;
        txt.selectable = true; // enable copy/paste

        // then set the text
        txt.text = text;

        addChild(txt);

        overlay.drawSubtitleShape(graphics, type, txt.textWidth, txt.textHeight);
    }
}
}
