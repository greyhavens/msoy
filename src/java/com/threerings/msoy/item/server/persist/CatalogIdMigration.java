//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.MySQLLiaison;
import com.samskivert.jdbc.PostgreSQLLiaison;
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

        boolean isMySQL;
        if (liaison instanceof MySQLLiaison) {
            isMySQL = true;
        } else if (liaison instanceof PostgreSQLLiaison) {
            isMySQL = false;
        } else {
            throw new IllegalArgumentException("Aii, we only know about MySQL and PostgreSQL.");
        }

        String tableStr = liaison.tableSQL(_tableName);
        String columnStr = liaison.columnSQL("catalogId");

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
        log.info("Adding and auto-populating " + _tableName + ".catalogId column...");
        liaison.addColumn(conn, _tableName, "catalogId", "serial unique", true);

        Statement stmt = conn.createStatement();
        try {
            String update = "alter table " + tableStr + " ";
            if (isMySQL) {
                update += " modify " + columnStr + " integer not null";
            } else {
                update += " alter " + columnStr + " drop default";
            }
            stmt.executeUpdate(update);

        } finally {
            JDBCUtil.close(stmt);
        }

        // add the new primary key
        log.info("Adding new primary key to " + _tableName + "...");
        liaison.addPrimaryKey(conn, _tableName, new String[] { "catalogId" });

        // finally set the next id in our sequence
        String seqName = _tableName.substring(0, _tableName.indexOf("CatalogRecord"));
        seqName = seqName.toUpperCase() + "_CATALOG";
        log.info("Configuring id sequence: " + seqName);
        stmt = conn.createStatement();
        try {
            // needless to say this simple function has to be called something completely
            // different in our two supported database systems
            String ifNull = isMySQL ? "IFNULL" : "COALESCE";
            stmt.executeUpdate(
                "insert into " + liaison.tableSQL("IdSequences") + " values ('" + seqName +
                "', (select " + ifNull + "(max(" + columnStr +")+1, 1) from " + tableStr + "))");

        } finally {
            JDBCUtil.close(stmt);
        }

        return 1; // oh yes, we modified things
    }

    protected String _tableName;
}
