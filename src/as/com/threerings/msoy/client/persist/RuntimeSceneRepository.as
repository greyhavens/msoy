//
// $Id$

package com.threerings.msoy.client.persist {

import flash.utils.ByteArray;
import flash.utils.Dictionary;
import flash.utils.Endian;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.whirled.client.persist.SceneRepository;

import com.threerings.whirled.data.SceneModel;
import com.threerings.whirled.util.NoSuchSceneError;

/**
 * A client-side scene repository that caches the last 20 scenes without
 * ever persisting them.
 */
public class RuntimeSceneRepository
    implements SceneRepository
{
    /**
     */
    public function RuntimeSceneRepository ()
    {
    }

    // from SceneRepository
    public function loadSceneModel (sceneId :int) :SceneModel
    {
        var index :int = _order.indexOf(sceneId);
        if (index == -1) {
            throw new NoSuchSceneError(sceneId);
        }

        // move the sceneId to the back of the array to indicate that it was used
        _order.splice(index, 1);
        _order.push(sceneId);

        // load the data!
        var ba :ByteArray = (_scenes[sceneId] as ByteArray);

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
        var sceneId :int = model.sceneId;

        // first, try streaming the new scene
        var ba :ByteArray = new ByteArray();
        ba.endian = Endian.BIG_ENDIAN;
        var out :ObjectOutputStream = new ObjectOutputStream(ba);
        out.writeObject(model);
        ba.compress();

        // delete any previous copy (and remove the sceneId from _order)
        deleteSceneModel(sceneId);

        // now add the new scene
        _order.push(sceneId);
        _scenes[sceneId] = ba;

        // and if we're over the maximum, delete the oldest
        if (_order.length > MAX) {
            deleteSceneModel(_order[0] as int);
        }
    }

    // from SceneRepository
    public function deleteSceneModel (sceneId :int) :void
    {
        var index :int = _order.indexOf(sceneId);
        if (index != -1) {
            _order.splice(index, 1);
            delete _scenes[sceneId];
        }
    }

    /** The storage area where we stash the scenes. */
    protected var _scenes :Dictionary = new Dictionary();

    /** The LRU order of the scenes. Index 0 is the oldest scene. */
    protected var _order :Array = [];

    /** The maximum number of scenes to cache. */
    protected static const MAX :int = 20;
}
}
