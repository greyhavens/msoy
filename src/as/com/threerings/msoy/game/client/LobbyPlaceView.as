//
// $Id$

package com.threerings.msoy.game.client {

import mx.containers.Canvas;

import caurina.transitions.Tweener;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.ui.ScalingMediaContainer;
import com.threerings.flex.FlexWrapper;

/**
 * View that displays the game's splash image underneath the lobby panel itself.
 */
public class LobbyPlaceView extends Canvas
    implements PlaceView
{
    public function LobbyPlaceView (splash :ScalingMediaContainer)
    {
        var wrapper :FlexWrapper = new FlexWrapper(splash);

        wrapper.setStyle("verticalCenter", "0");
        wrapper.setStyle("horizontalCenter", "0");
        wrapper.width = splash.maxW;
        wrapper.height = splash.maxH;
        
        addChild(wrapper);

        // slowly fade in the spash screen underneath the lobby
        wrapper.alpha = 0;
        Tweener.addTween(wrapper, { alpha: 1, time: 3, transition: "easeincubic" });
    }

    // from interface PlaceView
    public function willEnterPlace (plobj :PlaceObject) :void
    {
        // nothing
    }

    // from interface PlaceView
    public function didLeavePlace (plobj :PlaceObject) :void
    {
        // nothing
    }
}
}
