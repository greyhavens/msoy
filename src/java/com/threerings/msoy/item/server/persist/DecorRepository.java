//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.SchemaMigration;
import com.samskivert.jdbc.depot.annotation.Entity;

import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.server.persist.TagRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;

/**
 * Manages the persistent store of {@link Decor} items.
 */
@Singleton
public class DecorRepository extends ItemRepository<DecorRecord>
{
    @Entity(name="DecorTagRecord")
    public static class DecorTagRecord extends TagRecord
    {
    }

    @Entity(name="DecorTagHistoryRecord")
    public static class DecorTagHistoryRecord extends TagHistoryRecord
    {
    }

    @Inject public DecorRepository (PersistenceContext ctx)
    {
        super(ctx);

        ctx.registerMigration(DecorRecord.class,
            new SchemaMigration.Rename(17004, "scale", "actorScale"));
        ctx.registerMigration(DecorRecord.class, new SchemaMigration.Drop(17004, "offsetX"));
        ctx.registerMigration(DecorRecord.class, new SchemaMigration.Drop(17004, "offsetY"));
        // I should probably copy avatarScale -> furniScale, but I'll just let it get set to 1
    }

    @Override
    protected Class<DecorRecord> getItemClass ()
    {
        return DecorRecord.class;
    }

    @Override
    protected Class<CatalogRecord> getCatalogClass ()
    {
        return coerceCatalog(DecorCatalogRecord.class);
    }

    @Override
    protected Class<CloneRecord> getCloneClass ()
    {
        return coerceClone(DecorCloneRecord.class);
    }

    @Override
    protected Class<RatingRecord> getRatingClass ()
    {
        return coerceRating(DecorRatingRecord.class);
    }

    @Override
    protected TagRecord createTagRecord ()
    {
        return new DecorTagRecord();
    }

    @Override
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new DecorTagHistoryRecord();
    }
}
