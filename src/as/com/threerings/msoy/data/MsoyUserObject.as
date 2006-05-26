//
// $Id$

package com.threerings.msoy.data {

import com.threerings.util.Integer;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.crowd.data.BodyObject;

import com.threerings.whirled.spot.data.ClusteredBodyObject;

/**
 * Represents a connected msoy user.
 */
public class MsoyUserObject extends BodyObject
    implements ClusteredBodyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>sceneId</code> field. */
    public static const SCENE_ID :String = "sceneId";

    /** The field name of the <code>clusterOid</code> field. */
    public static const CLUSTER_OID :String = "clusterOid";
    // AUTO-GENERATED: FIELDS END

    /** The scene id that the user is currently occupying. */
    public var sceneId :int;

    /** The object ID of the user's cluster. */
    public var clusterOid :int;

    // documentation inherited from superinterface ScenedBodyObject
    public function getSceneId () :int
    {
        return sceneId;
    }

    // documentation inherited from interface ClusteredBodyObject
    public function getClusterOid () :int
    {
        return clusterOid;
    }

    // documentation inherited from interface ClusteredBodyObject
    public function getClusterField () :String
    {
        return CLUSTER_OID;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>sceneId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public function setSceneId (value :int) :void
    {
        var ovalue :int = this.sceneId;
        requestAttributeChange(
            SCENE_ID, new Integer(value), new Integer(ovalue));
        this.sceneId = value;
    }

    /**
     * Requests that the <code>clusterOid</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public function setClusterOid (value :int) :void
    {
        var ovalue :int = this.clusterOid;
        requestAttributeChange(
            CLUSTER_OID, new Integer(value), new Integer(ovalue));
        this.clusterOid = value;
    }
    // AUTO-GENERATED: METHODS END

    // documentation inherited
    public override function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeInt(sceneId);
        out.writeInt(clusterOid);
    }

    // documentation inherited
    public override function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        sceneId = ins.readInt();
        clusterOid = ins.readInt();
    }
}
}
