//
// $Id$

package com.threerings.msoy.underwire.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.servlet.user.AuthenticationFailedException;
import com.samskivert.servlet.user.InvalidPasswordException;
import com.samskivert.servlet.user.NoSuchUserException;

import com.threerings.user.OOOUser;

import com.threerings.underwire.server.SupportUserLogic;
import com.threerings.underwire.web.client.UnderwireException;
import com.threerings.underwire.web.data.Account;

import com.threerings.msoy.data.MsoyAuthCodes;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyAuthenticator;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.MsoyOOOUserRepository;
import com.threerings.msoy.web.gwt.ServiceException;

import com.threerings.msoy.underwire.gwt.MsoyAccount;

import static com.threerings.msoy.Log.log;

/**
 * Customizes the {@link SupportUserLogic} with some MSOY bits.
 */
@Singleton
public class MsoyUserLogic extends SupportUserLogic
{
    @Inject public MsoyUserLogic (MsoyUnderContext ctx, MsoyOOOUserRepository authRepo)
    {
        super(ctx, authRepo);
    }

    @Override // from SupportUserLogic
    public Caller loadCaller (String authtok)
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

    @Override // from SupportUserLogic
    public Caller userLogin (String username, String password, int expireDays)
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

    @Override // from SupportUserLogic
    protected String getUsername (OOOUser user)
        throws UnderwireException
    {
        return getUsername(user.email);
    }

    @Override // from SupportUserLogic
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
            throw new UnderwireException("m.internal_error");
        }
        return String.valueOf(name.getMemberId());
    }

    @Override // from SupportUserLogic
    protected Account createAccount ()
    {
        return new MsoyAccount();
    }

    @Override // from SupportUserLogic
    protected void setUserEmail (OOOUser user, String email)
    {
        if (user.username.equals(user.email)) {
            user.username = email;
        }
        user.email = email;
    }

    @Inject protected MemberRepository _memberRepo;
    @Inject protected MsoyAuthenticator _author;
}
