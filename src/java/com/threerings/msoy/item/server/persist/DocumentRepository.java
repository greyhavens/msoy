//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.annotation.Entity;

import com.threerings.msoy.item.data.all.Document;
import com.threerings.msoy.server.persist.RatingRecord;
import com.threerings.msoy.server.persist.TagRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;

/**
 * Manages the persistent store of {@link Document} items.
 */
@Singleton
public class DocumentRepository extends ItemRepository<DocumentRecord>
{
    @Entity(name="DocumentTagRecord")
    public static class DocumentTagRecord extends TagRecord
    {
    }

    @Entity(name="DocumentTagHistoryRecord")
    public static class DocumentTagHistoryRecord extends TagHistoryRecord
    {
    }

    @Inject public DocumentRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    @Override
    protected Class<DocumentRecord> getItemClass ()
    {
        return DocumentRecord.class;
    }

    @Override
    protected Class<CatalogRecord> getCatalogClass ()
    {
        return coerceCatalog(DocumentCatalogRecord.class);
    }

    @Override
    protected Class<CloneRecord> getCloneClass ()
    {
        return coerceClone(DocumentCloneRecord.class);
    }

    @Override
    protected Class<RatingRecord> getRatingClass ()
    {
        return coerceRating(DocumentRatingRecord.class);
    }

    @Override
    protected TagRecord createTagRecord ()
    {
        return new DocumentTagRecord();
    }

    @Override
    protected TagHistoryRecord createTagHistoryRecord ()
    {
        return new DocumentTagHistoryRecord();
    }
}
