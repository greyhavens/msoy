//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;

import com.threerings.io.Streamable;

/**
 * Keeps a history of tagging events for a given item.
 */
@Entity
@Table
public abstract class TagHistoryRecord extends PersistentRecord
    implements Streamable
{
    public static final int SCHEMA_VERSION = 1;

    public static final String ITEM_ID = "itemId";
    public static final String TAG_ID = "tagId";
    public static final String MEMBER_ID = "memberId";
    public static final String ACTION = "action";
    public static final String WHEN = "when";

    /** The ID of the item being operated on. */
    public int itemId;

    /** The ID of the tag that was added or deleted. */
    public int tagId;
    
    /** The ID of the member who added or deleted the tag. */
    public int memberId;
    
    /** The action taken (ADDED or REMOVED or COPIED). */
    public byte action;

    /** The time of the tagging event. */
    public Timestamp time;
}
