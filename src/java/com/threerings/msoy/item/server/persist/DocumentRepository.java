//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.ConnectionProvider;

/**
 * Manages the persistent store of {@link Document} items.
 */
public class DocumentRepository extends ItemRepository<
    DocumentRecord,
    DocumentCloneRecord,
    DocumentCatalogRecord,
    DocumentTagRecord,
    DocumentTagHistoryRecord,
    DocumentRatingRecord>
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
    protected Class<DocumentCatalogRecord> getCatalogClass ()
    {
        return DocumentCatalogRecord.class;
    }

    @Override
    protected Class<DocumentCloneRecord> getCloneClass ()
    {
        return DocumentCloneRecord.class;
    }

    @Override
    protected Class<DocumentTagRecord> getTagClass ()
    {
        return DocumentTagRecord.class;
    }

    @Override
    protected Class<DocumentTagHistoryRecord> getTagHistoryClass ()
    {
        return DocumentTagHistoryRecord.class;
    }

    @Override
    protected Class<DocumentRatingRecord> getRatingClass ()
    {
        return DocumentRatingRecord.class;
    }
}
