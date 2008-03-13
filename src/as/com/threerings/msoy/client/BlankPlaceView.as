//
// $Id$

package com.threerings.msoy.client {

import mx.containers.VBox;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

/**
 * Displays a blank view when we have nothing else to display.
 */
public class BlankPlaceView extends VBox
    implements PlaceView
{
    public function BlankPlaceView ()
    {
        styleName = "blankPlace";
    }

    // from interface PlaceView
    public function willEnterPlace (plobj :PlaceObject) :void
    {
        // nada
    }

    // from interface PlaceView
    public function didLeavePlace (plobj :PlaceObject) :void
    {
        // nada
    }
}
}
