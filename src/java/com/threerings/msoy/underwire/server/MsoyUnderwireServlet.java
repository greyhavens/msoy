//
// $Id$

package com.threerings.msoy.underwire.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.depot.PersistenceContext;

import com.samskivert.servlet.IndiscriminateSiteIdentifier;
import com.samskivert.servlet.SiteIdentifier;
import com.samskivert.servlet.user.AuthenticationFailedException;
import com.samskivert.servlet.user.InvalidPasswordException;
import com.samskivert.servlet.user.NoSuchUserException;
import com.samskivert.servlet.util.CookieUtil;

import com.threerings.presents.data.InvocationCodes;

import com.threerings.user.OOOUser;

import com.threerings.underwire.server.GameActionHandler;
import com.threerings.underwire.server.GameInfoProvider;
import com.threerings.underwire.server.persist.SupportRepository;
import com.threerings.underwire.server.persist.UnderwireRepository;
import com.threerings.underwire.web.client.AuthenticationException;
import com.threerings.underwire.web.client.UnderwireException;
import com.threerings.underwire.web.data.Account;
import com.threerings.underwire.web.server.UnderwireServlet;

import com.threerings.msoy.server.MsoyAuthenticator;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.MsoyOOOUserRepository;
import com.threerings.msoy.underwire.gwt.MsoyAccount;
import com.threerings.msoy.underwire.gwt.SupportService;
import com.threerings.msoy.underwire.gwt.MsoyAccount.SocialStatus;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.gwt.WebCreds;
import com.threerings.msoy.web.server.MemberHelper;

import static com.threerings.msoy.Log.log;

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
        if (greeter != memberRec.isGreeter() || troublemaker != memberRec.isTroublemaker()) {
            memberRec.setFlag(MemberRecord.Flag.GREETER, greeter);
            memberRec.setFlag(MemberRecord.Flag.TROUBLEMAKER, troublemaker);
            _memberRepo.storeFlags(memberRec);
            recordEvent(String.valueOf(caller.memberId), String.valueOf(memberId),
                        "Changed social status to " + status);
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
    protected SiteIdentifier createSiteIdentifier ()
    {
        return new IndiscriminateSiteIdentifier();
    }

    @Override // from UnderwireServlet
    protected SupportRepository createSupportRepository ()
    {
        return _authRepo;
    }

    @Override // from UnderwireServlet
    protected UnderwireRepository createUnderwireRepository ()
    {
        return _underRepo;
    }

    @Override // from UnderwireServlet
    protected Caller userLogin (String username, String password, int expireDays)
        throws AuthenticationFailedException
    {
        try {
            MemberRecord mrec = _author.authenticateSession(username, password);

            Caller caller = new Caller();
            caller.username = Integer.toString(mrec.memberId);
            caller.email = mrec.accountName;
            caller.authtok = _memberRepo.startOrJoinSession(mrec.memberId, expireDays);
            caller.isSupport = mrec.isSupport();
            return caller;

        } catch (ServiceException se) {
            // convert the exception into the required type
            String message = se.getMessage();
            if (message.equals(MsoyAuthCodes.NO_SUCH_USER)) {
                throw new NoSuchUserException(message);
            } else if (message.equals(MsoyAuthCodes.INVALID_PASSWORD)) {
                throw new InvalidPasswordException(message);
            } else {
                throw new AuthenticationFailedException(message);
            }
        }
    }

    @Override // from UnderwireServlet
    protected boolean allowEmailUpdate ()
    {
        return false;
    }

    @Override // from UnderwireServlet
    protected String getUsername (OOOUser user)
        throws UnderwireException
    {
        return getUsername(user.email);
    }

    @Override // from UnderwireServlet
    protected String getUsername (String username)
        throws UnderwireException
    {
        MemberName name;
        try {
            name = _memberRepo.loadMemberName(username);
        } catch (Exception e) {
            log.warning("Error looking up member", e);
            name = null; // handled with the next check
        }
        if (name == null) {
            log.warning("Unable to find member information [email=" + username + "].");
            throw new UnderwireException(InvocationCodes.INTERNAL_ERROR);
        }
        return String.valueOf(name.getMemberId());
    }

    @Override // from UnderwireServlet
    protected int getSiteId ()
    {
        return OOOUser.METASOY_SITE_ID;
    }

    @Override // from UnderwireServlet
    protected GameInfoProvider getInfoProvider ()
    {
        return _infoprov;
    }

    @Override // from UnderwireServlet
    protected GameActionHandler getActionHandler ()
    {
        return _actionHandler;
    }

    @Override // from UnderwireServlet
    protected Caller loadCaller (String authtok)
        throws UnderwireException
    {
        try {
            MemberRecord mrec = _memberRepo.loadMemberForSession(authtok);
            if (mrec == null) {
                return null;
            }

            Caller caller = new Caller();
            caller.authtok = authtok;
            caller.username = String.valueOf(mrec.memberId);
            caller.email = mrec.accountName;
            caller.isSupport = mrec.isSupport();
            return caller;

        } catch (Exception e) {
            log.warning("Failed to load caller [tok=" + authtok + "].", e);
            throw new UnderwireException("m.internal_error");
        }
    }

    @Override protected Account createAccount ()
    {
        return new MsoyAccount();
    }

    @Singleton
    protected static class MsoyUnderwireRepository extends UnderwireRepository
    {
        @Inject public MsoyUnderwireRepository (PersistenceContext perCtx) {
            super(perCtx);
        }
    }

    // our dependencies
    @Inject protected PersistenceContext _perCtx;
    @Inject protected MsoyGameActionHandler _actionHandler;
    @Inject protected MsoyGameInfoProvider _infoprov;
    @Inject protected MsoyAuthenticator _author;
    @Inject protected MsoyOOOUserRepository _authRepo;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyUnderwireRepository _underRepo;
    @Inject protected MemberHelper _memberHelper;
}
