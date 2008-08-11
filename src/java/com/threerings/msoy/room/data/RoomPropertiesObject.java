package com.threerings.msoy.room.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.threerings.presents.dobj.DObject;
import com.whirled.game.data.PropertySpaceObject;

/**
 * Provides a property space for a room.
 */
public class RoomPropertiesObject extends DObject
    implements PropertySpaceObject
{
    // from PropertySpaceObject
    public Map<String, Object> getUserProps ()
    {
        return _props;
    }

    // from PropertySpaceObject
    public Set<String> getDirtyProps ()
    {
        return _dirty;
    }

    /**
     * The current state of game data for a room.
     * On the server, this will be a byte[] for normal properties and a byte[][] for array
     * properties. On the client, the actual values are kept whole.
     */
    protected transient HashMap<String, Object> _props = new HashMap<String, Object>();

    /**
     * The persistent properties that have been written to since startup.
     */
    protected transient Set<String> _dirty = new HashSet<String>();
}
