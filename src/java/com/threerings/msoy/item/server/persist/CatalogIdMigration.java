//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.depot.EntityMigration;

import static com.threerings.msoy.Log.log;

/**
 * Does something extraordinary.
 */
public class CatalogIdMigration extends EntityMigration
{
    public CatalogIdMigration (Class<?> catalogClass) {
        super(5);
        _tableName = catalogClass.getName();
        _tableName = _tableName.substring(_tableName.lastIndexOf(".")+1);
    }

    public int invoke (Connection conn, DatabaseLiaison liaison)
        throws SQLException
    {
        log.info("Behold! A great big " + _tableName + " migration!");

        // drop the old primary key on itemId
        String pkName = null;
        ResultSet rs = conn.getMetaData().getPrimaryKeys(null, null, _tableName);
        while (rs.next()) {
            pkName = rs.getString("PK_NAME");
        }
        if (pkName != null) {
            log.info("Dropping old primary key: " + _tableName + "." + pkName);
            liaison.dropPrimaryKey(conn, _tableName, pkName);
        }

        // add the new catalogId column
        log.info("Adding " + _tableName + ".catalogId column...");
        liaison.addColumn(conn, _tableName, "catalogId", "integer not null", true);

        // populate the catalogId column with unique ids
        log.info("Populating " + _tableName + ".catalogId column...");
        int nextId = 1, modified;
        Statement stmt = conn.createStatement();
        try {
            do {
                modified = stmt.executeUpdate(
                    "update " + liaison.tableSQL(_tableName) +
                    " set " + liaison.columnSQL("catalogId") + " = " + (nextId++) +
                    " where " + liaison.columnSQL("catalogId") + " = 0 limit 1");
            } while (modified != 0);
            nextId--;

        } finally {
            JDBCUtil.close(stmt);
        }

        // add the new primary key
        log.info("Adding new primary key to " + _tableName + "...");
        liaison.addPrimaryKey(conn, _tableName, new String[] { "catalogId" });

        // finally set the next id in our sequence
        String seqName = _tableName.substring(0, _tableName.indexOf("CatalogRecord"));
        seqName = seqName.toUpperCase() + "_CATALOG";
        log.info("Configuring id sequence: " + seqName + "=" + nextId);
        stmt = conn.createStatement();
        try {
            stmt.executeUpdate(
                "insert into " + liaison.tableSQL("IdSequences") +
                " values ('" + seqName + "', " + nextId + ")");
        } finally {
            JDBCUtil.close(stmt);
        }

        return 1; // oh yes, we modified things
    }

    protected String _tableName;
}
