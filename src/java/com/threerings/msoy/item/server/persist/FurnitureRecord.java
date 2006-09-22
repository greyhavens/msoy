//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import com.threerings.msoy.item.util.ItemEnum;
import com.threerings.msoy.item.web.Furniture;
import com.threerings.msoy.item.web.Item;

/**
 * Represents a piece of furniture (any prop really) that a user can place into
 * a virtual world scene and potentially interact with.
 */
@Entity
@Table
@TableGenerator(
    name="itemId",
    allocationSize=1,
    pkColumnValue="FURNITURE")
public class FurnitureRecord extends ItemRecord
{
    public static final int SCHEMA_VERSION = BASE_SCHEMA_VERSION*0x100 + 1;

    public static final String ACTION = "action";
    public static final String DESCRIPTION = "description";
    
    /** An action associated with this furniture which is dispatched to the
     * virtual world client when the furniture is clicked on (max length 255
     * characters). */
    @Column(nullable=false)
    public String action = "";

    /** A description of this piece of furniture (max length 255 characters). */
    @Column(nullable=false)
    public String description;

    
    public FurnitureRecord ()
    {
        super();
    }

    protected FurnitureRecord (Furniture furniture)
    {
        super(furniture);

        this.action = furniture.action;
        this.description = furniture.description;
    }

    @Override // from Item
    public ItemEnum getType ()
    {
        return ItemEnum.FURNITURE;
    }
    
    @Override
    public Object clone ()
    {
        FurnitureRecord clone = (FurnitureRecord) super.clone();
        return clone;
    }

    @Override
    protected Item createItem ()
    {
        Furniture object = new Furniture();
        object.action = this.action;
        object.description = this.description;
        return object;
    }
}
