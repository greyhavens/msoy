//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.annotation.Entity;

import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRecord;

/**
 * Manages the persistent store of {@link TrophySourceRecord} items.
 */
@Singleton
public class TrophySourceRepository extends ItemRepository<
    TrophySourceRecord,
    TrophySourceCloneRecord,
    TrophySourceCatalogRecord,
    TrophySourceRatingRecord>
{
    @Entity(name="TrophySourceTagRecord")
    public static class TrophySourceTagRecord extends TagRecord
    {
    }

    @Entity(name="TrophySourceTagHistoryRecord")
    public static class TrophySourceTagHistoryRecord extends TagHistoryRecord
    {
    }

    @Inject public TrophySourceRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    @Override
    protected Class<TrophySourceRecord> getItemClass ()
    {
        return TrophySourceRecord.class;
    }

    @Override
    protected Class<TrophySourceCatalogRecord> getCatalogClass ()
    {
        return TrophySourceCatalogRecord.class;
    }

    @Override
    protected Class<TrophySourceCloneRecord> getCloneClass ()
    {
        return TrophySourceCloneRecord.class;
    }

    @Override
    protected Class<TrophySourceRatingRecord> getRatingClass ()
    {
        return TrophySourceRatingRecord.class;
    }

    @Override
    protected TagRecord createTagRecord ()
    {
        return new TrophySourceTagRecord();
    }

    @Override
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new TrophySourceTagHistoryRecord();
    }
}
