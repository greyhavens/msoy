//
// $Id$

package com.threerings.msoy.room.client.editor {

import flash.display.Sprite;
import flash.geom.Rectangle;

import com.threerings.flex.ScrollBox;

import com.threerings.msoy.ui.MsoyMediaContainer;

import com.threerings.msoy.room.client.RoomView;
import com.threerings.msoy.room.data.DecorData;

/**
 * A scrollbox specifically hacked to work with the RoomView.
 */
public class RoomViewScrollBox extends ScrollBox
{
    public function RoomViewScrollBox (
        roomView :RoomView, maxWidth :int, maxHeight :int)
    {
        super(roomView, maxWidth, maxHeight);

        // TODO: stop our superclass from listening on ENTER_FRAME and maybe
        // have a custom callback from the room view when it changes the
        // scroll rect
    }

    override protected function getScrollBounds () :Rectangle
    {
        return RoomView(_target).getScrollBounds();
    }

    override protected function recheckBounds () :void
    {
        super.recheckBounds();
        rescaleBackgroundSprite();
    }

    protected function rescaleBackgroundSprite () :void
    {
        if (_bg != null) {
            _bg.scaleX = _bg.scaleY = _scale;
            _bg.x = (width - _bg.width)  / 2;
            _bg.y = height - _bg.height;
        }
    }        
        
    public function setBackground (decorData :DecorData) :void
    {
        if (_bg != null) {
            rawChildren.removeChild(_bg);
            _bg = null;
        }

        if (decorData != null) {
            _bg = new MsoyMediaContainer(decorData.media);
            _bg.mask = _mask;
            rawChildren.addChildAt(_bg, 0);
            rescaleBackgroundSprite();
        }
    }

    public function getBackground () :Sprite
    {
        return _bg;
    }

    protected var _bg :Sprite;
}
}
