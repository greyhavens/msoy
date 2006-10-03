//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.ConnectionProvider;

/**
 * Manages the persistent store of {@link Pet} items.
 */
public class PetRepository extends ItemRepository<PetRecord>
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
    protected Class<? extends CatalogRecord<PetRecord>> getCatalogClass ()
    {
        return PetCatalogRecord.class;
    }

    @Override
    protected Class<? extends CloneRecord<PetRecord>> getCloneClass ()
    {
        return PetCloneRecord.class;
    }

    @Override
    protected Class<? extends TagRecord<PetRecord>> getTagClass ()
    {
        return PetTagRecord.class;
    }

    @Override
    protected Class<? extends TagHistoryRecord<PetRecord>> getTagHistoryClass ()
    {
        return PetTagHistoryRecord.class;
    }
    
    @Override
    protected Class<? extends RatingRecord<PetRecord>> getRatingClass ()
    {
        return PetRatingRecord.class;
    }
}
