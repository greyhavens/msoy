//
// $Id$

package com.threerings.msoy.client {

import mx.containers.HBox;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.flex.ChatDisplayBox;

/**
 * A place view that isn't really. It merely contains a chatbox.
 */
public class NoPlaceView extends HBox
    implements PlaceView
{
    public function NoPlaceView (ctx :BaseContext)
    {
        _ctx = ctx;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var chat :ChatDisplayBox = new ChatDisplayBox(_ctx);
        chat.percentWidth = 100;
        chat.percentHeight = 100;
        addChild(chat);
    }

    // from PlaceView
    public function willEnterPlace (plobj :PlaceObject) :void
    {
        // nada
    }

    // from PlaceView
    public function didLeavePlace (plobj :PlaceObject) :void
    {
        // nada
    }

    /** The giver of life, the saver of worlds. */
    protected var _ctx :BaseContext;
}
}
