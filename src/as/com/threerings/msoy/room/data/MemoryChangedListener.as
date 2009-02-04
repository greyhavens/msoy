//
// $Id$

package com.threerings.msoy.room.data {

import flash.utils.ByteArray;

import com.threerings.presents.dobj.ChangeListener;

import com.threerings.msoy.item.data.all.ItemIdent;

public interface MemoryChangedListener extends ChangeListener
{
    /**
     * Notify us that a memory has changed for this entity. If the value is null,
     * then the memory was removed.
     */
    function memoryChanged (ident :ItemIdent, key :String, value :ByteArray) :void;
}
}
