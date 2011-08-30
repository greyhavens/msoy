//
// $Id$

package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.util.Name;

/**
 * Represents the name of a jabber contact.
 */
public class JabberName extends Name
    implements IsSerializable
{
    /**
     * Creates an instance with the specified name.
     */
    public JabberName (String name)
    {
        this(name, null);
    }

    /**
     * Creates an instance with the specified name and display name.
     */
    public JabberName (String name, String display)
    {
        super(name);
        _displayName = display;
    }

    /**
     * Returns the full JID.
     */
    public String toJID ()
    {
        return _name;
    }

    /**
     * Returns the display name.
     */
    public String getDisplayName ()
    {
        return _displayName;
    }

    @Override // from Name
    public String toString ()
    {
        return _displayName != null ? _displayName : _name.substring(0, _name.indexOf("@"));
    }

    // An optional display name
    protected String _displayName;
}
