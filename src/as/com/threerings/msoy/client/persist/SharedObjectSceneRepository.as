package com.threerings.msoy.client.persist {

import flash.net.SharedObject;

import com.threerings.whirled.client.persist.SceneRepository;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.util.NoSuchSceneError;

/**
 * A client-side scene repository that uses possibly-available
 * SharedObjects to store data.
 */
public class SharedObjectSceneRepository
    implements SceneRepository
{
    public function SharedObjectSceneRepository ()
    {
        // TODO: we may want to use another shared object as a 'directory'
        // of all shared objects we have stored, for example keeping an array
        // of scene ids for which shared objects exist. We could then attempt
        // to manage the size of the cache and perhaps auto-delete old
        // scenes 
    }

    // documentation inherited from interface SceneRepository
    public function loadSceneModel (sceneId :int) :SceneModel
    {
        var so :SharedObject = getShared(sceneId);
        if (so == null) {
            throw new NoSuchSceneError(sceneId);
        }
        return (so.data.model as SceneModel);
    }

    // documentation inherited from interface SceneRepository
    public function storeSceneModel (model :SceneModel) :void
    {
        var so :SharedObject = getShared(model.sceneId);
        if (so == null) {
            return; // fail silently
        }

        // store the model directly in the shared object
        so.data.model = model;
        so.flush(); // TODO: we could specify a min size..
    }

    // documentation inherited from interface SceneRepository
    public function deleteSceneModel (sceneId :int) :void
    {
        var so :SharedObject = getShared(sceneId);
        if (so != null) {
            so.clear();
        }
    }

    /**
     * Get the shared object to be used for the specified scene.
     */
    protected function getShared (sceneId :int) :SharedObject
    {
        return SharedObject.getLocal("scene_" + sceneId, "/");
    }
}

}
