//
// $Id$

package com.threerings.msoy.server.util;

import java.sql.Connection;
import java.sql.SQLException;

import com.samskivert.depot.SchemaMigration;
import com.samskivert.jdbc.DatabaseLiaison;

/**
 * Migration to drops the primary key for a table.
 * TEMP: this is only needed because depot does not support this the addition of new @Id columns
 * This migration corrects the problem by forcing the regeneration of the pkey index by dropping
 * it first. This may be psql specific.
 * @see http://code.google.com/p/depot/issues/detail?id=8
 */
public class DropPrimaryKey extends SchemaMigration
{
    public DropPrimaryKey (int targetVersion)
    {
        super(targetVersion);
    }

    @Override // from SchemaMigration
    protected int invoke (Connection conn, DatabaseLiaison liaison)
        throws SQLException
    {
        liaison.dropPrimaryKey(conn, _tableName, _tableName + "_pkey");
        return 1; // hmm, we modify all rows technically
    }
}
