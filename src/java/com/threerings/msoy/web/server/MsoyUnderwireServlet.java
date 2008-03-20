//
// $Id$

package com.threerings.msoy.web.server;

import com.samskivert.util.Tuple;

import com.samskivert.io.PersistenceException;

import com.samskivert.servlet.IndiscriminateSiteIdentifier;
import com.samskivert.servlet.SiteIdentifier;

import com.samskivert.servlet.user.AuthenticationFailedException;
import com.samskivert.servlet.user.InvalidPasswordException;
import com.samskivert.servlet.user.NoSuchUserException;
import com.samskivert.servlet.user.User;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.OOOAuthenticationDomain;

import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MsoyOOOUserRepository;

import com.threerings.msoy.data.MsoyAuthCodes;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.underwire.server.MsoyGameActionHandler;
import com.threerings.msoy.underwire.server.MsoyGameInfoProvider;

import com.threerings.msoy.web.data.ServiceException;

import com.threerings.underwire.server.GameActionHandler;
import com.threerings.underwire.server.GameInfoProvider;

import com.threerings.underwire.server.persist.SupportRepository;
import com.threerings.underwire.server.persist.UnderwireRepository;

import com.threerings.underwire.web.client.UnderwireException;

import com.threerings.underwire.web.server.UnderwireServlet;

import com.threerings.user.OOOUser;

import static com.threerings.msoy.Log.log;

/**
 * An underwire servlet which uses a the msoy connection provider and user manager.
 */
public class MsoyUnderwireServlet extends UnderwireServlet
{
    // documentation inherited from UnderwireServlet
    public SiteIdentifier createSiteIdentifier ()
    {
        return new IndiscriminateSiteIdentifier();
    }

    // documentation inherited from UnderwireServlet
    public SupportRepository createSupportRepository ()
    {
        return ((OOOAuthenticationDomain)MsoyServer.author.getDefaultDomain()).getRepository();
    }

    // documentation inherited from UnderwireServlet
    public UnderwireRepository createUnderwireRepository ()
    {
        return new UnderwireRepository(MsoyServer.userCtx);
    }

    // documentation inherited from UnderwireServlet
    public Tuple<User,String> userLogin (String username, String password, int expireDays)
        throws PersistenceException, AuthenticationFailedException
    {
        try {
            MemberRecord member = MsoyServer.author.authenticateSession(username, password);
            User user = ((MsoyOOOUserRepository)_supportrepo).loadUserByEmail(
                    member.accountName, false);
            String token = ((MsoyOOOUserRepository)_supportrepo).registerSession(user, expireDays);

            return new Tuple<User,String>(user, token);

        } catch (ServiceException se) {
            // convert the excpetion into the required type
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

    @Override // documentation inherited from UnderwireServlet
    public boolean allowEmailUpdate ()
    {
        return false;
    }

    @Override // documnetation inherited from UnderwireServlet
    public String getUsername (OOOUser user)
        throws UnderwireException
    {
        return getUsername(user.email);
    }

    @Override // documentation inherited from UnderwireServlet
    public String getUsername (String username)
        throws UnderwireException
    {
        MemberName name = null;
        try {
            name = MsoyServer.memberRepo.loadMemberName(username);
        } catch (PersistenceException pe) {
            // handled with the next check
        }
        if (name == null) {
            log.warning("Unable to find member information [email=" + username + "].");
            throw new UnderwireException("m.internal_error");
        }
        return Integer.toString(name.getMemberId());
    }

    @Override //documentation inherited from UnderwireServlet
    protected int getSiteId ()
    {
        return OOOUser.METASOY_SITE_ID;
    }

    // documentation inherited from UnderwireServlet
    protected GameInfoProvider getInfoProvider ()
    {
        if (_infoprov == null) {
            _infoprov = new MsoyGameInfoProvider();
        }
        return _infoprov;
    }

    // documentation inherited from UnderwireServlet
    protected GameActionHandler getActionHandler ()
    {
        if (_actionHandler == null) {
            _actionHandler = new MsoyGameActionHandler();
        }
        return _actionHandler;
    }

    protected MsoyGameInfoProvider _infoprov;
    protected MsoyGameActionHandler _actionHandler;
}
