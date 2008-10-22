//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.annotation.Entity;

import com.threerings.msoy.item.data.all.Pet;

import com.threerings.msoy.server.persist.RatingRecord;
import com.threerings.msoy.server.persist.RatingRepository;
import com.threerings.msoy.server.persist.TagRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;

/**
 * Manages the persistent store of {@link Pet} items.
 */
@Singleton
public class PetRepository extends ItemRepository<PetRecord>
{
    @Entity(name="PetTagRecord")
    public static class PetTagRecord extends TagRecord
    {
    }

    @Entity(name="PetTagHistoryRecord")
    public static class PetTagHistoryRecord extends TagHistoryRecord
    {
    }

    @Inject public PetRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    @Override
    protected Class<PetRecord> getItemClass ()
    {
        return PetRecord.class;
    }

    @Override
    protected Class<CatalogRecord> getCatalogClass ()
    {
        return coerceCatalog(PetCatalogRecord.class);
    }

    @Override
    protected Class<CloneRecord> getCloneClass ()
    {
        return coerceClone(PetCloneRecord.class);
    }

    @Override
    protected Class<RatingRecord> getRatingClass ()
    {
        return RatingRepository.coerceRating(PetRatingRecord.class);
    }

    @Override
    protected TagRecord createTagRecord ()
    {
        return new PetTagRecord();
    }

    @Override
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new PetTagHistoryRecord();
    }
}
