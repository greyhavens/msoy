//
// $Id$

package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.util.Name;

/**
 * Represents the name of a custom chat channel.
 *
 * <p> NOTE: this class (and all {@link Name} derivatives} must use custom field serializers (in
 * this case {@link ChannelName_CustomFieldSerializer}) because IsSerializable only serializes the
 * fields in the class that declares that interface and all subclasses, it does not serialize
 * fields from the superclass. In this case, we have fields from our superclass that need to be
 * serialized, but we can't make {@link Name} implement IsSerializable without introducing an
 * otherwise unwanted dependency on GWT in Narya.
 *
 * <p> If you extend this class (or if you extend {@link Name}) you will have to implement a custom
 * field serializer for your derived class.
 */
public class ChannelName extends Name
    implements IsSerializable
{
    /** The maximum length of a channel name */
    public static final int LENGTH_MAX = 24;

    /** The minimum length of a channel name */
    public static final int LENGTH_MIN = 3;

    /** Used when unserializing */
    public ChannelName ()
    {
    }

    /**
     * Creates an instance with the specified name and creator.
     */
    public ChannelName (String name, int creatorId)
    {
        super(name);
        _creatorId = creatorId;
    }

    /**
     * Returns the member id of this channel's creator.
     */
    public int getCreatorId ()
    {
        return _creatorId;
    }

    @Override // from Object
    public int hashCode ()
    {
        return _name.hashCode() ^ _creatorId;
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        if (other instanceof ChannelName) {
            ChannelName oc = (ChannelName)other;
            return _creatorId == oc._creatorId && _name.equals(oc._name);
        }
        return  false;
    }

    @Override // from Name
    public int compareTo (Name o)
    {
        ChannelName oc = (ChannelName)o;
        if (_creatorId == oc._creatorId) {
            return super.compareTo(oc);
        } else {
            return _creatorId - oc._creatorId;
        }
    }

    /** The member id of this channel's creator. */
    protected int _creatorId;
}
