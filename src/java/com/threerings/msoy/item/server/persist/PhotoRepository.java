//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Connection;
import java.sql.SQLException;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.jora.Table;

import com.threerings.msoy.item.web.Photo;

/**
 * Manages the persistent store of {@link Photo} items.
 */
public class PhotoRepository extends ItemRepository<Photo>
{
    @Override // from ItemRepository
    protected Table<Photo> getTable ()
    {
        return _table;
    }
    
    @Override // from ItemRepository
    protected String getCloneTableName ()
    {
        return "PHOTO_CLONES";
    }

    @Override // from ItemRepository
    protected String getCatalogTableName ()
    {
        return "PHOTO_CATALOG";
    }

    @Override // from JORARepository
    protected void migrateSchema (Connection conn, DatabaseLiaison liaison)
        throws SQLException, PersistenceException
    {
        JDBCUtil.createTableIfMissing(conn, "PHOTOS", new String[] {
            "ITEM_ID integer not null auto_increment primary key",
            "FLAGS tinyint not null",
            "CREATOR_ID integer not null",
            "OWNER_ID integer not null",
            "THUMB_MEDIA_HASH tinyblob",
            "THUMB_MIME_TYPE tinyint",
            "FURNI_MEDIA_HASH tinyblob",
            "FURNI_MIME_TYPE tinyint",
            "PHOTO_MEDIA_HASH tinyblob not null",
            "PHOTO_MIME_TYPE tinyint not null",
            "CAPTION varchar(255) not null",
        }, "");
        
        JDBCUtil.createTableIfMissing(conn, "PHOTO_CLONES", new String[] {
            "ORIGINAL_ITEM_ID integer not null",
            "OWNER_ID integer not null",
        }, "");
        
        JDBCUtil.createTableIfMissing(conn, "PHOTO_CATALOG", new String[] {
            "ITEM_ID integer not null",
            "LISTED_DATE date not null",
        }, "");
    }

    @Override // from JORARepository
    protected void createTables ()
    {
	_table = new Table<Photo>(Photo.class, "PHOTOS", "ITEM_ID", true);
    }

    protected Table<Photo> _table;
}
