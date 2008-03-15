//
// $Id$

package com.threerings.msoy.underwire.server;

import java.util.HashMap;
import java.util.HashSet;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;

import com.threerings.msoy.server.MsoyBaseServer;

import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.underwire.server.GameInfoProvider;

import com.threerings.underwire.web.data.Account;

/**
 * Provides game-specific info for Whirled.
 */
public class MsoyGameInfoProvider extends GameInfoProvider
{
    @Override // from GameInfoProvider
    public void init (ConnectionProvider conprov)
        throws PersistenceException
    {
        // nothing to init
    }

    @Override // from GameInfoProvider
    public HashMap<String,String> resolveGameNames (HashSet<String> names)
        throws PersistenceException
    {
        return MsoyBaseServer.memberRepo.loadMemberNameAssociations(names);
    }

    @Override // from GameInfoProvider
    public String lookupAccountName (String gameName)
        throws PersistenceException
    {
        // this will require some refacting since display names aren't unique
        // and perma names aren't required
        return null;
    }

    @Override // from GameInfoProvider
    public void populateAccount (Account account)
        throws PersistenceException
    {
        MemberRecord member = MsoyBaseServer.memberRepo.loadMember(account.name.accountName);
        if (member != null) {
            account.firstSession = member.created;
            account.lastSession = member.lastSession;
        }
    }
}
