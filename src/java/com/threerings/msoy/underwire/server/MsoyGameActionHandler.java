//
// $Id$

package com.threerings.msoy.underwire.server;

import java.sql.Timestamp;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;

import com.threerings.underwire.server.GameActionHandler;

/**
 * Provides whirled game-specific action handling.
 */
public class MsoyGameActionHandler extends GameActionHandler
{
    @Override // from GameActionHandler
    public void init (ConnectionProvider conprov)
    {
        // nothing to init
    }

    @Override // from GameActionHandler
    public void tempBan (String accountName, Timestamp expires, String warning)
        throws PersistenceException
    {
    }

    @Override // from GameActionHandler
    public void warn (String accountName, String warning)
        throws PersistenceException
    {
    }
}
