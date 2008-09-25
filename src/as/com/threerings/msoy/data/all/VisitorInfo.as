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
     * Return a list of variables suitable for the FlashVars embed param.  
     * @param affiliate most likely the memberId of the player requesting embed code
     * @param placeId id of the room to throw people into (sceneId/gameLobby)
     * @param inGame true if the flash vars are for a game and placeId is a game lobby whirled
     */
    public static function makeFlashVars(affiliate :String, placeId :int, inGame :Boolean) :String
    {
        var vars :String = AFFILIATE_ID + "=" + affiliate;
        if (inGame) {
            vars += "&gameLobby=" + placeId + "&" + VECTOR_ID + "=" + GAME_VECTOR + "-" + placeId;
        } else if (placeId != 0) {
            vars += "&sceneId=" + placeId + "&" + VECTOR_ID + "=" + ROOM_VECTOR + "-" + placeId;
        }
        return vars;
    }
    
    /** This vector string represents an embedded room. */
    protected static const ROOM_VECTOR :String = "room";

    /** This vector string represents an embedded game. */
    protected static const GAME_VECTOR :String = "game";
}
}
