//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.ConnectionProvider;

/**
 * Manages the persistent store of {@link AudioRecord} items.
 */
public class AudioRepository extends ItemRepository<
    AudioRecord,
    AudioCloneRecord,
    AudioCatalogRecord,
    AudioTagRecord,
    AudioTagHistoryRecord,
    AudioRatingRecord>
{
    public AudioRepository (ConnectionProvider provider)
    {
        super(provider);
    }

    @Override
    protected Class<AudioRecord> getItemClass () {
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
    protected Class<AudioTagRecord> getTagClass ()
    {
        return AudioTagRecord.class;
    }

    @Override
    protected Class<AudioTagHistoryRecord> getTagHistoryClass ()
    {
        return AudioTagHistoryRecord.class;
    }

    @Override
    protected Class<AudioRatingRecord> getRatingClass ()
    {
        return AudioRatingRecord.class;
    }
}
