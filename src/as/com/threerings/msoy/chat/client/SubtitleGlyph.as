//
// $Id$

package com.threerings.msoy.chat.client {

import flash.events.MouseEvent;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import mx.managers.PopUpManager;

import com.threerings.flex.PopUpUtil;

import com.threerings.msoy.client.UberClient;
import com.threerings.orth.data.MediaDesc;

import com.threerings.msoy.utils.TextUtil;
import com.threerings.msoy.ui.MediaWrapper;

public class SubtitleGlyph extends ChatGlyph
{
    /** If this glyph is showing chat history, the index into the HistoryList
     * to find the corresponding ChatMessage. */
    public var histIndex :int;

    public var thumbnail :MediaDesc;

    public static var thumbsEnabled :Boolean;

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
        // default to transparent
        setTransparent(true);

        var txt :TextField = createTextField();
        // set it up to be as wide as it can, and to wrap around if it wants
        txt.width = overlay.getTargetTextWidth();
        txt.autoSize = TextFieldAutoSize.LEFT;

        // then set the text
        TextUtil.setText(txt, texts, defaultFmt);
        // mouse enabled will get turned on when/if the mouse is hovering over an actual URL.
        txt.mouseEnabled = false;
        sizeFieldToText(txt);

        // add the text and draw a shape around it
        addChild(_txt = txt);
        txt.x = overlay.drawSubtitleShape(graphics, type, txt.width, txt.height);

        addEventListener(MouseEvent.ROLL_OVER, handleRollOver);
        addEventListener(MouseEvent.ROLL_OUT, handleRollOut);
    }

    public function setTransparent (transparent :Boolean) :void
    {
        alpha = transparent ? 0.75 : 1;
    }

    protected function handleRollOver (event :MouseEvent) :void
    {
        if (thumbnail != null && thumbsEnabled) {
            _pop = MediaWrapper.createView(thumbnail);
            _pop.x = event.stageX + 10;
            _pop.y = event.stageY - (_pop.height/2);
            PopUpManager.addPopUp(_pop, UberClient.getApplication(), false);
            PopUpUtil.fit(_pop);
        }
    }

    protected function handleRollOut (event :MouseEvent) :void
    {
        if (_pop != null) {
            PopUpManager.removePopUp(_pop);
            _pop = null;
        }
    }

    protected var _pop :MediaWrapper;
}
}
