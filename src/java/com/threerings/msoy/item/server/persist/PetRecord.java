//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.TableGenerator;

import com.threerings.msoy.item.data.all.Pet;
import com.threerings.msoy.item.data.all.Item;

/**
 * Represents a pet that a user can place into a virtual world scene and potentially interact with.
 */
@TableGenerator(name="itemId", pkColumnValue="PET")
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

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #PetRecord}
     * with the supplied key values.
     */
    public static Key<PetRecord> getKey (int itemId)
    {
        return new Key<PetRecord>(
                PetRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
