//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.geom.Rectangle;

import com.threerings.flex.ScrollBox;

import com.threerings.msoy.world.client.RoomView;

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
}
}
