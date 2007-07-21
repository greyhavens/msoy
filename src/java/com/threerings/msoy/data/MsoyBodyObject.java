//
// $Id$

package com.threerings.msoy.data;

import com.threerings.crowd.data.BodyObject;

import com.threerings.whirled.data.ScenePlace;
import com.threerings.whirled.spot.data.ClusteredBodyObject;

/**
 * Does something extraordinary.
 */
public class MsoyBodyObject extends BodyObject
    implements ClusteredBodyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>clusterOid</code> field. */
    public static final String CLUSTER_OID = "clusterOid";
    // AUTO-GENERATED: FIELDS END

    /** The cluster object we're in. */
    public int clusterOid;

    /** The current state of the body's actor avatar, or null if unset/unknown/default. */
    public transient String avatarState;

    /**
     * Returns the id of the scene currently occupied by this member or -1 if we're not in a scene.
     */
    public int getSceneId ()
    {
        return (location instanceof ScenePlace) ? ((ScenePlace)location).sceneId : -1;
    }

    // from ClusteredBodyObject
    public int getClusterOid ()
    {
        return clusterOid;
    }

    // from ClusteredBodyObject
    public String getClusterField ()
    {
        return CLUSTER_OID;
    }

    // AUTO-GENERATED: METHODS START
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
    // AUTO-GENERATED: METHODS END
}
