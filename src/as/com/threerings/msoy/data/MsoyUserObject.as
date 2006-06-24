//
// $Id$

package com.threerings.msoy.data {

import com.threerings.util.Integer;
import com.threerings.util.Short;

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

    /** The field name of the <code>avatar</code> field. */
    public static const AVATAR :String = "avatar";

    /** The field name of the <code>chatStyle</code> field. */
    public static const CHAT_STYLE :String = "chatStyle";

    /** The field name of the <code>chatPopStyle</code> field. */
    public static const CHAT_POP_STYLE :String = "chatPopStyle";
    // AUTO-GENERATED: FIELDS END

    /** The scene id that the user is currently occupying. */
    public var sceneId :int;

    /** The object ID of the user's cluster. */
    public var clusterOid :int;

    /** The tokens defining the access controls for this user. */
    public var tokens :TokenRing;

    /** The avatar that the user has chosen. */
    public var avatar :MediaData;

    /** The style of our chat. */
    public var chatStyle :int;

    /** The pop style of our chat. */
    public var chatPopStyle :int;

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
            SCENE_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
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
            CLUSTER_OID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.clusterOid = value;
    }

    public function setTokens (value :TokenRing) :void
    {
        var ovalue :TokenRing = this.tokens;
        requestAttributeChange(
            TOKENS, value, ovalue);
        this.tokens = value;
    }

    /**
     * Requests that the <code>avatar</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public function setAvatar (value :MediaData) :void
    {
        var ovalue :MediaData = this.avatar;
        requestAttributeChange(
            AVATAR, value, ovalue);
        this.avatar = value;
    }

    /**
     * Requests that the <code>chatStyle</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public function setChatStyle (value :int) :void
    {
        var ovalue :int = this.chatStyle;
        requestAttributeChange(
            CHAT_STYLE, Short.valueOf(value), Short.valueOf(ovalue));
        this.chatStyle = value;
    }

    /**
     * Requests that the <code>chatPopStyle</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public function setChatPopStyle (value :int) :void
    {
        var ovalue :int = this.chatPopStyle;
        requestAttributeChange(
            CHAT_POP_STYLE, Short.valueOf(value), Short.valueOf(ovalue));
        this.chatPopStyle = value;
    }
    // AUTO-GENERATED: METHODS END

    // documentation inherited
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);

        out.writeInt(sceneId);
        out.writeInt(clusterOid);
        out.writeObject(tokens);
        out.writeObject(avatar);
        out.writeShort(chatStyle);
        out.writeShort(chatPopStyle);
    }

    // documentation inherited
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        sceneId = ins.readInt();
        clusterOid = ins.readInt();
        tokens = (ins.readObject() as TokenRing);
        avatar = (ins.readObject() as MediaData);
        chatStyle = ins.readShort();
        chatPopStyle = ins.readShort();
    }
}
}
