//
// $Id$

package com.threerings.msoy.world.client.updates {

import com.threerings.io.TypedArray;

import com.threerings.whirled.data.Scene;
import com.threerings.whirled.data.SceneUpdate;

import com.threerings.msoy.world.client.RoomController;
import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.ModifyFurniUpdate;

/**
 * Generates a command to update a single piece of furni.
 */
public class FurniUpdateAction
    implements UpdateAction
{
    /** Create a new furni update command. */
    public function FurniUpdateAction (ctx :WorldContext, oldData :FurniData, newData :FurniData)
    {
        _ctx = ctx;
        _oldData = oldData;
        _newData = newData;
    }

    // documentation inherited from
    public function makeApply () :SceneUpdate
    {
        return makeUpdate(_oldData, _newData);
    }

    // documentation inherited
    public function makeUndo () :SceneUpdate
    {
        return makeUpdate(_newData, _oldData);
    }

    /**
     * Makes an update to send to the server. /toRemove/ will be removed, and /toAdd/ added.
     */
    protected function makeUpdate (toRemove :FurniData, toAdd :FurniData) :SceneUpdate
    {
        var addedData :TypedArray;
        var removedData :TypedArray;

        if (toAdd != null) {
            addedData = TypedArray.create(FurniData);
            addedData.push(toAdd);
        }

        if (toRemove != null) {
            removedData = TypedArray.create(FurniData);
            removedData.push(toRemove);
        }

        var scene :Scene = _ctx.getSceneDirector().getScene();
        var furniUpdate :ModifyFurniUpdate = new ModifyFurniUpdate();
        furniUpdate.initialize(scene.getId(), scene.getVersion(), removedData, addedData);

        return furniUpdate;
    }

    protected var _oldData :FurniData;
    protected var _newData :FurniData;
    protected var _ctx :WorldContext;
}
}
