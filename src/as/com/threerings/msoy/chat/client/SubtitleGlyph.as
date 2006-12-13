//
// $Id$

package com.threerings.msoy.chat.client {

import flash.text.TextField;
import flash.text.TextFieldAutoSize;

public class SubtitleGlyph extends ChatGlyph
{
    public function SubtitleGlyph (
        overlay :ChatOverlay, type :int, text :String)
    {
        super(overlay, type);

        var txt :TextField = new TextField();
        // set it up to be as wide as it can, and to wrap around if it wants
        txt.multiline = true;
        txt.wordWrap = true;
        txt.width = overlay.getTargetWidth();;
        txt.autoSize = TextFieldAutoSize.LEFT;
        // then set the text
        txt.text = text;

        addChild(txt);

        overlay.drawSubtitleShape(graphics, type, txt.textWidth, txt.textHeight);
    }
}
}
