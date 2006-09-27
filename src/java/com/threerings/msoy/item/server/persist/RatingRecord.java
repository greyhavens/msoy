//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.threerings.io.Streamable;

/**
 * Represents a member's rating of an item.
 */
@Entity
@Table(uniqueConstraints =
        @UniqueConstraint(columnNames={
            RatingRecord.ITEM_ID, RatingRecord.MEMBER_ID}))
public abstract class RatingRecord<T extends ItemRecord>
    implements Streamable
{
    public static final int SCHEMA_VERSION = 1;
    
    public static final String ITEM_ID = "itemId";
    public static final String MEMBER_ID = "memberId";
    public static final String RATING = "rating";

    /** The ID of the tagged item. */
    @Column(nullable=false)
    public int itemId;
    
    /** The ID of the rating member. */
    @Column(nullable=false)
    public int memberId;
    
    /** The rating, from 1 to 5 */
    @Column(nullable=false)
    public byte rating;
    
}
