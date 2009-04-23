//
// $Id$

package com.threerings.msoy.server.persist;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import java.sql.Date;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.clause.Join;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.operator.Conditionals.Equals;
import com.samskivert.depot.operator.Conditionals;
import com.samskivert.depot.operator.Logic.And;
import com.samskivert.depot.operator.SQLOperator;

import com.samskivert.servlet.user.User;
import com.samskivert.servlet.user.UserUtil;

import com.threerings.msoy.server.persist.RecordFunctions;
import com.threerings.user.OOOUser;
import com.threerings.user.depot.DepotUserRepository;
import com.threerings.user.depot.HistoricalUserRecord;
import com.threerings.user.depot.OOOUserRecord;
import com.threerings.user.depot.UserIdentRecord;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.underwire.server.persist.SupportRepository;

/**
 * Whirled-specific table-compatible simulation of the parts of the user repository that we want.
 *
 * WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING
 * WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING
 *
 * OOOUser.userId != MemberRecord.memberId
 *
 * WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING
 * WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING
 *
 * Do not do anything with MemberRepository here. If you need to coordinate changes to MemberRecord
 * and OOOUserRecord, you need to do it in the code that calls into this repository.
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
     * initiated account deletion use to be implemented <code>disableUser</code>.
     */
    public void uncreateUser (int userId)
    {
        delete(OOOUserRecord.class, userId);
        delete(HistoricalUserRecord.class, userId);
        deleteAll(UserIdentRecord.class, new Where(UserIdentRecord.USER_ID, userId));
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
                new Where(SessionRecord.MEMBER_ID, user.userId));

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

    /**
     * Deletes all data associated with the supplied members. This is done as a part of purging
     * member accounts.
     */
    public void purgeMembers (Collection<String> emails)
    {
        List<Integer> userIds = Lists.transform(
            findAllKeys(OOOUserRecord.class, false,
                        new Where(new Conditionals.In(OOOUserRecord.EMAIL, emails))),
            RecordFunctions.<OOOUserRecord>getIntKey());
        if (!userIds.isEmpty()) {
            deleteAll(OOOUserRecord.class,
                      new Where(new Conditionals.In(OOOUserRecord.USER_ID, userIds)));
            deleteAll(HistoricalUserRecord.class,
                      new Where(new Conditionals.In(HistoricalUserRecord.USER_ID, userIds)));
            deleteAll(UserIdentRecord.class,
                      new Where(new Conditionals.In(UserIdentRecord.USER_ID, userIds)));
        }
    }

    // from SupportRepository
    public OOOUser loadUserByAccountName (String accountName)
    {
        // this is some hackery that we do to integerate Underwire with Whirled: we provide
        // MemberRecord.memberId to Underwire as the account's string name, then when Underwire
        // looks up accounts by name, we turn the string back into a memberId, look up the email
        // address for that member and then use that to load the OOOUser record for the member in
        // question; the only guarantee is that MemberRecord.accountName == OOOUser.email,
        // remember: MemberRecord.memberId != OOOUser.userId
        int memberId;
        try {
            memberId = Integer.valueOf(accountName);
        } catch (NumberFormatException nfe) {
            return null;
        }
        SQLOperator joinCondition = new And(
                new Equals(MemberRecord.MEMBER_ID, memberId),
                new Equals(OOOUserRecord.EMAIL, MemberRecord.ACCOUNT_NAME));
        return toUser(load(OOOUserRecord.class, new Join(MemberRecord.class, joinCondition)));
    }

    // from SupportRepository
    public User loadUserBySession (String sessionKey)
    {
        SQLOperator joinCondition = new And(
                new Equals(OOOUserRecord.USER_ID, SessionRecord.MEMBER_ID),
                new Equals(SessionRecord.TOKEN, sessionKey));
        return toUser(load(OOOUserRecord.class, new Join(SessionRecord.class, joinCondition)));
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
