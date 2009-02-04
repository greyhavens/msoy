//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.presents.dobj.ChangeListener;

import com.threerings.msoy.item.data.all.ItemIdent;

public interface MemoryChangedListener extends ChangeListener
{
    /**
     * Notify us that a memory has changed for this entity. If the value is null,
     * then the memory was removed.
     */
    void memoryChanged (ItemIdent ident, String key, byte[] value);
}
