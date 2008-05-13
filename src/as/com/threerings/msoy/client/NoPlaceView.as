//
// $Id$

package com.threerings.msoy.client {

import com.threerings.crowd.data.PlaceObject;

/**
 * A place view that isn't really. It merely allows the display of chat.
 */
public class NoPlaceView extends LayeredContainer
    implements ChatPlaceView
{
    public function NoPlaceView ()
    {
        styleName = "noPlaceView";
    }

    public function willEnterPlace (plobj :PlaceObject) :void
    {
        // Nada
    }

    public function didLeavePlace (plobj :PlaceObject) :void
    {
        // Nada
    }
}
}
