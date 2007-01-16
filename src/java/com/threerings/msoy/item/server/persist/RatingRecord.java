//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.annotation.Table;

import com.threerings.io.Streamable;

/**
 * Represents a member's rating of an item.
 */
@Entity
@Table
public abstract class RatingRecord<T extends ItemRecord> extends PersistentRecord
    implements Streamable
{
    public static final int SCHEMA_VERSION = 1;
    
    public static final String ITEM_ID = "itemId";
    public static final String MEMBER_ID = "memberId";
    public static final String RATING = "rating";

    /** The ID of the tagged item. */
    @Id
    public int itemId;

    /** The ID of the rating member. */
    @Id
    public int memberId;
    
    /** The rating, from 1 to 5 */
    public byte rating;
    
}
