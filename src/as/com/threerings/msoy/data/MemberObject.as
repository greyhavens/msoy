//
// $Id$

package com.threerings.msoy.data {

import com.threerings.util.Integer;
import com.threerings.util.Name;
import com.threerings.util.Short;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.TokenRing;

import com.threerings.whirled.spot.data.ClusteredBodyObject;

import com.threerings.msoy.item.web.Avatar;

/**
 * Represents a connected msoy user.
 */
public class MemberObject extends BodyObject
    implements ClusteredBodyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>memberName</code> field. */
    public static const MEMBER_NAME :String = "memberName";

    /** The field name of the <code>sceneId</code> field. */
    public static const SCENE_ID :String = "sceneId";

    /** The field name of the <code>clusterOid</code> field. */
    public static const CLUSTER_OID :String = "clusterOid";

    /** The field name of the <code>recentScenes</code> field. */
    public static const RECENT_SCENES :String = "recentScenes";

    /** The field name of the <code>tokens</code> field. */
    public static const TOKENS :String = "tokens";

    /** The field name of the <code>homeSceneId</code> field. */
    public static const HOME_SCENE_ID :String = "homeSceneId";

    /** The field name of the <code>avatar</code> field. */
    public static const AVATAR :String = "avatar";

    /** The field name of the <code>chatStyle</code> field. */
    public static const CHAT_STYLE :String = "chatStyle";

    /** The field name of the <code>chatPopStyle</code> field. */
    public static const CHAT_POP_STYLE :String = "chatPopStyle";

    /** The field name of the <code>friends</code> field. */
    public static const FRIENDS :String = "friends";
    // AUTO-GENERATED: FIELDS END

    /** The member name and id for this user. */
    public var memberName :MemberName;

    /** The scene id that the user is currently occupying. */
    public var sceneId :int;

    /** The object ID of the user's cluster. */
    public var clusterOid :int;

    /** The recent scenes we've been through. */
    public var recentScenes :DSet;
    
    /** The scenes we own. */
    public var ownedScenes :DSet;

    /** The tokens defining the access controls for this user. */
    public var tokens :MsoyTokenRing;

    /** The id of the user's home scene. */
    public var homeSceneId :int;

    /** The avatar that the user has chosen, or null for guests. */
    public var avatar :Avatar;

    /** The style of our chat. */
    public var chatStyle :int;

    /** The pop style of our chat. */
    public var chatPopStyle :int;

    /** The buddies of this player. */
    public var friends :DSet;

    /**
     * Return this member's unique id.
     */
    public function getMemberId () :int
    {
        return (memberName == null) ? 0 : memberName.getMemberId();
    }

    /**
     * Return true if this user is merely a guest.
     */
    public function isGuest () :Boolean
    {
        return (getMemberId() <= 0);
    }

    /**
     * Get a sorted list of friends that are "real" friends
     * (the friendship is not in a pending state).
     */
    public function getSortedEstablishedFriends () :Array
    {
        var friends :Array = this.friends.toArray();
        friends = friends.filter(
            function (fe :FriendEntry, index :int, arr :Array) :Boolean {
                return (fe.status == FriendEntry.FRIEND);
            });
        friends = friends.sort(
            function (fe1 :FriendEntry, fe2 :FriendEntry) :int {
                return MemberName.BY_DISPLAY_NAME(fe1.name, fe2.name);
            });
        return friends;
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

    override public function getVisibleName () :Name
    {
        return memberName;
    }

//    // AUTO-GENERATED: METHODS START
//    /**
//     * Requests that the <code>sceneId</code> field be set to the
//     * specified value. The local value will be updated immediately and an
//     * event will be propagated through the system to notify all listeners
//     * that the attribute did change. Proxied copies of this object (on
//     * clients) will apply the value change when they received the
//     * attribute changed notification.
//     */
//    public function setSceneId (value :int) :void
//    {
//        var ovalue :int = this.sceneId;
//        requestAttributeChange(
//            SCENE_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
//        this.sceneId = value;
//    }
//
//    /**
//     * Requests that the <code>clusterOid</code> field be set to the
//     * specified value. The local value will be updated immediately and an
//     * event will be propagated through the system to notify all listeners
//     * that the attribute did change. Proxied copies of this object (on
//     * clients) will apply the value change when they received the
//     * attribute changed notification.
//     */
//    public function setClusterOid (value :int) :void
//    {
//        var ovalue :int = this.clusterOid;
//        requestAttributeChange(
//            CLUSTER_OID, Integer.valueOf(value), Integer.valueOf(ovalue));
//        this.clusterOid = value;
//    }
//
//    /**
//     * Requests that the <code>tokens</code> field be set to the
//     * specified value. The local value will be updated immediately and an
//     * event will be propagated through the system to notify all listeners
//     * that the attribute did change. Proxied copies of this object (on
//     * clients) will apply the value change when they received the
//     * attribute changed notification.
//     */
//    public function setTokens (value :MsoyTokenRing) :void
//    {
//        var ovalue :MsoyTokenRing = this.tokens;
//        requestAttributeChange(
//            TOKENS, value, ovalue);
//        this.tokens = value;
//    }
//
//    /**
//     * Requests that the <code>homeSceneId</code> field be set to the
//     * specified value. The local value will be updated immediately and an
//     * event will be propagated through the system to notify all listeners
//     * that the attribute did change. Proxied copies of this object (on
//     * clients) will apply the value change when they received the
//     * attribute changed notification.
//     */
//    public function setHomeSceneId (value :int) :void
//    {
//        var ovalue :int = this.homeSceneId;
//        requestAttributeChange(
//            HOME_SCENE_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
//        this.homeSceneId = value;
//    }
//
//    /**
//     * Requests that the <code>avatar</code> field be set to the
//     * specified value. The local value will be updated immediately and an
//     * event will be propagated through the system to notify all listeners
//     * that the attribute did change. Proxied copies of this object (on
//     * clients) will apply the value change when they received the
//     * attribute changed notification.
//     */
//    public function setAvatar (value :MediaDesc) :void
//    {
//        var ovalue :MediaDesc = this.avatar;
//        requestAttributeChange(
//            AVATAR, value, ovalue);
//        this.avatar = value;
//    }
//
//    /**
//     * Requests that the <code>chatStyle</code> field be set to the
//     * specified value. The local value will be updated immediately and an
//     * event will be propagated through the system to notify all listeners
//     * that the attribute did change. Proxied copies of this object (on
//     * clients) will apply the value change when they received the
//     * attribute changed notification.
//     */
//    public function setChatStyle (value :int) :void
//    {
//        var ovalue :int = this.chatStyle;
//        requestAttributeChange(
//            CHAT_STYLE, Short.valueOf(value), Short.valueOf(ovalue));
//        this.chatStyle = value;
//    }
//
//    /**
//     * Requests that the <code>chatPopStyle</code> field be set to the
//     * specified value. The local value will be updated immediately and an
//     * event will be propagated through the system to notify all listeners
//     * that the attribute did change. Proxied copies of this object (on
//     * clients) will apply the value change when they received the
//     * attribute changed notification.
//     */
//    public function setChatPopStyle (value :int) :void
//    {
//        var ovalue :int = this.chatPopStyle;
//        requestAttributeChange(
//            CHAT_POP_STYLE, Short.valueOf(value), Short.valueOf(ovalue));
//        this.chatPopStyle = value;
//    }
//    // AUTO-GENERATED: METHODS END
//
//    override public function writeObject (out :ObjectOutputStream) :void
//    {
//        super.writeObject(out);
//
//        out.writeObject(memberName);
//        out.writeInt(sceneId);
//        out.writeInt(clusterOid);
//        out.writeObject(recentScenes);
//        out.writeObject(tokens);
//        out.writeInt(homeSceneId);
//        out.writeObject(avatar);
//        out.writeShort(chatStyle);
//        out.writeShort(chatPopStyle);
//        out.writeObject(friends);
//    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        memberName = (ins.readObject() as MemberName);
        sceneId = ins.readInt();
        clusterOid = ins.readInt();
        recentScenes = (ins.readObject() as DSet);
        ownedScenes = (ins.readObject() as DSet);
        tokens = (ins.readObject() as MsoyTokenRing);
        homeSceneId = ins.readInt();
        avatar = (ins.readObject() as Avatar);
        chatStyle = ins.readShort();
        chatPopStyle = ins.readShort();
        friends = (ins.readObject() as DSet);
    }
}
}
