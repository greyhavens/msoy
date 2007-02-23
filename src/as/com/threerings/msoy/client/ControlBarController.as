//
// $Id$

package com.threerings.msoy.client {

import flash.events.IEventDispatcher;
import mx.controls.Button;

import com.threerings.msoy.data.SceneBookmarkEntry;
import com.threerings.msoy.data.MemberObject;

import com.threerings.util.CommandEvent;
import com.threerings.util.Controller;


/**
 * Controller for actions from the ControlBar UI.
 */
public class ControlBarController extends Controller
{
    public static const log :Log = Log.getLog(ControlBarController);

    /** Command to display a volume slider. */
    public static const POP_VOLUME :String = "handlePopVolume";

    /** Command to move back to the previous location. */
    public static const MOVE_BACK :String = "handleMoveBack";

    /**
     * Create the controller.
     */
    public function ControlBarController (ctx :WorldContext, controlBar :ControlBar)
    {
        _ctx = ctx;
        _controlBar = controlBar;
        setControlledPanel(ctx.getStage());
    }

    /**
     * Handle the POP_VOLUME command.
     */
    public function handlePopVolume (trigger :Button) :void
    {
        var popup :VolumePopup = new VolumePopup (trigger);
        popup.show();
    }

    /**
     * Handle the MOVE_BACK command.
     */
    public function handleMoveBack (trigger :Button) :void
    {
        var memberObj :MemberObject = _ctx.getClientObject ();
        var recentScenes :Array = memberObj.recentScenes.toArray ();

        // sort to get the last one
        recentScenes.sort (
            function (sb1 :SceneBookmarkEntry, sb2 :SceneBookmarkEntry) :int
            {
                return int(sb2.lastVisit - sb1.lastVisit);
            });

        // get the last one and go there
        if (recentScenes.length > 1)
        {
            // current scene is always at position [0],
            // so the previous one is at [1].
            var mostRecent :SceneBookmarkEntry = recentScenes[1];
            CommandEvent.dispatch (trigger, MsoyController.GO_SCENE, mostRecent.sceneId);
        }        
            
    }

    // PROTECTED VARIABLES

    /** World information. */
    protected var _ctx :WorldContext;

    /** Control bar that drives these actions. */
    protected var _controlBar :ControlBar;
}

}

