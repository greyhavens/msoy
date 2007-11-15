//
// $Id$

package com.threerings.msoy.data;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.item.data.all.Pet;
import com.threerings.msoy.world.data.PetInfo;

/**
 * Contains the distributed state associated with a Pet.
 */
public class PetObject extends MsoyBodyObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>pet</code> field. */
    public static final String PET = "pet";

    /** The field name of the <code>followId</code> field. */
    public static final String FOLLOW_ID = "followId";
    // AUTO-GENERATED: FIELDS END

    /** The digital item from whence this pet came. */
    public Pet pet;

    /** The member id of our owner if we are following them, 0 otherwise. */
    public int followId;

    @Override // from BodyObject
    public OccupantInfo createOccupantInfo (PlaceObject plobj)
    {
        return new PetInfo(this, followId != 0);
    }

    // AUTO-GENERATED: METHODS START
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
