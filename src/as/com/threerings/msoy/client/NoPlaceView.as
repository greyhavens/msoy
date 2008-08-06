//
// $Id$

package com.threerings.msoy.client {

import com.threerings.crowd.data.PlaceObject;

/**
 * A place view that isn't really. It merely allows the display of chat.
 */
public class NoPlaceView extends LayeredContainer
    implements MsoyPlaceView
{
    public function NoPlaceView ()
    {
        styleName = "noPlaceView";
    }

    // from MsoyPlaceView
    public function setPlaceSize (uw :Number, uh :Number) :void
    {
        // nada
    }

    // from MsoyPlaceView
    public function setIsShowing (showing :Boolean) :void
    {
        // nada
    }

    // from MsoyPlaceView
    public function padVertical () :Boolean
    {
        return false;
    }

    // from MsoyPlaceView
    public function shouldUseChatOverlay () :Boolean
    {
        return true;
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
