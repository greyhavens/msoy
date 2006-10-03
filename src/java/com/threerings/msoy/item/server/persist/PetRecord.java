//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import com.threerings.msoy.item.web.Pet;
import com.threerings.msoy.item.web.Item;

/**
 * Represents a pet that a user can place into
 * a virtual world scene and potentially interact with.
 */
@Entity
@Table
@TableGenerator(name="itemId", allocationSize=1, pkColumnValue="PET")
public class PetRecord extends ItemRecord
{
    public static final int SCHEMA_VERSION = BASE_SCHEMA_VERSION*0x100 + 1;

    public static final String DESCRIPTION = "description";

    /** A description of this pet (max length 255 characters). */
    @Column(nullable=false)
    public String description;

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
        object.description = description;
        return object;
    }
}
