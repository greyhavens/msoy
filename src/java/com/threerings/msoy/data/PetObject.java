//
// $Id$

package com.threerings.msoy.data;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.whirled.spot.data.ClusteredBodyObject;

import com.threerings.msoy.item.web.Pet;
import com.threerings.msoy.world.data.WorldPetInfo;

/**
 * Contains the distributed state associated with a Pet.
 */
public class PetObject extends BodyObject
    implements ClusteredBodyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>sceneId</code> field. */
    public static final String SCENE_ID = "sceneId";

    /** The field name of the <code>clusterOid</code> field. */
    public static final String CLUSTER_OID = "clusterOid";

    /** The field name of the <code>pet</code> field. */
    public static final String PET = "pet";

    /** The field name of the <code>followId</code> field. */
    public static final String FOLLOW_ID = "followId";
    // AUTO-GENERATED: FIELDS END

    /** The scene id that the user is currently occupying. */
    public int sceneId;

    /** The object ID of the user's cluster. */
    public int clusterOid;

    /** The digital item from whence this pet came. */
    public Pet pet;

    /** The member id of our owner if we are following them, 0 otherwise. */
    public int followId;

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

    @Override // from BodyObject
    public OccupantInfo createOccupantInfo (PlaceObject plobj)
    {
        return new WorldPetInfo(this, followId != 0);
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
     * Requests that the <code>pet</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setPet (Pet value)
    {
        Pet ovalue = this.pet;
        requestAttributeChange(
            PET, value, ovalue);
        this.pet = value;
    }

    /**
     * Requests that the <code>followId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setFollowId (int value)
    {
        int ovalue = this.followId;
        requestAttributeChange(
            FOLLOW_ID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.followId = value;
    }
    // AUTO-GENERATED: METHODS END
}
