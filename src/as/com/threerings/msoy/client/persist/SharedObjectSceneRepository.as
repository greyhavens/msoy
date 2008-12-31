//
// $Id$

package com.threerings.msoy.client.persist {

import flash.net.SharedObject;

import flash.utils.ByteArray;
import flash.utils.Endian;
import flash.utils.getTimer; // function import

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.whirled.client.persist.SceneRepository;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.util.NoSuchSceneError;

/**
 * A client-side scene repository that uses possibly-available
 * SharedObjects to store data.
 */
// TODO: this class is no longer used
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

    // from SceneRepository
    public function loadSceneModel (sceneId :int) :SceneModel
    {
        // retrieve the shared object
        var so :SharedObject = getShared(sceneId);
        if (so == null) {
            throw new NoSuchSceneError(sceneId);
        }

        // if found, try to get the byte array
        var ba :ByteArray = (so.data.model as ByteArray);
        if (ba == null) {
            return null;
        }

        // we need to keep the saved object compressed, so we copy
        // the bytes into another ByteArray to uncompress
        var ba2 :ByteArray = new ByteArray();
        ba2.endian = Endian.BIG_ENDIAN;
        ba2.writeBytes(ba);
        ba2.position = 0;
        ba2.uncompress();
        var ins :ObjectInputStream = new ObjectInputStream(ba2);
        return SceneModel(ins.readObject());
    }

    // from SceneRepository
    public function storeSceneModel (model :SceneModel) :void
    {
        // retrieve the shared object
        var so :SharedObject = getShared(model.sceneId);
        if (so == null) {
            return; // fail silently
        }

        // stream the model to the array, compress
        var ba :ByteArray = new ByteArray();
        ba.endian = Endian.BIG_ENDIAN;
        var out :ObjectOutputStream = new ObjectOutputStream(ba);
        out.writeObject(model);
        ba.compress();

        // store the byte array
        so.data.model = ba;
        so.flush(); // TODO: we could specify a min size..
    }

    // from SceneRepository
    public function deleteSceneModel (sceneId :int) :void
    {
        var so :SharedObject = getShared(sceneId);
        if (so != null) {
            so.clear();
        }
    }

    /**
     * Temporary? code to clear the scene cache.
     */
    public function TEMPClearSceneCache () :void
    {
        trace("Cleaning old scene cache...");
        try {
            // TODO: this is unbelievably slow
            // What we likely need to do is maintain a directory of stored scenes
            // as noted in the constructor's note
            var tooLongStamp :Number = 4000 + getTimer(); // 4 seconds
            for (var ii :int = 0; ii < 500; ii++) {
                deleteSceneModel(ii);
                if (getTimer() > tooLongStamp) {
                    trace("Cleaning old cache is taking too long, aborting...");
                    break;
                }
            }
        } catch (e :Error) {
            trace("Exception cleaning old scene cache:");
            trace(e.getStackTrace());
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
