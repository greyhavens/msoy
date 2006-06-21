//
// $Id$

package com.threerings.msoy.data {

import com.threerings.util.Integer;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.TokenRing;

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

    /** The field name of the <code>tokens</code> field. */
    public static const TOKENS :String = "tokens";
    // AUTO-GENERATED: FIELDS END

    /** The scene id that the user is currently occupying. */
    public var sceneId :int;

    /** The object ID of the user's cluster. */
    public var clusterOid :int;

    /** The tokens defining the access controls for this user. */
    public var tokens :TokenRing;

    /**
     * Return true if this user is merely a guest.
     */
    public function isGuest () :Boolean
    {
        return username.toString().indexOf("guest") == 0;
    }

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

    // documentation inherited
    override public function getTokens () :TokenRing
    {
        return tokens;
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

    public function setTokens (value :TokenRing) :void
    {
        var ovalue :TokenRing = this.tokens;
        requestAttributeChange(
            TOKENS, value, ovalue);
        this.tokens = value;
    }
    // AUTO-GENERATED: METHODS END

    // documentation inherited
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeInt(sceneId);
        out.writeInt(clusterOid);
        out.writeObject(tokens);
    }

    // documentation inherited
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        sceneId = ins.readInt();
        clusterOid = ins.readInt();
        tokens = (ins.readObject() as TokenRing);
    }
}
}
