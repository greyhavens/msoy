//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.threerings.io.Streamable;

/**
 * Represents a member's rating of an item.
 */
@Entity
@Table
public abstract class RatingRecord<T extends ItemRecord>
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
    @Column(nullable=false)
    public byte rating;
    
}
