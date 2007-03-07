//
// $Id$

package com.threerings.msoy.data;

import com.threerings.crowd.data.BodyObject;

import com.threerings.whirled.spot.data.ClusteredBodyObject;

/**
 * Does something extraordinary.
 */
public class MsoyBodyObject extends BodyObject
    implements ClusteredBodyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>sceneId</code> field. */
    public static final String SCENE_ID = "sceneId";

    /** The field name of the <code>clusterOid</code> field. */
    public static final String CLUSTER_OID = "clusterOid";
    // AUTO-GENERATED: FIELDS END

    /** The sceneId we occupy. */
    public int sceneId;

    /** The cluster object we're in. */
    public int clusterOid;

    // from ScenedBodyObject
    public int getSceneId ()
    {
        return sceneId;
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
    // AUTO-GENERATED: METHODS END
}
