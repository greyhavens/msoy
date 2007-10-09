//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.EntityMigration;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.annotation.Entity;

import com.threerings.msoy.server.persist.TagRecord;
import com.threerings.msoy.server.persist.TagHistoryRecord;

/**
 * Manages the persistent store of {@link Document} items.
 */
public class DocumentRepository extends ItemRepository<
    DocumentRecord,
    DocumentCloneRecord,
    DocumentCatalogRecord,
    DocumentRatingRecord>
{
    @Entity(name="DocumentTagRecord")
    public static class DocumentTagRecord extends TagRecord
    {
    }

    @Entity(name="DocumentTagHistoryRecord")
    public static class DocumentTagHistoryRecord extends TagHistoryRecord
    {
    }

    public DocumentRepository (PersistenceContext ctx)
    {
        super(ctx);
        _ctx.registerMigration(getItemClass(), new EntityMigration.Drop(16001, "suiteId"));
    }

    @Override
    protected Class<DocumentRecord> getItemClass ()
    {
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
    protected Class<DocumentRatingRecord> getRatingClass ()
    {
        return DocumentRatingRecord.class;
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
