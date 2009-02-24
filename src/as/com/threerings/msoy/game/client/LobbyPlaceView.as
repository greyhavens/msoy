//
// $Id$

package com.threerings.msoy.game.client {

import mx.containers.Canvas;

import caurina.transitions.Tweener;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.flex.FlexWrapper;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.ui.ScalingMediaContainer;

/**
 * View that displays the game's splash image underneath the lobby panel itself.
 */
public class LobbyPlaceView extends Canvas
    implements PlaceView
{
    public function LobbyPlaceView (game :Game)
    {
        var splash :ScalingMediaContainer = getLoadingMedia(game);
        var wrapper :FlexWrapper = new FlexWrapper(splash);

        wrapper.setStyle("verticalCenter", "0");
        wrapper.setStyle("horizontalCenter", "0");
        wrapper.width = splash.maxW;
        wrapper.height = splash.maxH;
        
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
    
    /** Returns the descriptor of a piece of media to be displayed while the game is loading. */ 
    public static function getLoadingMedia (game :Game) :ScalingMediaContainer
    {
        // get the full splash media if possible
        if (game.splashMedia != null) {
            return ScalingMediaContainer.createView(game.splashMedia, MediaDesc.GAME_SPLASH_SIZE);
        }

        // if splash media is not available, try furni, and finally a thumbnail
        var media :MediaDesc = game.getRawFurniMedia() != null ? 
            game.getFurniMedia() : game.getThumbnailMedia();

        return ScalingMediaContainer.createView(media, MediaDesc.PREVIEW_SIZE);
    }
}
}
