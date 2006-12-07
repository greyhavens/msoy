package com.threerings.msoy.world.data {

import com.threerings.util.ArrayIterator;
import com.threerings.util.ArrayUtil;
import com.threerings.util.Hashtable;
import com.threerings.util.Iterator;
import com.threerings.util.Short;

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

    /** Constant for Member room owners **/
    public static const OWNER_TYPE_MEMBER :int = 1;

    /** Constant for Group room owners **/
    public static const OWNER_TYPE_GROUP :int = 2;

    /** The type of owner that owns this scene. */
    public var ownerType :int;

    /** The id of the owner of this scene, interpreted using ownerType. */
    public var ownerId :int;

    /** The type of scene. */
    public var sceneType :int;

    /** The "pixel" depth of the room. */
    public var depth :int;

    /** The pixel width of the room. */
    public var width :int;

    /** A value between 0 - 1, for the height of the horizon in the room. */
    public var horizon :Number;

    /** The furniture in the scene. */
    public var furnis :TypedArray;

    /** The entrance location. */
    public var entrance :MsoyLocation;

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
    public function getNextFurniId (aboveId :int) :int
    {
        if (aboveId > Short.MAX_VALUE || aboveId < Short.MIN_VALUE) {
            aboveId = Short.MIN_VALUE;
        }
        var length :int = (furnis == null) ? 0 : furnis.length;
        for (var ii :int = aboveId + 1; ii != aboveId; ii++) {
            if (ii > Short.MAX_VALUE) {
                ii = Short.MIN_VALUE;
                if (ii == aboveId) {
                    break;
                }
            }
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
        var p :Portal = new Portal();
        p.portalId = -1;
        p.loc = entrance;
        p.targetSceneId = sceneId;
        p.targetPortalId = -1;

        return p;
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
        // if non-null, we're already valid
        if (_portalInfo != null) {
            return;
        }

        _portalInfo = new Hashtable();
        for each (var furni :FurniData in furnis) {
            if (furni.actionType != FurniData.ACTION_PORTAL) {
                continue;
            }

            var vals :Array = furni.splitActionData();

            var p :Portal = new Portal();
            p.portalId = furni.id;
            p.loc = furni.loc;
            p.targetSceneId = int(vals[0]);
            p.targetPortalId = -1; // int(vals[1]);

            // remember this portal
            _portalInfo.put(p.portalId, p);
        }
    }

    override public function clone () :Object
    {
        var model :MsoySceneModel = (super.clone() as MsoySceneModel);

        model.ownerType = ownerType;
        model.ownerId = ownerId;
        model.sceneType = sceneType;
        model.depth = depth;
        model.width = width;
        model.horizon = horizon;
        model.furnis = (furnis.clone() as TypedArray);
        model.entrance = entrance;

        return model;
    }

    // documentation inherited
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeByte(ownerType);
        out.writeInt(ownerId);
        out.writeByte(sceneType);
        out.writeShort(depth);
        out.writeShort(width);
        out.writeFloat(horizon);
        out.writeObject(furnis);
        out.writeObject(entrance);
    }

    // documentation inherited
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        ownerType = ins.readByte();
        ownerId = ins.readInt();
        sceneType = ins.readByte();
        depth = ins.readShort();
        width = ins.readShort();
        horizon = ins.readFloat();
        furnis = (ins.readObject() as TypedArray);
        entrance = (ins.readObject() as MsoyLocation);
    }

    override public function toString () :String
    {
        return "MsoySceneModel[\"" + name + "\" (" + sceneId + ")" +
            ", version=" + version + ", sceneType=" + sceneType + "]";
    }

    /** Cached portal info. Not streamed. */
    protected var _portalInfo :Hashtable;
}
}
