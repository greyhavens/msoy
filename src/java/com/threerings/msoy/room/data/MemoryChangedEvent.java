//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.NamedEvent;

import com.threerings.msoy.item.data.all.ItemIdent;

import static com.threerings.msoy.Log.log;

public class MemoryChangedEvent extends NamedEvent
{
    /** Suitable for serialization. */
    public MemoryChangedEvent ()
    {
    }

    public MemoryChangedEvent (
        int targetOid, String name, ItemIdent ident, String key, byte[] newValue)
    {
        super(targetOid, name);

        _ident = ident;
        _key = key;
        _value = newValue;
    }

    @Override
    public boolean alreadyApplied ()
    {
        return _applied;
    }

    @Override
    public boolean applyToObject (DObject target)
    {
        if (!alreadyApplied()) {
            DSet<EntityMemories> set = target.getSet(_name);
            EntityMemories mems = set.get(_ident);
            if (mems != null) {
                mems.setMemory(_key, _value);

            } else if (_value != null) {
                // mems == null && _value == null is kosher because we allow a memory clear
                // to be dispatched to clients even if it modifies nothing. But if _value != null..
                log.warning("Request to add a memory to non-existent entry!",
                    "ident", _ident, "key", _key, new Exception());
            }
            _applied = true;
        }
        return true;
    }

    @Override
    protected void notifyListener (Object listener)
    {
        if (listener instanceof MemoryChangedListener) {
            ((MemoryChangedListener) listener).memoryChanged(_ident, _key, _value);
        }
    }

    @Override
    protected void toString (StringBuilder buf)
    {
        buf.append("MEMCHANGE:");
        super.toString(buf);
        buf.append(", ident=").append(_ident);
        buf.append(", key=").append(_key);
        buf.append(", value=").append(_value);
    }

    /** The item to which this change applies. */
    protected ItemIdent _ident;

    /** The key that we're changing. */
    protected String _key;

    /** The new value, or null to indicate removal. */
    protected byte[] _value;

    /** Has this change already been applied to the local copy of the dobj? */
    protected transient boolean _applied;
}
