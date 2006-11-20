//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;

import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

import com.threerings.io.Streamable;

/**
 * Keeps a history of tagging events for a given item.
 */
@Entity
@Table
public abstract class TagHistoryRecord<T extends ItemRecord>
    implements Streamable
{
    public static final int SCHEMA_VERSION = 1;

    public static final String ITEM_ID = "itemId";
    public static final String TAG_ID = "tagId";
    public static final String MEMBER_ID = "memberId";
    public static final String ACTION = "action";
    public static final String WHEN = "when";

    /** The ID of the item being operated on. */
    @Column(nullable=false)
    public int itemId;

    /** The ID of the tag that was added or deleted. */
    @Column(nullable=false)
    public int tagId;
    
    /** The ID of the member who added or deleted the tag. */
    @Column(nullable=false)
    public int memberId;
    
    /** The action taken (ADDED or REMOVED or COPIED). */
    @Column(nullable=false)
    public byte action;

    /** The time of the tagging event. */
    @Column(nullable=false)
    public Timestamp time;
}
