//
// $Id$

package com.threerings.msoy.server.persist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import java.sql.Date;

import com.samskivert.util.Tuple;
import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.DuplicateKeyException;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.operator.Conditionals.Equals;
import com.samskivert.jdbc.depot.operator.Conditionals.In;
import com.samskivert.jdbc.depot.operator.Logic.And;
import com.samskivert.jdbc.depot.operator.SQLOperator;

import com.samskivert.servlet.user.User;
import com.samskivert.servlet.user.UserExistsException;
import com.samskivert.servlet.user.Username;
import com.samskivert.servlet.user.UserUtil;

import com.threerings.underwire.server.persist.SupportRepository;

import com.threerings.user.OOOUser;
import com.threerings.user.OOOUserRepository;

/**
 * Whirled-specific table-compatible simulation of the parts of {@link OOOUserRepository} we want.
 */
public class MsoyOOOUserRepository extends DepotRepository
    implements SupportRepository
{
    public MsoyOOOUserRepository (PersistenceContext ctx)
    {
        super(ctx);
    }
    /**
     * Looks up a user by userid.
     *
     * @return the user with the specified user id or null if no user with that id exists.
     */
    public OOOUserRecord loadUserRecord (int userId)
        throws PersistenceException
    {
        return load(OOOUserRecord.class, userId);
    }

    // documentation inherited by SupportRepository
    public User loadUser (int userId)
        throws PersistenceException
    {
        return toUser(loadUserRecord(userId));
    }

    /**
     * Looks up a user by username.
     *
     * @return the user with the specified user id or null if no user with that id exists.
     */
    public OOOUserRecord loadUserRecord (String username)
        throws PersistenceException
    {
        return load(OOOUserRecord.class, new Where(OOOUserRecord.USERNAME_C, username));
    }

    // documentation inherited by SupportRepository
    public OOOUser loadUser (String username, boolean loadIdents)
        throws PersistenceException
    {
        OOOUser user = toUser(loadUserRecord(username));
        if (user == null || !loadIdents) {
            return user;
        }
        loadMachineIdents(user);
        return user;
    }

    // documentation inherited by SupportRepository
    public User loadUserBySession (String sessionKey)
        throws PersistenceException
    {
        SQLOperator joinCondition = new And(
                new Equals(OOOUserRecord.USER_ID_C, SessionRecord.MEMBER_ID_C),
                new Equals(SessionRecord.TOKEN_C, sessionKey));
        return toUser(load(OOOUserRecord.class, new Join(SessionRecord.class, joinCondition)));
    }

    /**
     * Looks up a user by email address.
     *
     * @return the user with the specified address or null if no such user exists.
     */
    public OOOUserRecord loadUserRecordByEmail (String email)
        throws PersistenceException
    {
        return load(OOOUserRecord.class, new Where(OOOUserRecord.EMAIL_C, email));
    }

    // documentation inherited from SupportRepository
    public String[] getUsernames (String email)
        throws PersistenceException
    {
        ArrayList<String> usernames = new ArrayList<String>();
        Where where = new Where(OOOUserRecord.EMAIL_C, email);
        for (OOOUserRecord record : findAll(OOOUserRecord.class, where)) {
            usernames.add(record.username);
        }
        return usernames.toArray(new String[usernames.size()]);
    }

    // documentation inherited from SupportRepository
    public List<String> getTokenUsernames (Collection<String> usernames, byte token)
        throws PersistenceException
    {
        // We're doing a manual token check after loading the users, however ideally having the
        // depot support for a regexp comparison on a hex converted tokens field would be faster
        ArrayList<String> retnames = new ArrayList<String>();
        Where where = new Where(new In(OOOUserRecord.USERNAME_C, usernames));
        for (OOOUserRecord record : findAll(OOOUserRecord.class, where)) {
            if (record.holdsToken(token)) {
                retnames.add(record.username);
            }
        }
        return retnames;
    }

    /**
     * Loads up the machine ident information for the supplied user.
     */
    public String[] loadMachineIdents (int userId)
        throws PersistenceException
    {
        ArrayList<String> idents = new ArrayList<String>();
        Where where = new Where(UserIdentRecord.USER_ID_C, userId);
        for (UserIdentRecord record : findAll(UserIdentRecord.class, where)) {
            idents.add(record.machIdent);
        }
        String[] machIdents = idents.toArray(new String[idents.size()]);
        Arrays.sort(machIdents); // sort the idents in java to ensure correct collation
        return machIdents;
    }

    // documentation inherited from SupportRepository
    public void loadMachineIdents (OOOUser user)
        throws PersistenceException
    {
        user.machIdents = loadMachineIdents(user.userId);
    }

    // documentation inherited from SupportRepository
    public List<Tuple<Integer, String>> getUsersOfMachIdent (String machIdent)
        throws PersistenceException
    {
        List<Tuple<Integer,String>> users = new ArrayList<Tuple<Integer,String>>();
        Join join = new Join(UserIdentRecord.class, new And(
                new Equals(OOOUserRecord.USER_ID_C, UserIdentRecord.USER_ID_C),
                new Equals(UserIdentRecord.MACH_IDENT_C, machIdent)));
        for (OOOUserRecord record : findAll(OOOUserRecord.class, join)) {
            users.add(new Tuple<Integer,String>(record.userId, record.username));
        }
        return users;
    }

    /**
     * Add the userId -> machIdent mapping to the database.
     */
    public void addUserIdent (int userId, String machIdent)
        throws PersistenceException
    {
        insert(new UserIdentRecord(userId, machIdent));
    }

    /**
     * Checks to see if the specified machine identifier is tainted.
     */
    public boolean isTaintedIdent (String machIdent)
        throws PersistenceException
    {
        return load(TaintedIdentRecord.class, machIdent) != null;
    }

    // documentation inherited from SupportRepository
    public Collection<String> filterTaintedIdents (String[] idents)
        throws PersistenceException
    {
        ArrayList<String> tainted = new ArrayList<String>();
        if (idents != null && idents.length >= 0) {
            Where where = new Where(new In(TaintedIdentRecord.MACH_IDENT_C, Arrays.asList(idents)));
            for (TaintedIdentRecord record : findAll(TaintedIdentRecord.class, where)) {
                tainted.add(record.machIdent);
            }
        }
        return tainted;
    }

    /**
     * Store to the database that the passed in machIdent has been tainted by a banned player.
     */
    public void addTaintedIdent (String machIdent)
        throws PersistenceException
    {
        insert(new TaintedIdentRecord(machIdent));
    }

    // documentation inherited from SupportRepository
    public void removeTaintedIdent (String machIdent)
        throws PersistenceException
    {
        delete(TaintedIdentRecord.class, machIdent);
    }

    // documentation inherited from SupportRepository
    public Collection<String> filterBannedIdents (String[] idents, int siteId)
        throws PersistenceException
    {
        ArrayList<String> banned = new ArrayList<String>();
        if (idents != null && idents.length >= 0) {
            Where where = new Where(new And(
                        new Equals(BannedIdentRecord.SITE_ID_C, siteId),
                        new In(BannedIdentRecord.MACH_IDENT_C, Arrays.asList(idents))));
            for (BannedIdentRecord record : findAll(BannedIdentRecord.class, where)) {
                banned.add(record.machIdent);
            }
        }
        return banned;
    }

    /**
     * Store to the database that the passed in machIdent has been banned on the site.
     */
    public void addBannedIdent (String machIdent, int siteId)
        throws PersistenceException
    {
        insert(new BannedIdentRecord(machIdent, siteId));
    }

    // documentation inherited from SupportRepository
    public void removeBannedIdent (String machIdent, int siteId)
        throws PersistenceException
    {
        delete(BannedIdentRecord.class, BannedIdentRecord.getKey(machIdent, siteId));
    }

    /**
     * Creates a new user record in the repository with no auxiliary data.
     */
    public int createUser (Username username, String password, String email)
        throws UserExistsException, PersistenceException
    {
        OOOUserRecord user = new OOOUserRecord();

        // fill in the base user information
        user.username = username.getUsername();
        user.password = password;
        user.realname = "";
        user.email = email;
        user.created = new Date(System.currentTimeMillis());
        user.siteId = OOOUser.METASOY_SITE_ID;

        // fill in the ooo-specific user information
        user.tokens = new byte[0];
        user.spots = "";
        user.affiliateTagId = 0;

        insert(user);
        insert(new HistoricalUserRecord(
                   user.userId, username.getUsername(), user.created, user.siteId));

        return user.userId;
    }

    /**
     * Changes a user's username.
     *
     * @return true if the old username existed and was changed to the new name, false if the old
     * username did not exist.
     *
     * @exception UserExistsException thrown if the new name is already in use.
     */
    public boolean changeUsername (int userId, String username)
        throws PersistenceException, UserExistsException
    {
        try {
            return 0 != updatePartial(
                OOOUserRecord.class, userId, OOOUserRecord.USERNAME, username);
        } catch (DuplicateKeyException pe) {
            throw new UserExistsException("error.user_exists");
        }
    }

    /**
     * Updates the specified user's email address.
     */
    public void changeEmail (int userId, String email)
        throws PersistenceException
    {
        updatePartial(OOOUserRecord.class, userId, OOOUserRecord.EMAIL, email);
    }

    /**
     * Updates the specified user's password (should already be encrypted).
     */
    public void changePassword (int userId, String password)
        throws PersistenceException
    {
        updatePartial(OOOUserRecord.class, userId, OOOUserRecord.PASSWORD, password);
    }

    // documentation inherited from SupportRepository
    public boolean updateUser (User user)
        throws PersistenceException
    {
        return update(OOOUserRecord.fromUser((OOOUser)user)) == 1;
    }

    /**
     * Creates a new session for the specified user and returns the randomly generated session
     * identifier for that session.  If a session entry already exists for the specified user it
     * will be reused.
     *
     * @param expireDays the number of days in which the session token should expire.
     */
    public String registerSession (User user, int expireDays)
        throws PersistenceException
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

    // documentation inherited from SupportRepository
    public boolean refreshSession (String sessionKey, int expireDays)
        throws PersistenceException
    {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, expireDays);
        Date expires = new Date(cal.getTime().getTime());

        return updatePartial(SessionRecord.class, sessionKey, SessionRecord.EXPIRES, expires) == 1;
    }

    // documentation inherited from SupportRepository
    public boolean ban (int site, String username)
        throws PersistenceException
    {
        // Not currently tainting every system this user has ever touched or will touch in the
        // future
        OOOUser user = loadUser(username, false);
        if (user == null) {
            return false;
        }

        if (!user.setBanned(site, true)) {
            return false;
        }
        updateUser(user);
        return true;
    }

    // documentation inherited from SupportRepository
    public boolean unban (int site, String username, boolean untaint)
        throws PersistenceException
    {
        // Not currently tainting every system this user has ever touched or will touch in the
        // future
        OOOUser user = loadUser(username, untaint);
        if (user == null) {
            return false;
        }

        if (!user.setBanned(site, true)) {
            return false;
        }
        updateUser(user);

        if (untaint) {
            for (int ii = 0; ii < user.machIdents.length; ii++) {
                removeTaintedIdent(user.machIdents[ii]);
            }
        }
        return true;
    }

    /**
     * Converts a possibly null OOOUserRecord to a OOOUser.
     */
    public OOOUser toUser (OOOUserRecord record)
    {
        return (record == null ? null : record.toUser());
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(OOOUserRecord.class);
        classes.add(UserIdentRecord.class);
        classes.add(TaintedIdentRecord.class);
        classes.add(BannedIdentRecord.class);
        classes.add(HistoricalUserRecord.class);
    }
}
