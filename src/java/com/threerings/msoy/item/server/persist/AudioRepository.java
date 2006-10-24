//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.ConnectionProvider;

/**
 * Manages the persistent store of {@link AudioRecord} items.
 */
public class AudioRepository extends ItemRepository<AudioRecord>
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
    protected Class<? extends CatalogRecord<AudioRecord>> getCatalogClass ()
    {
        return AudioCatalogRecord.class;
    }

    @Override
    protected Class<? extends CloneRecord<AudioRecord>> getCloneClass ()
    {
        return AudioCloneRecord.class;
    }

    @Override
    protected Class<? extends TagRecord<AudioRecord>> getTagClass ()
    {
        return AudioTagRecord.class;
    }

    @Override
    protected Class<? extends TagHistoryRecord<AudioRecord>> getTagHistoryClass ()
    {
        return AudioTagHistoryRecord.class;
    }

    @Override
    protected Class<? extends RatingRecord<AudioRecord>> getRatingClass ()
    {
        return AudioRatingRecord.class;
    }
}
