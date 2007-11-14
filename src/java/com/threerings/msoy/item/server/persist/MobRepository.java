//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.annotation.Entity;

import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagRecord;

import static com.threerings.msoy.Log.log;

/**
 * Manages the persistent store of {@link MobRecord} items.
 */
public class MobRepository extends ItemRepository<
    MobRecord,
    MobCloneRecord,
    MobCatalogRecord,
    MobRatingRecord>
{
    @Entity(name="MobTagRecord")
    public static class MobTagRecord extends TagRecord
    {
    }

    @Entity(name="MobTagHistoryRecord")
    public static class MobTagHistoryRecord extends TagHistoryRecord
    {
    }

    public MobRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    @Override
    protected Class<MobRecord> getItemClass ()
    {
        return MobRecord.class;
    }

    @Override
    protected Class<MobCatalogRecord> getCatalogClass ()
    {
        return MobCatalogRecord.class;
    }

    @Override
    protected Class<MobCloneRecord> getCloneClass ()
    {
        return MobCloneRecord.class;
    }

    @Override
    protected Class<MobRatingRecord> getRatingClass ()
    {
        return MobRatingRecord.class;
    }

    @Override
    protected TagRecord createTagRecord ()
    {
        return new MobTagRecord();
    }

    @Override
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new MobTagHistoryRecord();
    }
}
