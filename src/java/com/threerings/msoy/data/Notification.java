//
// $Id$

package com.threerings.msoy.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.util.ActionScript;

/**
 * Notification from the server to the client. Instances of notifications are collected inside
 * a DSet on the member object. Once the client dealt with a notification, it must acknowledge
 * it via an appropriate call to the MemberService, which will remove the notification from
 * the member object.
 */
public class Notification extends SimpleStreamableObject
    implements Comparable, DSet.Entry
{
    /**
     * Id of the notification. This field will be set by the MemberObject to which
     * the notification is sent. Within the scope of that object, each notification 
     * will have a unique id.
     */
    public int id;

    @ActionScript(omit=true)
    public Notification ()
    {
        // nothing special
    }        

    // from DSet.Entry
    public Comparable getKey ()
    {
        return id;
    }

    // from Comparable
    public int compareTo (Object other)
    {
        Notification that = (Notification) other;
        return this.id - that.id;
    }
}
