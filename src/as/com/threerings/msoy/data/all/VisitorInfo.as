//
// $Id$

package com.threerings.msoy.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

/**
 * Information about the unique visitor to the site. Initially it's stored in a cookie, until the
 * user registers, in which case it gets added to their account. The tracking number gets used to
 * assign the visitor to test groups, and to assemble a history of their activities up to
 * registration.
 *
 * This is the ActionScript version of VisitorInfo.java
 */
public class VisitorInfo
    implements Streamable
{
    /** Creates a new, not authoritative visitor info. */
    public VisitorInfo ()
    {
        // take current system time in milliseconds, shift left by eight bits,
        // and fill in the newly emptied bits with a random value. return as a hex string.
        // this gives us a resolution of 256 unique tracking numbers per millisecond.
        //
        // note: this corresponds to a similar function in VisitorInfo.java
        
        var now :Number = (new Date()).time * 256;
        var rand :Number = Math.floor(Math.random() * 256);
        var total :Number = now + rand;
        this.tracker = total.toString(16);
        this.isAuthoritative = false;
    }

    /** Creates a new instance with the given tracking id. */
    public VisitorInfo (tracker :String, isAuthoritative :Boolean)
    {
        this.tracker = tracker;
        this.isAuthoritative = isAuthoritative;
    }

    /** Player's tracking number, used to assign them to test groups. */
    public var tracker :String;

    /** Did this visitor info come from the server during a login? */
    public var isAuthoritative :Boolean;

    public function toString() :String
    {
        var auth :String = isAuthoritative ? " (server)" : " (client)";
        return "VisitorInfo [" + tracker + auth + "]";
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        tracker = ins.readField(String) as String;
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeField(tracker);
    }
}
}
