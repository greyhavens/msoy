//
// $Id$

package com.threerings.msoy.room.data {

import com.threerings.util.ArrayIterator;
import com.threerings.util.ArrayUtil;
import com.threerings.util.HashMap;
import com.threerings.util.Iterator;
import com.threerings.util.Name;
import com.threerings.util.Short;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.TypedArray;

import com.threerings.whirled.data.SceneModel;

import com.threerings.whirled.spot.data.Portal;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.DefaultItemMediaDesc;
import com.threerings.msoy.item.data.all.Item;

public class MsoySceneModel extends SceneModel
{
    /** Constant for Member room owners **/
    public static const OWNER_TYPE_MEMBER :int = 1;

    /** Constant for Group room owners **/
    public static const OWNER_TYPE_GROUP :int = 2;

    /** Access control constant, denotes that anyone can enter this scene. */
    public static const ACCESS_EVERYONE :int = 0;

    /** Access control constant, denotes that only the scene owner and friends
     *  (or group manager and members, in case of a group scene) can enter this scene. */ 
    public static const ACCESS_OWNER_AND_FRIENDS :int = 1;
    
    /** Access control constant, denotes that only the scene owner (or group manager,
     *  in case of a group scene) can enter this scene. */
    public static const ACCESS_OWNER_ONLY :int = 2;

    /** Access control, as one of the ACCESS constants. Limits who can enter the scene. */
    public var accessControl :int;
    
    /** The type of owner that owns this scene. */
    public var ownerType :int;

    /** The id of the owner of this scene, interpreted using ownerType. */
    public var ownerId :int;

    /** The name of the owner, either a MemberName or the group's name. */
    public var ownerName :Name;

    /** The game associated with this room (usually the group's game), or 0 if none. */
    public var gameId :int;

    /** The furniture in the scene. */
    public var furnis :TypedArray /* of FurniData */;

    /** The entrance location. */
    public var entrance :MsoyLocation;

    /** Decor item reference. */
    public var decor :Decor;
    
    /** Audio data representation. */
    public var audioData :AudioData;

    /** Constructor. */
    public function MsoySceneModel ()
    {
        audioData = new AudioData();
    }

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

        _portalInfo = new HashMap();
        for each (var furni :FurniData in furnis) {
            if (furni.actionType != FurniData.ACTION_PORTAL) {
                continue;
            }
            var p :MsoyPortal = new MsoyPortal(furni);
            _portalInfo.put(p.portalId, p);
        }
    }

    override public function clone () :Object
    {
        var model :MsoySceneModel = (super.clone() as MsoySceneModel);
        model.accessControl = accessControl;
        model.ownerType = ownerType;
        model.ownerId = ownerId;
        model.ownerName = ownerName;
        model.gameId = gameId;
        model.furnis = (furnis.clone() as TypedArray);
        model.entrance = (entrance.clone() as MsoyLocation);
        model.decor = decor;
        model.audioData = (audioData == null) ? null : (audioData.clone() as AudioData);
        return model;
    }

    // documentation inherited
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeByte(accessControl);
        out.writeByte(ownerType);
        out.writeInt(ownerId);
        out.writeObject(ownerName);
        out.writeInt(gameId);
        out.writeObject(furnis);
        out.writeObject(entrance);
        out.writeObject(decor);
        out.writeObject(audioData);
    }

    // documentation inherited
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        accessControl = ins.readByte();
        ownerType = ins.readByte();
        ownerId = ins.readInt();
        ownerName = (ins.readObject() as Name);
        gameId = ins.readInt();
        furnis = (ins.readObject() as TypedArray);
        entrance = (ins.readObject() as MsoyLocation);
        decor = (ins.readObject() as Decor);
        audioData = (ins.readObject() as AudioData);
    }

    override public function toString () :String
    {
        return "MsoySceneModel[\"" + name + "\" (" + sceneId + ")" +
            ", version=" + version + ", sceneType=" + decor.type +
            ", decorId=" + decor.itemId + ", audio=" + audioData.itemId + "]";
    }

    /**
     * Create a default decor for a blank scene. The decor will not be completely filled in,
     * because it doesn't correspond to an entity inside the database, but it has enough
     * to be displayed inside the room.
     */
    public static function defaultMsoySceneModelDecor () :Decor
    {
        var decor :Decor = new Decor();
        decor.itemId = 0; // doesn't correspond to an object
        decor.setFurniMedia(
            new DefaultItemMediaDesc(MediaDesc.IMAGE_PNG, Item.DECOR, Item.MAIN_MEDIA));
        decor.type = Decor.IMAGE_OVERLAY;
        decor.hideWalls = false;
        decor.width = 800;
        decor.height = 494;
        decor.depth = 400;
        decor.horizon = 0.5;
        decor.scale = 1;
        decor.offsetX = 0;
        decor.offsetY = 0;
        return decor;
    }
    
    /** Cached portal info. Not streamed. */
    protected var _portalInfo :HashMap;
}
}
