//
// $Id$

package com.threerings.msoy.data;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
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
    public static final String SCENE_ID = "sceneId";

    /** The field name of the <code>clusterOid</code> field. */
    public static final String CLUSTER_OID = "clusterOid";

    /** The field name of the <code>tokens</code> field. */
    public static final String TOKENS = "tokens";
    // AUTO-GENERATED: FIELDS END

    /** The scene id that the user is currently occupying. */
    public int sceneId;

    /** The object ID of the user's cluster. */
    public int clusterOid;

    /** The tokens defining the access controls for this user. */
    public TokenRing tokens;

    /**
     * Return true if this user is merely a guest.
     */
    public boolean isGuest ()
    {
        // TODO
        return username.toString().startsWith("guest");
    }

    // documentation inherited from superinterface ScenedBodyObject
    public int getSceneId ()
    {
        return sceneId;
    }

    // documentation inherited from interface ClusteredBodyObject
    public int getClusterOid ()
    {
        return clusterOid;
    }

    // documentation inherited from interface ClusteredBodyObject
    public String getClusterField ()
    {
        return CLUSTER_OID;
    }

    // documentation inherited
    public OccupantInfo createOccupantInfo ()
    {
        return new MsoyOccupantInfo(this);
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
    public void setSceneId (int value)
    {
        int ovalue = this.sceneId;
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
    public void setClusterOid (int value)
    {
        int ovalue = this.clusterOid;
        requestAttributeChange(
            CLUSTER_OID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.clusterOid = value;
    }

    /**
     * Requests that the <code>tokens</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setTokens (TokenRing value)
    {
        TokenRing ovalue = this.tokens;
        requestAttributeChange(
            TOKENS, value, ovalue);
        this.tokens = value;
    }
    // AUTO-GENERATED: METHODS END
}
