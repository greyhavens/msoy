//
// $Id$

package client.util;

import java.util.HashMap;

/**
 * A HashMap with int keys. I've become spoiled by autoboxing and our own HashIntMap.
 */
public class HashIntMap extends HashMap
{
    public boolean containsKey (int key)
    {
        return containsKey(new Integer(key));
    }

    public Object get (int key)
    {
        return get(new Integer(key));
    }

    public Object put (int key, Object value)
    {
        return put(new Integer(key), value);
    }

    public Object remove (int key)
    {
        return remove(new Integer(key));
    }
}
