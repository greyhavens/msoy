//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DataMigration;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.annotation.Entity;

import com.threerings.msoy.server.persist.RatingRecord;
import com.threerings.msoy.server.persist.RatingRepository;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRecord;

/**
 * Manages the persistent store of {@link LauncherRecord} items.
 */
@Singleton
public class LauncherRepository extends ItemRepository<LauncherRecord>
{
    @Entity(name="LauncherTagRecord")
    public static class LauncherTagRecord extends TagRecord
    {
    }

    @Entity(name="LauncherTagHistoryRecord")
    public static class LauncherTagHistoryRecord extends TagHistoryRecord
    {
    }

    @Inject public LauncherRepository (PersistenceContext ctx)
    {
        super(ctx);

        registerMigration(new DataMigration("2009_05_18_launchers") {
            @Override public void invoke () {
                migrateLaunchers();
            }
        });
    }

    @Override
    protected Class<LauncherRecord> getItemClass ()
    {
        return LauncherRecord.class;
    }

    @Override
    protected Class<CatalogRecord> getCatalogClass ()
    {
        return coerceCatalog(LauncherCatalogRecord.class);
    }

    @Override
    protected Class<CloneRecord> getCloneClass ()
    {
        return coerceClone(LauncherCloneRecord.class);
    }

    @Override
    protected Class<RatingRecord> getRatingClass ()
    {
        return RatingRepository.coerceRating(LauncherRatingRecord.class);
    }

    @Override
    protected TagRecord createTagRecord ()
    {
        return new LauncherTagRecord();
    }

    @Override
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new LauncherTagHistoryRecord();
    }
}
