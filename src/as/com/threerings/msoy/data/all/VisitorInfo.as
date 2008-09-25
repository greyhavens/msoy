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
    /** This vector string represents an embedded room. */
    public static const ROOM_VECTOR :String = "room";

    /** This vector string represents an embedded game. */
    public static const GAME_VECTOR :String = "game";

    /** This vector string represents my butt. */
    public static const GENERIC_VECTOR :String = "?";
    
    /** Id for affiliate in the url or flash params */
    public static const AFFILIATE_ID :String = "aff";
    
    /** Id for vector in the url or flash params */
    public static const VECTOR_ID :String = "vec";
    
    /** Id for visitorId in the url or flash params */
    public static const VISITOR_ID :String = "vid";

    /** Creates a new visitor info with optional tracking id provided. */
    public function VisitorInfo (id :String = null, isAuthoritative :Boolean = false)
    {
        if (id != null) {
            this.id = id;

        } else {
            // take current system time in milliseconds, shift left by eight bits,
            // and fill in the newly emptied bits with a random value. return as a hex string.
            // this gives us a resolution of 256 unique tracking numbers per millisecond.
            //
            // note: this corresponds to a similar function in VisitorInfo.java
            
            var now :Number = (new Date()).time * 256;
            var rand :Number = Math.floor(Math.random() * 256);
            var total :Number = now + rand;
            this.id = total.toString(16);
        }
        
        this.isAuthoritative = isAuthoritative;
    }

    /** Player's tracking number, used to assign them to test groups. */
    public var id :String;

    /** Did this visitor info come from the server during a login? */
    public var isAuthoritative :Boolean;

    public function toString() :String
    {
        var auth :String = isAuthoritative ? " (server)" : " (client)";
        return "VisitorInfo [" + id + auth + "]";
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
    
    /** 
     * Return the argument string that will append a tracking id to a GWT page url.
     */
    public function makeTrackingArgs(affiliate :String = null) :String
    {
        var args :String = "_" + VISITOR_ID + "_" + id;
        args += affiliate ? "?" + AFFILIATE_ID + "=" + affiliate : "";
        return args; 
    }
    
    /** 
     * Return a list of tracking variables suitable for the FlashVars embed param.  
     * @param affiliate most likely the memberId of the player requesting embed code
     * @vector one of ROOM_VECTOR, GAME_VECTOR, GENERIC_VECTOR
     */
    public static function makeFlashVars(affiliate :String, vector :String) :String
    {
        return AFFILIATE_ID + "=" + affiliate + "&" + VECTOR_ID + "=" + vector;
    }
}
}
