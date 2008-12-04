//
// $Id$

package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.util.Name;

/**
 * Contains a group name and group id in one handy object.
 *
 * <p> NOTE: this class (and all {@link Name} derivatives} must use custom field serializers (in
 * this case {@link GroupName_CustomFieldSerializer}) because IsSerializable only serializes the
 * fields in the class that declares that interface and all subclasses, it does not serialize
 * fields from the superclass. In this case, we have fields from our superclass that need to be
 * serialized, but we can't make {@link Name} implement IsSerializable without introducing an
 * otherwise unwanted dependency on GWT in Narya.
 *
 * <p> If you extend this class (or if you extend {@link Name}) you will have to implement a custom
 * field serializer for your derived class.
 */
public class GroupName extends Name
    implements IsSerializable
{
    /** The maximum length of a group name */
    public static final int LENGTH_MAX = 24;

    /** The minimum length of a group name */
    public static final int LENGTH_MIN = 3;

    /**
     * Creates a group name that can be used as a key for a DSet lookup or whereever else one might
     * need to use a {@link GroupName} instance as a key but do not have the (unneeded) group name.
     */
    public static GroupName makeKey (int groupId)
    {
        return new GroupName(null, groupId);
    }

    /** Used when unserializing */
    public GroupName ()
    {
    }

    /**
     * Creates a group name with the specified information.
     */
    public GroupName (String groupName, int groupId)
    {
        super(groupName);
        _groupId = groupId;
    }

    /**
     * Returns the group's numeric identifier.
     */
    public int getGroupId ()
    {
        return _groupId;
    }

    @Override // from Object
    public int hashCode ()
    {
        return _groupId;
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return (other instanceof GroupName) && _groupId == ((GroupName)other)._groupId;
    }

    @Override // from Name
    public int compareTo (Name o)
    {
        return _groupId - ((GroupName) o)._groupId;
    }

    /** The group's id. */
    protected int _groupId;
}
