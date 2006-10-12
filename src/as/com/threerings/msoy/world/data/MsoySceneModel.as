package com.threerings.msoy.world.data {

import com.threerings.util.ArrayIterator;
import com.threerings.util.ArrayUtil;
import com.threerings.util.Hashtable;
import com.threerings.util.Iterator;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.TypedArray;

import com.threerings.whirled.data.SceneModel;

import com.threerings.whirled.spot.data.Portal;

import com.threerings.msoy.item.web.MediaDesc;

public class MsoySceneModel extends SceneModel
{
    /** A type constant indicating a normal room where defaultly
     * draw some walls. */
    public static const DRAWN_ROOM :int = 0;

    /** A type constant indicating a room where the background image should
     * be drawn covering everything, but layered behind everything else such
     * that the background image IS the scene to the viewer. */
    public static const IMAGE_OVERLAY :int = 1;

    /** The number of type constants. */
    public static const TYPE_COUNT :int = 2;

    /** The type of scene. */
    public var type :int;

    /** The memberId of the owner of this scene. */
    public var ownerId :int;

    /** The default entrance for this scene. */
    public var defaultEntranceId :int;

    /** The "pixel" depth of the room. */
    public var depth :int;

    /** The pixel width of the room. */
    public var width :int;

    /** A value between 0 - 1, for the height of the horizon in the room. */
    public var horizon :Number;

    /** The furniture in the scene. */
    public var furnis :TypedArray;

    /**
     * Add a piece of furniture to this model.
     */
    public function addFurni (furni :FurniData) :void
    {
        furnis.push(furni);
        invalidatePortalInfo(furni);
    }

    /**
     * Remove a piece of furniture to this model.
     */
    public function removeFurni (furni :FurniData) :void
    {
        ArrayUtil.removeFirst(furnis, furni);
        invalidatePortalInfo(furni);
    }

    /**
     * Get the next available furni id.
     */
    public function getNextFurniId () :int
    {
        var length :int = (furnis == null) ? 0 : furnis.length;
        for (var ii :int = 1; ii < 5000; ii++) {
            var found :Boolean = false;
            for (var idx :int = 0; idx < length; idx++) {
                if ((furnis[idx] as FurniData).id == ii) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return ii;
            }
        }
        return -1;
    }

    /**
     * Support for SpotScene.
     */
    public function getDefaultEntrance () :Portal
    {
        // Note that we can't just call getPortal(_defaultPortalId) because
        // we have to validate prior to accessing _defaultPortalId.
        validatePortalInfo();
        return (_portalInfo.get(_defaultPortalId) as Portal);
    }

    /**
     * Support for SpotScene.
     */
    public function getPortal (portalId :int) :Portal
    {
        validatePortalInfo();
        return (_portalInfo.get(portalId) as Portal);
    }
    
    /**
     * Support for SpotScene.
     */
    public function getPortalCount () :int
    {
        validatePortalInfo();
        return _portalInfo.size();
    }

    /**
     * Support for SpotScene.
     */
    public function getPortals () :Iterator
    {
        validatePortalInfo();
        return new ArrayIterator(_portalInfo.values());
    }

    /**
     * Invalidate our portal info if the specified piece of furniture
     * is a portal.
     */
    protected function invalidatePortalInfo (
        changedFurni :FurniData = null) :void
    {
        if (changedFurni == null ||
                changedFurni.actionType == FurniData.ACTION_PORTAL) {
            _portalInfo = null;
        }
    }

    /**
     * Validate that the portalInfo is up-to-date and ready to use.
     */
    protected function validatePortalInfo () :void
    {
        if (_portalInfo != null) {
            return;
        }

        _portalInfo = new Hashtable();
        _defaultPortalId = int.MIN_VALUE;
        for each (var furni :FurniData in furnis) {
            if (furni.actionType != FurniData.ACTION_PORTAL) {
                continue;
            }

            var vals :Array = furni.actionData.split(":");

            var p :Portal = new Portal();
            p.portalId = furni.id;
            p.loc = furni.loc;
            p.targetSceneId = int(vals[0]);
            p.targetPortalId = int(vals[1]);

            // TODO: something real here.. :)
            if (_defaultPortalId == int.MIN_VALUE) {
                _defaultPortalId = p.portalId;
            }

            // remember this portal
            _portalInfo.put(p.portalId, p);
        }
    }

    override public function clone () :Object
    {
        var model :MsoySceneModel = (super.clone() as MsoySceneModel);

        model.type = type;
        model.ownerId = ownerId;
        model.defaultEntranceId = defaultEntranceId;
        model.depth = depth;
        model.width = width;
        model.horizon = horizon;
        model.furnis = (furnis.clone() as TypedArray);

        return model;
    }

    // documentation inherited
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeByte(type);
        out.writeInt(ownerId);
        out.writeShort(defaultEntranceId);
        out.writeShort(depth);
        out.writeShort(width);
        out.writeFloat(horizon);
        out.writeObject(furnis);
    }

    // documentation inherited
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        type = ins.readByte();
        ownerId = ins.readInt();
        defaultEntranceId = ins.readShort();
        depth = ins.readShort();
        width = ins.readShort();
        horizon = ins.readFloat();
        furnis = (ins.readObject() as TypedArray);
    }

    override public function toString () :String
    {
        return "MsoySceneModel[\"" + name + "\" (" + sceneId + ")" +
            ", version=" + version + ", type=" + type + "]";
    }

    /** Cached portal info. Not streamed. */
    protected var _portalInfo :Hashtable;
    protected var _defaultPortalId :int;
}
}
