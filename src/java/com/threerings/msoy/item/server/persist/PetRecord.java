//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.TableGenerator;

import com.threerings.msoy.item.web.Pet;
import com.threerings.msoy.item.web.Item;

/**
 * Represents a pet that a user can place into a virtual world scene and potentially interact with.
 */
@Entity
@Table
@TableGenerator(name="itemId", allocationSize=1, pkColumnValue="PET")
public class PetRecord extends ItemRecord
{
    public static final int SCHEMA_VERSION = 1 +
        BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    public PetRecord ()
    {
        super();
    }

    protected PetRecord (Pet pet)
    {
        super(pet);

        description = pet.description;
    }

    @Override // from Item
    public byte getType ()
    {
        return Item.PET;
    }

    @Override
    protected Item createItem ()
    {
        Pet object = new Pet();
        return object;
    }
}
