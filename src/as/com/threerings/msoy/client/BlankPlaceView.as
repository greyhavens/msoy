//
// $Id$

package com.threerings.msoy.client {

import mx.containers.Canvas;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.flex.FlexWrapper;

import com.threerings.msoy.ui.LoadingSpinner;

/**
 * Displays a blank view when we have nothing else to display.
 */
public class BlankPlaceView extends Canvas
    implements PlaceView
{
    public function BlankPlaceView ()
    {
        var spinner :LoadingSpinner = new LoadingSpinner();
        spinner.setProgress(0, 1);
        var wrapper :FlexWrapper = new FlexWrapper(spinner);

        wrapper.setStyle("verticalCenter", "0");
        wrapper.setStyle("horizontalCenter", "0");
        wrapper.width = LoadingSpinner.WIDTH;
        wrapper.height = LoadingSpinner.HEIGHT;
        addChild(wrapper);
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
