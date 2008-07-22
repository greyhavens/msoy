//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.annotation.Entity;

import com.threerings.msoy.server.persist.TagRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;

/**
 * Manages the persistent store of {@link AudioRecord} items.
 */
@Singleton
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

    @Inject public AudioRepository (PersistenceContext ctx)
    {
        super(ctx);
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
