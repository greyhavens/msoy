//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.threerings.io.Streamable;

/**
 * Represents which tags have been added to which items.
 */
@Entity
@Table(uniqueConstraints =
        @UniqueConstraint(columnNames={"tagId", "itemId"}))
public abstract class TagRecord<T extends ItemRecord>
    implements Streamable
{
    public static final int SCHEMA_VERSION = 1;
    
    public static final String TAG_ID = "tagId";
    public static final String ITEM_ID = "itemId";
 
    /** The ID of the tag. */
    @Column(nullable=false)
    public int tagId;

     /** The ID of the tagged item. */
    @Column(nullable=false)
    public int itemId;
}
