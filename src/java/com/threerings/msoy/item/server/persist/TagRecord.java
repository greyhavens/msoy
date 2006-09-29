//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Entity;
import javax.persistence.Id;

import com.threerings.io.Streamable;

/**
 * Represents which tags have been added to which items.
 */
@Entity
public abstract class TagRecord<T extends ItemRecord>
    implements Streamable
{
    public static final int SCHEMA_VERSION = 1;
    
    public static final String TAG_ID = "tagId";
    public static final String ITEM_ID = "itemId";
 
    /** The ID of the tag. */
    @Id
    public int tagId;

     /** The ID of the tagged item. */
    @Id
    public int itemId;
}
