//
// $Id$

package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.util.Name;

/**
 * Contains a group name and group id in one handy object.
 *
 * <p> NOTE: this class (and all {@link Name} derivatives} must use custom field serializers (in
 * this case {@link RoomName_CustomFieldSerializer}) because IsSerializable only serializes the
 * fields in the class that declares that interface and all subclasses, it does not serialize
 * fields from the superclass. In this case, we have fields from our superclass that need to be
 * serialized, but we can't make {@link Name} implement IsSerializable without introducing an
 * otherwise unwanted dependency on GWT in Narya.
 *
 * <p> If you extend this class (or if you extend {@link Name}) you will have to implement a custom
 * field serializer for your derived class.
 */
public class RoomName extends Name
    implements IsSerializable
{
    /** Used when unserializing */
    public RoomName ()
    {
    }

    /**
     * Creates a room name with the specified information.
     */
    public RoomName (String roomName, int sceneId)
    {
        super(roomName);
        _sceneId = sceneId;
    }

    /**
     * Returns the rooms's numeric identifier.
     */
    public int getSceneId ()
    {
        return _sceneId;
    }

    @Override // from Object
    public int hashCode ()
    {
        return _sceneId;
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return (other instanceof RoomName) && _sceneId == ((RoomName)other)._sceneId;
    }

    @Override // from Name
    public int compareTo (Name o)
    {
        // TODO:
        //return Comparators.compare(_sceneId, ((RoomName) o)._sceneId);
        // but, no, thank you GWT...
        int otherSceneId = ((RoomName) o)._sceneId;
        return (_sceneId < otherSceneId ? -1 : (_sceneId == otherSceneId ? 0 : 1));
    }

    /** The scene's id. */
    protected int _sceneId;
}
