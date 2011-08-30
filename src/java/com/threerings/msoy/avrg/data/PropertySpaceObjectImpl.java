//
// $Id$

package com.threerings.msoy.avrg.data;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import com.whirled.game.data.PropertySpaceObject;
import com.whirled.game.server.PropertySpaceHelper;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.SimpleStreamableObject;

@com.threerings.util.ActionScript(omit=true)
public class PropertySpaceObjectImpl extends SimpleStreamableObject
    implements PropertySpaceObject
{
    public Map<String, Object> getUserProps ()
    {
        return _props;
    }

    public Set<String> getDirtyProps ()
    {
        return _dirty;
    }

    /**
     * A custom serialization method.
     */
    public void writeObject (ObjectOutputStream out)
        throws IOException
    {
        out.defaultWriteObject();

        PropertySpaceHelper.writeProperties(this, out);
    }

    /**
     * A custom serialization method.
     */
    public void readObject (ObjectInputStream ins)
        throws IOException, ClassNotFoundException
    {
        ins.defaultReadObject();

        PropertySpaceHelper.readProperties(this, ins);
    }

    protected transient Map<String, Object> _props = Maps.newHashMap();
    protected transient Set<String> _dirty = Sets.newHashSet();
}

