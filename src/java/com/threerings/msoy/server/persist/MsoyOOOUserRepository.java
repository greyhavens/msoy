//
// $Id$

package com.threerings.msoy.server.persist;

import java.util.Calendar;

import java.sql.Date;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.operator.Conditionals.Equals;
import com.samskivert.jdbc.depot.operator.Logic.And;
import com.samskivert.jdbc.depot.operator.SQLOperator;

import com.samskivert.servlet.user.User;
import com.samskivert.servlet.user.UserUtil;

import com.threerings.user.OOOUser;
import com.threerings.user.depot.DepotUserRepository;
import com.threerings.user.depot.HistoricalUserRecord;
import com.threerings.user.depot.OOOUserRecord;
import com.threerings.user.depot.UserIdentRecord;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.underwire.server.persist.SupportRepository;

/**
 * Whirled-specific table-compatible simulation of the parts of the user repository that we want.
 */
@Singleton @BlockingThread
public class MsoyOOOUserRepository extends DepotUserRepository
    implements SupportRepository
{
    @Inject public MsoyOOOUserRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Deletes all records associated with the supplied user id. This should only be used when
     * account creation failed and we want to wipe a user account that never existed. For user
     * initiated account deletion use {@link #disableUser} (not yet implemented).
     */
    public void uncreateUser (int userId)
    {
        delete(OOOUserRecord.class, userId);
        delete(HistoricalUserRecord.class, userId);
        deleteAll(UserIdentRecord.class, new Where(UserIdentRecord.USER_ID_C, userId));
    }

    // from SupportRepository
    public OOOUser loadUserByAccountName (String accountName)
    {
        int memberId;
        try {
            memberId = Integer.valueOf(accountName);
        } catch (NumberFormatException nfe) {
            return null;
        }
        SQLOperator joinCondition = new And(
                new Equals(MemberRecord.MEMBER_ID_C, memberId),
                new Equals(OOOUserRecord.EMAIL_C, MemberRecord.ACCOUNT_NAME_C));
        return toUser(load(OOOUserRecord.class, new Join(MemberRecord.class, joinCondition)));
    }

    // from SupportRepository
    public User loadUserBySession (String sessionKey)
    {
        SQLOperator joinCondition = new And(
                new Equals(OOOUserRecord.USER_ID_C, SessionRecord.MEMBER_ID_C),
                new Equals(SessionRecord.TOKEN_C, sessionKey));
        return toUser(load(OOOUserRecord.class, new Join(SessionRecord.class, joinCondition)));
    }

    /**
     * Creates a new session for the specified user and returns the randomly generated session
     * identifier for that session.  If a session entry already exists for the specified user it
     * will be reused.
     *
     * @param expireDays the number of days in which the session token should expire.
     */
    public String registerSession (User user, int expireDays)
    {
        // see if we have a pre-existing session for this user
        SessionRecord session = load(SessionRecord.class,
                new Where(SessionRecord.MEMBER_ID_C, user.userId));

        // figure out when to expire the session
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, expireDays);
        Date expires = new Date(cal.getTime().getTime());

        // if we found one, update its expires time and reuse it
        if (session != null) {
            updatePartial(SessionRecord.class, session.token, SessionRecord.EXPIRES, expires);

        // otherwire create a new one and insert it into the table
        } else {
            session = new SessionRecord(UserUtil.genAuthCode(user));
            session.memberId = user.userId;
            session.expires = expires;
            store(session);
        }
        return session.token;
    }

    // from SupportRepository
    public boolean refreshSession (String sessionKey, int expireDays)
    {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, expireDays);
        Date expires = new Date(cal.getTime().getTime());
        return updatePartial(SessionRecord.class, sessionKey, SessionRecord.EXPIRES, expires) == 1;
    }
}
