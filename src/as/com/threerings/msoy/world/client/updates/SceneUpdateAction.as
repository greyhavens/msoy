//
// $Id$

package com.threerings.msoy.world.client.updates {

import com.threerings.io.TypedArray;
import com.threerings.msoy.client.WorldContext;
import com.threerings.whirled.data.SceneUpdate;
import com.threerings.msoy.world.client.RoomController;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.SceneAttrsUpdate;


/**
 * Generates a command to update scene attributes.
 */
public class SceneUpdateAction
    implements UpdateAction
{
    /** Create a new furni update command. */
    public function SceneUpdateAction (ctx :WorldContext, oldScene :MsoyScene, newScene :MsoyScene)
    {
        _ctx = ctx;
        _oldSceneData = new Object();
        _newSceneData = new Object();

        copySceneAttributes(oldScene.getSceneModel(), _oldSceneData);
        copySceneAttributes(newScene.getSceneModel(), _newSceneData);
    }

    // documentation inherited 
    public function makeApply () :SceneUpdate
    {
        return makeUpdate(_newSceneData);
    }

    // documentation inherited 
    public function makeUndo () :SceneUpdate
    {
        return makeUpdate(_oldSceneData);
    }

    /** Makes a shallow copy of the salient scene attributes from one object to another. */
    protected function copySceneAttributes (from :Object, to :Object) :void
    {
        for each (var attribute :String in ATTRS_TO_COPY) {
            // sanity check, since we're circumventing model definitions 
            if (! from.hasOwnProperty(attribute)) {
                Log.getLog(this).warning("UpdateAction: trying to copy absent attribute " +
                                         "[attribute=" + attribute + "]");
            }
                                         
            to[attribute] = from[attribute];
        }
    }
    
    /** Creates a SceneUpdate to send to the server. */
    protected function makeUpdate (sceneData :Object) :SceneUpdate
    {
        var currentScene :MsoyScene = _ctx.getSceneDirector().getScene() as MsoyScene;
        var attrUpdate :SceneAttrsUpdate = new SceneAttrsUpdate();

        attrUpdate.init(currentScene.getId(), currentScene.getVersion());
        copySceneAttributes(sceneData, attrUpdate);
        return attrUpdate;
    }

    /** Names of scene attributes that will be updated during an update. */
    protected static const ATTRS_TO_COPY :Array =
        [ "name", "accessControl", "decor", "audioData", "entrance" ];

    protected var _oldSceneData :Object;
    protected var _newSceneData :Object;
    protected var _ctx :WorldContext;

}
}
