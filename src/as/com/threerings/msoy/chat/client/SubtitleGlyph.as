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
        txt.multiline = true;
        txt.width = overlay.getTargetWidth();
        txt.autoSize = TextFieldAutoSize.LEFT;
        txt.text = text;

        addChild(txt);

        overlay.drawSubtitleShape(graphics, type, txt.width, txt.height);
    }
}
}
