//
// $Id$

package com.threerings.msoy.bureau.data;

import com.threerings.presents.data.ClientObject;

/** Client object purely for distinguishing windows in service methods. */
public class WindowClientObject extends ClientObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>bureauId</code> field. */
    public static final String BUREAU_ID = "bureauId";
    // AUTO-GENERATED: FIELDS END

    /** The bureau id of the owner of this window. */
    public String bureauId;

    // AUTO-GENERATED: METHODS START
    /**
     * Requests that the <code>bureauId</code> field be set to the
     * specified value. The local value will be updated immediately and an
     * event will be propagated through the system to notify all listeners
     * that the attribute did change. Proxied copies of this object (on
     * clients) will apply the value change when they received the
     * attribute changed notification.
     */
    public void setBureauId (String value)
    {
        String ovalue = this.bureauId;
        requestAttributeChange(
            BUREAU_ID, value, ovalue);
        this.bureauId = value;
    }
    // AUTO-GENERATED: METHODS END
}
