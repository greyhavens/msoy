//
// $Id: $


package com.threerings.msoy.data;

import javax.annotation.Generated;
import com.threerings.presents.data.ClientObject;

/**
 * The tiny loader client for a member.
 */
public class MemberClientObject extends ClientObject
{
    /** The field name of the <code>bodyOid</code> field. */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public static final String BODY_OID = "bodyOid";
    // AUTO-GENERATED: FIELDS END

    /** The oid of the {@link MemberObject} once it's loaded; zero until then. */
    public int bodyOid;

    /** The {@link }MemberObject} itself, once it's resolved, available only on the server. */ 
    public transient MemberObject memobj;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>bodyOid</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    @Generated(value={"com.threerings.presents.tools.GenDObjectTask"})
    public void setBodyOid (int value)
    {
        int ovalue = this.bodyOid;
        requestAttributeChange(
            BODY_OID, Integer.valueOf(value), Integer.valueOf(ovalue));
        this.bodyOid = value;
    }
    // AUTO-GENERATED: METHODS END
}
