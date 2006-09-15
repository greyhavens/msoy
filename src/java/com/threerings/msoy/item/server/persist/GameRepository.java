//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Connection;
import java.sql.SQLException;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.jora.Table;

import com.threerings.msoy.item.web.Game;

/**
 * Manages the persistent store of {@link Game} items.
 */
public class GameRepository extends ItemRepository<Game>
{
    @Override
    protected Table<Game> getTable ()
    {
        return _table;
    }

    @Override
    protected void migrateSchema (Connection conn, DatabaseLiaison liaison)
        throws SQLException, PersistenceException
    {
        JDBCUtil.createTableIfMissing(conn, "GAME", new String[] {
            "ITEM_ID integer not null auto_increment primary key",
            "FLAGS tinyint not null",
            "CREATOR_ID integer not null",
            "OWNER_ID integer not null",
            "THUMBNAIL_MEDIA_HASH tinyblob",
            "THUMBNAIL_MIME_TYPE tinyint",
            "FURNI_MEDIA_HASH tinyblob",
            "FURNI_MIME_TYPE tinyint",
            "GAME_MEDIA_HASH tinyblob not null",
            "GAME_MIME_TYPE tinyint not null",
            "NAME varchar(255) not null",
            "MIN_PLAYERS smallint not null",
            "MAX_PLAYERS smallint not null",
            "DESIRED_PLAYERS smallint not null",
        }, "");
    }

    @Override
    protected void createTables ()
    {
        _table = new Table<Game>(
            Game.class, "GAME", "ITEM_ID", true);
    }

    protected Table<Game> _table;
}
