//
// $Id$

package com.threerings.msoy.underwire.server;

import com.google.inject.Inject;

import com.samskivert.servlet.util.CookieUtil;

import com.threerings.user.OOOUser;

import com.threerings.underwire.server.UnderContext;
import com.threerings.underwire.server.UserLogic;
import com.threerings.underwire.web.client.AuthenticationException;
import com.threerings.underwire.web.client.UnderwireException;
import com.threerings.underwire.web.server.UnderwireServlet;

import com.threerings.msoy.server.MemberNodeActions;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.gwt.WebCreds;
import com.threerings.msoy.web.server.MemberHelper;

import com.threerings.msoy.underwire.gwt.MsoyAccount.SocialStatus;
import com.threerings.msoy.underwire.gwt.SupportService;

/**
 * An underwire servlet which uses a the msoy connection provider and user manager.
 */
public class MsoyUnderwireServlet extends UnderwireServlet
    implements SupportService
{
    // from SupportService
    public void setSocialStatus (int memberId, SocialStatus status)
        throws UnderwireException
    {
        MemberRecord caller = requireAuthedSupport();
        MemberRecord memberRec = _memberRepo.loadMember(memberId);
        boolean greeter = status == SocialStatus.GREETER;
        boolean troublemaker = status == SocialStatus.TROUBLEMAKER;
        boolean greeterChanged = greeter != memberRec.isGreeter();
        if (greeterChanged || troublemaker != memberRec.isTroublemaker()) {
            memberRec.setFlag(MemberRecord.Flag.GREETER, greeter);
            memberRec.setFlag(MemberRecord.Flag.TROUBLEMAKER, troublemaker);
            _memberRepo.storeFlags(memberRec);
            recordEvent(String.valueOf(caller.memberId), String.valueOf(memberId),
                        "Changed social status to " + status);

            if (greeterChanged) {
                // let the world servers know about the info change
                MemberNodeActions.tokensChanged(memberRec.memberId, memberRec.toTokenRing());
            }
        }
    }

    protected MemberRecord requireAuthedSupport ()
        throws UnderwireException
    {
        try {
            MemberRecord memberRecord = _memberHelper.requireAuthedUser(CookieUtil.getCookieValue(
                getThreadLocalRequest(), WebCreds.credsCookie()));
            if (memberRecord == null || !memberRecord.isSupport()) {
                throw new AuthenticationException("m.access_denied");
            }
            return memberRecord;

        } catch (ServiceException se) {
            throw new AuthenticationException("m.access_denied");
        }
    }

    @Override // from UnderwireServlet
    protected UnderContext createContext ()
    {
        return _underCtx;
    }

    @Override // from UnderwireServlet
    protected UserLogic createUserLogic (UnderContext ctx)
    {
        return _userLogic;
    }

    @Override // from UnderwireServlet
    protected int getSiteId ()
    {
        return OOOUser.METASOY_SITE_ID;
    }

    // our dependencies
    @Inject protected MemberHelper _memberHelper;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyUnderContext _underCtx;
    @Inject protected MsoyUserLogic _userLogic;
}
