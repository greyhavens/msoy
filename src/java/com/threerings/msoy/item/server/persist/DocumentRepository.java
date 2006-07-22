//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Connection;
import java.sql.SQLException;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.jora.Table;

import com.threerings.msoy.item.data.Document;

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

    @Override // from JORARepository
    protected void migrateSchema (Connection conn, DatabaseLiaison liaison)
        throws SQLException, PersistenceException
    {
        JDBCUtil.createTableIfMissing(conn, "DOCUMENTS", new String[] {
            "ITEM_ID integer not null auto_increment primary key",
            "FLAGS tinyint not null",
            "OWNER_ID integer not null",
            "MEDIA_HASH varchar(64) not null",
            "MIME_TYPE tinyint not null",
            "TITLE varchar(255) not null",
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
