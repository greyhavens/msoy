//
// $Id$

package com.threerings.msoy.game.client {

import mx.containers.Canvas;

import caurina.transitions.Tweener;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.flex.FlexWrapper;

import com.threerings.orth.data.MediaDesc;
import com.threerings.msoy.data.all.MediaDescSize;
import com.threerings.msoy.ui.ScalingMsoyMediaContainer;

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
        splash :MediaDesc, thumb :MediaDesc) :ScalingMsoyMediaContainer
    {
        // get the full splash media if possible
        if (splash != null) {
            return ScalingMsoyMediaContainer.createView(splash, MediaDescSize.GAME_SPLASH_SIZE);
        } else {
            // if splash media is not available, use the thumbnail which always is
            return ScalingMsoyMediaContainer.createView(thumb, MediaDescSize.PREVIEW_SIZE);
        }
    }

    public function SplashPlaceView (splash :MediaDesc, thumb :MediaDesc)
    {
        var scont :ScalingMsoyMediaContainer = getLoadingMedia(splash, thumb);
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
