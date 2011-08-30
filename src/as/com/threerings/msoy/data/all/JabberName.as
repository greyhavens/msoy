//
// $Id$

package com.threerings.msoy.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.util.Name;

/**
 * Represents the name of a jabber contact.
 */
public class JabberName extends Name
{
    public function JabberName (name :String = null, displayName :String = null)
    {
        super(name);
        _displayName = displayName;
    }

    // from Name
    override public function toString () :String
    {
        return _displayName != null ? _displayName : _name.substring(0, _name.indexOf("@"));
    }

    /**
     * Returns the full JID.
     */
    public function toJID () :String
    {
        return _name;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _displayName = (ins.readField(String) as String);
    }

    // from interface Streamable
    override public function writeObject (out :ObjectOutputStream) :void
    {
        super.writeObject(out);
        out.writeField(_displayName);
    }

    /** The optional display name. */
    protected var _displayName :String;
}
}
