//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.ConnectionProvider;

/**
 * Manages the persistent store of {@link PhotoRecord} items.
 */
public class PhotoRepository extends ItemRepository<
    PhotoRecord,
    PhotoCloneRecord,
    PhotoCatalogRecord,
    PhotoTagRecord,
    PhotoTagHistoryRecord,
    PhotoRatingRecord>
{
    public PhotoRepository (ConnectionProvider provider)
    {
        super(provider);
    }

    @Override
    protected Class<PhotoRecord> getItemClass () {
        return PhotoRecord.class;
    }
    
    @Override
    protected Class<PhotoCatalogRecord> getCatalogClass ()
    {
        return PhotoCatalogRecord.class;
    }

    @Override
    protected Class<PhotoCloneRecord> getCloneClass ()
    {
        return PhotoCloneRecord.class;
    }

    @Override
    protected Class<PhotoTagRecord> getTagClass ()
    {
        return PhotoTagRecord.class;
    }

    @Override
    protected Class<PhotoTagHistoryRecord> getTagHistoryClass ()
    {
        return PhotoTagHistoryRecord.class;
    }

    @Override
    protected Class<PhotoRatingRecord> getRatingClass ()
    {
        return PhotoRatingRecord.class;
    }
}
