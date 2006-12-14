//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.TableGenerator;

import com.threerings.msoy.item.web.Furniture;
import com.threerings.msoy.item.web.Item;

/**
 * Represents a piece of furniture (any prop really) that a user can place into
 * a virtual world scene and potentially interact with.
 */
@Entity
@Table
@TableGenerator(name="itemId", allocationSize=1, pkColumnValue="FURNITURE")
public class FurnitureRecord extends ItemRecord
{
    public static final int SCHEMA_VERSION = BASE_SCHEMA_VERSION*0x100 + 1;

    public static final String ACTION = "action";
    public static final String DESCRIPTION = "description";

    /** An action associated with this furniture which is dispatched to the
     * virtual world client when the furniture is clicked on (max length 255
     * characters). */
    public String action = "";

    public FurnitureRecord ()
    {
        super();
    }

    protected FurnitureRecord (Furniture furniture)
    {
        super(furniture);

        action = furniture.action;
    }

    @Override // from Item
    public byte getType ()
    {
        return Item.FURNITURE;
    }

    @Override
    protected Item createItem ()
    {
        Furniture object = new Furniture();
        object.action = action;
        return object;
    }
}
