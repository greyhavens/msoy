//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.ConnectionProvider;

/**
 * Manages the persistent store of {@link Document} items.
 */
public class DocumentRepository extends ItemRepository<DocumentRecord>
{
    public DocumentRepository (ConnectionProvider provider)
    {
        super(provider);
    }

    @Override
    protected Class<DocumentRecord> getItemClass () {
        return DocumentRecord.class;
    }
    
    @Override
    protected Class<? extends CatalogRecord<DocumentRecord>> getCatalogClass ()
    {
        return DocumentCatalogRecord.class;
    }

    @Override
    protected Class<? extends CloneRecord<DocumentRecord>> getCloneClass ()
    {
        return DocumentCloneRecord.class;
    }

    @Override // from ItemRepository
    protected String getTypeEponym ()
    {
        return "DOCUMENT";
    }
}
