//
// $Id$

package com.threerings.msoy.underwire.server;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;

import com.threerings.msoy.server.MsoyBaseServer;

import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberWarningRecord;

import com.threerings.msoy.data.all.MemberName;

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
        HashSet<Integer> memberIds = new HashSet<Integer>();
        for (String name : names) {
            try {
                memberIds.add(Integer.valueOf(name));
            } catch (NumberFormatException nfe) {
                // this should never happen
            }
        }
        HashMap<String,String> map = new HashMap<String,String>();
        for (MemberName name : MsoyBaseServer.memberRepo.loadMemberNames(memberIds)) {
            map.put(Integer.toString(name.getMemberId()), name.toString());
        }
        return map;
    }

    @Override // from GameInfoProvider
    public String[] lookupAccountNames (String gameName)
        throws PersistenceException
    {
        List<Integer> memberIds =
            MsoyBaseServer.memberRepo.findMembersByDisplayName(gameName, LOOKUP_LIMIT);
        ArrayList<String> names = new ArrayList<String>(memberIds.size());
        for (Integer memberId : memberIds) {
            names.add(memberId.toString());
        }
        return names.toArray(new String[names.size()]);
    }

    @Override // from GameInfoProvider
    public void populateAccount (Account account)
        throws PersistenceException
    {
        MemberRecord member = MsoyBaseServer.memberRepo.loadMember(account.email);
        if (member != null) {
            account.firstSession = new Date(member.created.getTime());
            account.lastSession = new Date(member.lastSession.getTime());
            account.altName = member.permaName;
            MemberWarningRecord warning =
                MsoyBaseServer.memberRepo.loadMemberWarningRecord(member.memberId);
            if (warning != null) {
                account.tempBan = warning.banExpires == null ?
                    null : new Date(warning.banExpires.getTime());
                account.warning = warning.warning;
            }
        }
    }

    // maximum number of display names to return
    protected static final int LOOKUP_LIMIT = 50;
}
