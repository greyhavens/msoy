//
// $Id$

package com.threerings.msoy.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

/**
 * Stores referral info for the current player: affiliate who sent them to us,
 * entry vector type, creative id (eg. one for each banner graphic), and a unique
 * tracking number used to assign them to test groups. 
 */
public class ReferralInfo
    implements Streamable
{
    /** Creates a random tracking number. */
    public static function makeRandomTracker () :String
    {
        // take current system time in milliseconds, shift left by eight bits,
        // and fill in the newly emptied bits with a random value. return as a hex string.
        // this gives us a resolution of 256 unique tracking numbers per millisecond.
        var now :Number = (new Date()).time * 256;
        var rand :Number = Math.floor(Math.random() * 256);
        var total :Number = now + rand;
        return total.toString(16);
    }

    /**
     * Instance creator. Returns a new instance, or null if one of the parameters is null.
     */
    public static function makeInstance (
        affiliate :String, vector :String, creative :String, tracker :String) :ReferralInfo
    {
        if (affiliate == null || vector == null || creative == null || tracker == null) {
            return null;
        }
        
        var ref :ReferralInfo = new ReferralInfo();
        ref.affiliate = affiliate;
        ref.vector = vector;
        ref.creative = creative;
        ref.tracker = tracker;
        return ref;
    }

    /** Identifies the affiliate who referred this player to us. */
    public var affiliate :String;

    /** Identifies the entry vector type. */
    public var vector :String;

    /** Identifies the creative piece / banner / etc. used in this referral. */
    public var creative :String;

    /** Player's tracking number, used to assign them to test groups. */
    public var tracker :String;

    /** Constructor. */
    public function ReferralInfo () { }

    public function toString() :String
    {
        return "Referral [ref=" + affiliate + ", " + vector + ", " +
            creative + ", " + tracker  + "]";
    }

    // from superinterface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        affiliate = ins.readField(String) as String;
        vector = ins.readField(String) as String;
        creative = ins.readField(String) as String;
        tracker = ins.readField(String) as String;
    }

    // from superinterface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeField(affiliate);
        out.writeField(vector);
        out.writeField(creative);
        out.writeField(tracker);
    }
}
}
