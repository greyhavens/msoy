//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations

@Entity
@Table
public abstract class CloneRecord<T extends ItemRecord>
{
    public static final int SCHEMA_VERSION = 2;

    public static final String ITEM_ID = "itemId";
    public static final String ORIGINAL_ITEM_ID = "originalItemId";
    public static final String OWNER_ID = "ownerId";
    public static final String USED = "used";
    public static final String LOCATION = "location";

    /** This clone's ID, unique relative all items of the same type. */
    @Id
    @GeneratedValue(generator="cloneId", strategy=GenerationType.TABLE)
    public int itemId;

    /** The ID of the immutable item from which this was cloned. */
    @Column(nullable=false)
    public int originalItemId;

    /** The owner of this clone. */
    @Column(nullable=false)
    public int ownerId;

    /** How this item is being used (see Item.USED_AS_FURNITURE). */
    @Column(nullable=false)
    public byte used;

    /** Where it's being used. */
    @Column(nullable=false)
    public int location;
}
