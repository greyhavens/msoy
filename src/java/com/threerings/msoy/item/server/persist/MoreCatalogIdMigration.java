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
import com.samskivert.util.HashIntMap;

import static com.threerings.msoy.Log.log;

/**
 * Does something extraordinary.
 */
public class MoreCatalogIdMigration extends EntityMigration
{
    public MoreCatalogIdMigration (Class<?> itemClass, Class<?> catalogClass) {
        super(7);
        _itemClass = itemClass;
        _catalogClass = catalogClass;
    }

    public boolean runBeforeDefault ()
    {
        return false;
    }

    public int invoke (Connection conn, DatabaseLiaison liaison)
        throws SQLException
    {
        String catTableName = _catalogClass.getName();
        catTableName = catTableName.substring(catTableName.lastIndexOf(".")+1);
        catTableName = liaison.tableSQL(catTableName);

        String itemTableName = _itemClass.getName();
        itemTableName = itemTableName.substring(itemTableName.lastIndexOf(".")+1);
        itemTableName= liaison.tableSQL(itemTableName);

        log.info("Behold! More " + catTableName + " migration!");

        HashIntMap<CatalogData> bycat = new HashIntMap<CatalogData>();
        HashIntMap<CatalogData> bylisted = new HashIntMap<CatalogData>();
        Statement stmt = conn.createStatement();
        try {
            String catColumn = liaison.columnSQL("catalogId");
            String itemColumn = liaison.columnSQL("itemId");
            String listedItemColumn = liaison.columnSQL("listedItemId");
            String originalItemColumn = liaison.columnSQL("originalItemId");
            String ownerColumn = liaison.columnSQL("ownerId");

            ResultSet rs = stmt.executeQuery(
                "select " + catColumn + ", " + listedItemColumn + " from " + catTableName);
            while (rs.next()) {
                CatalogData data = new CatalogData();
                data.catalogId = rs.getInt(1);
                data.listedItemId = rs.getInt(2);
                bycat.put(data.catalogId, data);
                bylisted.put(data.listedItemId, data);
            }

            rs = stmt.executeQuery("select " + itemColumn + ", " + catColumn +
                                   " from " + itemTableName +
                                   " where " + ownerColumn + " != 0 and " + catColumn + " != 0");
            while (rs.next()) {
                int originalId = rs.getInt(1);
                int catalogId = rs.getInt(2); // actually listedItemId
                CatalogData data = bylisted.get(catalogId);
                if (data == null) {
                    log.warning("Missing catalog record for original item " +
                                "[listedId=" + catalogId + ", originalId=" + originalId);
                } else {
                    data.originalItemId = originalId;
                }
            }

            for (CatalogData data : bycat.values()) {
                stmt.executeUpdate("update " + itemTableName +
                                   " set " + catColumn + " = " + data.catalogId +
                                   " where " + itemColumn + " = " + data.listedItemId);
                if (data.originalItemId != 0) {
                    stmt.executeUpdate("update " + itemTableName +
                                       " set " + catColumn + " = " + data.catalogId +
                                       " where " + itemColumn + " = " + data.originalItemId);
                    stmt.executeUpdate("update " + catTableName +
                                       " set " + originalItemColumn + " = " + data.originalItemId +
                                       " where " + catColumn + " = " + data.catalogId);
                } else {
                    log.warning("Missing original item for catalogId=" + data.catalogId + ".");
                }
            }

        } finally {
            JDBCUtil.close(stmt);
        }

        return 1;
    }

    protected static class CatalogData
    {
        public int catalogId;
        public int listedItemId;
        public int originalItemId;
    }

    protected Class<?> _itemClass;
    protected Class<?> _catalogClass;
}
