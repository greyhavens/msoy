//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.EntityMigration;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.annotation.Entity;

import com.threerings.msoy.server.persist.TagRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;

/**
 * Manages the persistent store of {@link AudioRecord} items.
 */
public class AudioRepository extends ItemRepository<
    AudioRecord,
    AudioCloneRecord,
    AudioCatalogRecord,
    AudioRatingRecord>
{
    @Entity(name="AudioTagRecord")
    public static class AudioTagRecord extends TagRecord
    {
    }

    @Entity(name="AudioTagHistoryRecord")
    public static class AudioTagHistoryRecord extends TagHistoryRecord
    {
    }

    public AudioRepository (PersistenceContext ctx)
    {
        super(ctx);
        _ctx.registerMigration(getItemClass(), new EntityMigration.Drop(16001, "suiteId"));
    }

    @Override
    protected Class<AudioRecord> getItemClass ()
    {
        return AudioRecord.class;
    }
    
    @Override
    protected Class<AudioCatalogRecord> getCatalogClass ()
    {
        return AudioCatalogRecord.class;
    }

    @Override
    protected Class<AudioCloneRecord> getCloneClass ()
    {
        return AudioCloneRecord.class;
    }

    @Override
    protected Class<AudioRatingRecord> getRatingClass ()
    {
        return AudioRatingRecord.class;
    }

    @Override
    protected TagRecord createTagRecord ()
    {
        return new AudioTagRecord();
    }

    @Override
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new AudioTagHistoryRecord();
    }
}
