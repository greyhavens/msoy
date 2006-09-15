//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Connection;
import java.sql.SQLException;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.jora.Table;

import com.threerings.msoy.item.web.Furniture;

/**
 * Manages the persistent store of {@link Furniture} items.
 */
public class FurnitureRepository extends ItemRepository<Furniture>
{
    @Override // from ItemRepository
    protected Table<Furniture> getTable ()
    {
        return _table;
    }

    @Override // from JORARepository
    protected void migrateSchema (Connection conn, DatabaseLiaison liaison)
        throws SQLException, PersistenceException
    {
        JDBCUtil.createTableIfMissing(conn, "FURNITURE", new String[] {
            "ITEM_ID integer not null auto_increment primary key",
            "FLAGS tinyint not null",
            "CREATOR_ID integer not null",
            "OWNER_ID integer not null",
            "THUMBNAIL_MEDIA_HASH tinyblob",
            "THUMBNAIL_MIME_TYPE tinyint",
            "FURNI_MEDIA_HASH tinyblob",
            "FURNI_MIME_TYPE tinyint",
            "ACTION varchar(255) not null",
            "DESCRIPTION varchar(255) not null",
        }, "");

        // Temp
        JDBCUtil.changeColumn(conn, "FURNITURE", "MEDIA_HASH",
            "MEDIA_HASH tinyblob not null");
    }

    @Override // from JORARepository
    protected void createTables ()
    {
	_table = new Table<Furniture>(
            Furniture.class, "FURNITURE", "ITEM_ID", true);
    }

    protected Table<Furniture> _table;
}
