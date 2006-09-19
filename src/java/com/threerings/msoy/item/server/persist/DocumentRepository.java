//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Connection;
import java.sql.SQLException;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.jora.Table;

import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.Document;

/**
 * Manages the persistent store of {@link Document} items.
 */
public class DocumentRepository extends ItemRepository<Document>
{
    @Override // from ItemRepository
    protected Table<Document> getTable ()
    {
        return _table;
    }
    
    @Override // from ItemRepository
    protected String getCatalogTableName ()
    {
        return "DOCUMENT_CATALOG";
    }

    @Override // from ItemRepository
    protected String getCloneTableName ()
    {
        return "DOCUMENT_CLONES";
    }

    @Override // from JORARepository
    protected void migrateSchema (Connection conn, DatabaseLiaison liaison)
        throws SQLException, PersistenceException
    {
        JDBCUtil.createTableIfMissing(conn, "DOCUMENTS", new String[] {
            "ITEM_ID integer not null auto_increment primary key",
            "FLAGS tinyint not null",
            "CREATOR_ID integer not null",
            "OWNER_ID integer not null",
            "THUMB_MEDIA_HASH tinyblob",
            "THUMB_MIME_TYPE tinyint",
            "FURNI_MEDIA_HASH tinyblob",
            "FURNI_MIME_TYPE tinyint",
            "DOC_MEDIA_HASH tinyblob not null",
            "DOC_MIME_TYPE tinyint not null",
            "TITLE varchar(255) not null",
        }, "");
        
        JDBCUtil.createTableIfMissing(conn, "DOCUMENT_CLONES", new String[] {
            "ORIGINAL_ITEM_ID integer not null",
            "OWNER_ID integer not null",
        }, "");

        JDBCUtil.createTableIfMissing(conn, "DOCUMENT_CATALOG", new String[] {
            "ITEM_ID integer not null",
            "LISTED_DATE datetime not null",
        }, "");
    }

    @Override // from JORARepository
    protected void createTables ()
    {
	_table = new Table<Document>(
            Document.class, "DOCUMENTS", "ITEM_ID", true);
    }

    protected Table<Document> _table;
}
