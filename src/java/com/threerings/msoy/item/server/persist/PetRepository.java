//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.ConnectionProvider;

/**
 * Manages the persistent store of {@link Pet} items.
 */
public class PetRepository extends ItemRepository<
    PetRecord,
    PetCloneRecord,
    PetCatalogRecord,
    PetTagRecord,
    PetTagHistoryRecord,
    PetRatingRecord>
{
    public PetRepository (ConnectionProvider provider)
    {
        super(provider);
    }

    @Override
    protected Class<PetRecord> getItemClass () {
        return PetRecord.class;
    }
    
    @Override
    protected Class<PetCatalogRecord> getCatalogClass ()
    {
        return PetCatalogRecord.class;
    }

    @Override
    protected Class<PetCloneRecord> getCloneClass ()
    {
        return PetCloneRecord.class;
    }

    @Override
    protected Class<PetTagRecord> getTagClass ()
    {
        return PetTagRecord.class;
    }

    @Override
    protected Class<PetTagHistoryRecord> getTagHistoryClass ()
    {
        return PetTagHistoryRecord.class;
    }
    
    @Override
    protected Class<PetRatingRecord> getRatingClass ()
    {
        return PetRatingRecord.class;
    }
}
