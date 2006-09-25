//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.ConnectionProvider;

/**
 * Manages the persistent store of {@link PhotoRecord} items.
 */
public class PhotoRepository extends ItemRepository<PhotoRecord>
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
    protected Class<? extends CatalogRecord<PhotoRecord>> getCatalogClass ()
    {
        return PhotoCatalogRecord.class;
    }

    @Override
    protected Class<? extends CloneRecord<PhotoRecord>> getCloneClass ()
    {
        return PhotoCloneRecord.class;
    }
}
