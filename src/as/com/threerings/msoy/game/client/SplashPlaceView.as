//
// $Id$

package com.threerings.msoy.game.client {

import caurina.transitions.Tweener;

import mx.containers.Canvas;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.orth.data.MediaDesc;
import com.threerings.orth.data.MediaDescSize;
import com.threerings.orth.ui.ScalingMediaDescContainer;

import com.threerings.flex.FlexWrapper;

/**
 * View that displays the game's splash image underneath the lobby panel itself.
 */
public class SplashPlaceView extends Canvas
    implements PlaceView
{
    /**
     * Returns the descriptor of a piece of media to be displayed while the game is loading.
     */
    public static function getLoadingMedia (
        splash :MediaDesc, thumb :MediaDesc) :ScalingMediaDescContainer
    {
        // get the full splash media if possible
        if (splash != null) {
            return ScalingMediaDescContainer.createView(splash, MediaDescSize.GAME_SPLASH_SIZE);
        } else {
            // if splash media is not available, use the thumbnail which always is
            return ScalingMediaDescContainer.createView(thumb, MediaDescSize.PREVIEW_SIZE);
        }
    }

    public function SplashPlaceView (splash :MediaDesc, thumb :MediaDesc)
    {
        var scont :ScalingMediaDescContainer = getLoadingMedia(splash, thumb);
        var wrapper :FlexWrapper = new FlexWrapper(scont);

        wrapper.setStyle("verticalCenter", "0");
        wrapper.setStyle("horizontalCenter", "0");
        wrapper.width = scont.maxW;
        wrapper.height = scont.maxH;

        addChild(wrapper);

        // slowly fade in the splash screen underneath the lobby
        wrapper.alpha = 0;
        Tweener.addTween(wrapper, { alpha: 1, time: 1, transition: "easeinsine" });
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
