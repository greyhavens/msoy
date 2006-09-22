//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.*; // for EJB3 annotations

@Entity
@Table
public abstract class CloneRecord<T extends ItemRecord>
{
    public static final int SCHEMA_VERSION = 1;

    public static final String ITEM_ID = "itemId";
    public static final String ORIGINAL_ITEM_ID = "originalItemId";
    public static final String OWNER_ID = "ownerId";

    /** This clone's ID, unique relative all items of the same type. */
    @Id
    @GeneratedValue(generator="itemId", strategy=GenerationType.TABLE)
    public int itemId;

    /** The ID of the immutable item from which this was cloned. */
    @Column(nullable=false)
    public int originalItemId;

    /** The owner of this clone. */
    @Column(nullable=false)
    public int ownerId;
}
