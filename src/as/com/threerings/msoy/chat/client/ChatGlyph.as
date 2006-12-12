//
// $Id$

package com.threerings.msoy.chat.client {

import flash.display.Sprite;

import flash.text.TextField;

public class ChatGlyph extends Sprite
{
    public function ChatGlyph (overlay :ChatOverlay, type :int)
    {
        _overlay = overlay;
        _type = type;
    }

    public function getType () :int
    {
        return _type;
    }

    protected var _overlay :ChatOverlay;

    protected var _type :int;
}
}
