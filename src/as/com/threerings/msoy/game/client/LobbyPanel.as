//
// $Id$

package com.threerings.msoy.game.client {

import mx.containers.VBox;

import com.threerings.crowd.client.PlaceView;

import com.threerings.crowd.data.PlaceObject;

// TODO
public class LobbyPanel extends VBox
    implements PlaceView
{
    // from PlaceView
    public function willEnterPlace (plobj :PlaceObject) :void
    {
        // nada for now
    }

    // from PlaceView
    public function didLeavePlace (plobj :PlaceObject) :void
    {
        // nada for now
    }
}
}
