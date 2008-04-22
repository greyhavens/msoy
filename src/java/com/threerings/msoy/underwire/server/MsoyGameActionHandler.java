//
// $Id$

package com.threerings.msoy.underwire.server;

import java.sql.Timestamp;

import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.web.server.ServletUtil;

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
    public void ban (String accountName)
        throws PersistenceException
    {
        int memberId = getMemberId(accountName);
        if (memberId > 0) {
            bootMember(memberId);
        }
    }

    @Override // from GameActionHandler
    public void tempBan (String accountName, Timestamp expires, String warning)
        throws PersistenceException
    {
        int memberId = getMemberId(accountName);
        if (memberId > 0) {
            MsoyServer.memberRepo.tempBanMember(memberId, expires, warning);
            bootMember(memberId);
        }
    }

    @Override // from GameActionHandler
    public void warn (String accountName, String warning)
        throws PersistenceException
    {
        int memberId = getMemberId(accountName);
        if (memberId > 0) {
            if (StringUtil.isBlank(warning)) {
                MsoyServer.memberRepo.clearMemberWarning(memberId);
            } else {
                MsoyServer.memberRepo.updateMemberWarning(memberId, warning);
            }
        }
    }

    /**
     * Boots a member off any active session and clears their web session token as well.
     */
    protected void bootMember (final int memberId)
        throws PersistenceException
    {
        // boot the player from the flash client
        MemberNodeActions.bootMember(memberId);

        // then clear out their session data from the web client
        ServletUtil.clearSessionToken(memberId);
        MsoyServer.memberRepo.clearSession(memberId);
    }

    protected int getMemberId (String accountName)
    {
        try {
            return Integer.parseInt(accountName);
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }
}
