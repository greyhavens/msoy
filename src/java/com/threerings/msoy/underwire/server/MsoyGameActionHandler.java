//
// $Id$

package com.threerings.msoy.underwire.server;

import java.sql.Timestamp;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.web.server.MemberHelper;

import com.threerings.underwire.server.GameActionHandler;

/**
 * Provides whirled game-specific action handling.
 */
@Singleton
public class MsoyGameActionHandler extends GameActionHandler
{
    @Override // from GameActionHandler
    public void init (ConnectionProvider conprov)
    {
        // nothing to init
    }

    @Override // from GameActionHandler
    public void ban (String accountName)
    {
        int memberId = getMemberId(accountName);
        if (memberId > 0) {
            bootMember(memberId);
        }
    }

    @Override // from GameActionHandler
    public void tempBan (String accountName, Timestamp expires, String warning)
    {
        int memberId = getMemberId(accountName);
        if (memberId > 0) {
            _memberRepo.tempBanMember(memberId, expires, warning);
            bootMember(memberId);
        }
    }

    @Override // from GameActionHandler
    public void warn (String accountName, String warning)
    {
        int memberId = getMemberId(accountName);
        if (memberId > 0) {
            if (StringUtil.isBlank(warning)) {
                _memberRepo.clearMemberWarning(memberId);
            } else {
                _memberRepo.updateMemberWarning(memberId, warning);
            }
        }
    }

    /**
     * Boots a member off any active session and clears their web session token as well.
     */
    protected void bootMember (final int memberId)
    {
        // boot the player from the flash client
        MemberNodeActions.bootMember(memberId);

        // then clear out their session data from the web client
        _mhelper.clearSessionToken(memberId);
        _memberRepo.clearSession(memberId);
    }

    protected int getMemberId (String accountName)
    {
        try {
            return Integer.parseInt(accountName);
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    // our dependencies
    @Inject protected MemberHelper _mhelper;
    @Inject protected MemberRepository _memberRepo;
}
