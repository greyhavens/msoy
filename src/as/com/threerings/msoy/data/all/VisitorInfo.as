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
    /** Id for vector in the url or flash params */
    public static const VECTOR_ID :String = "vec";

    /** Player's tracking number, used to assign them to test groups. */
    public var id :String;

    /** Did this visitor info come from the server during a login? */
    public var isAuthoritative :Boolean;

    /**
     * Generates a new visitor id using the current time and builtin random number generator. This
     * is needed because an embedded client can sometimes create a new account (without GWT
     * intervention).
     */
    public static function createLocalId () :String
    {
        // take current system time in milliseconds, shift left by eight bits,
        // and fill in the newly emptied bits with a random value. return as a hex string.
        // this gives us a resolution of 256 unique tracking numbers per millisecond.
        //
        // note: this corresponds to a similar function in VisitorInfo.java
        var now :Number = (new Date()).time * 256;
        var rand :Number = Math.floor(Math.random() * 256);
        var total :Number = now + rand;
        var info :VisitorInfo = new VisitorInfo();
        return total.toString(16);
    }

    public function toString () :String
    {
        var auth :String = isAuthoritative ? "server" : "client";
        return "VisitorInfo [" + id + " (" + auth + ")]";
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        id = ins.readField(String) as String;
        isAuthoritative = ins.readBoolean();
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeField(id);
        out.writeBoolean(isAuthoritative);
    }
}
}
