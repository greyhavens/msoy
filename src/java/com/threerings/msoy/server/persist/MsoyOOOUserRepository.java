//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Date;

import java.util.ArrayList;
import java.util.Arrays;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.ConnectionProvider;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.clause.Where;

import com.samskivert.servlet.user.Password;
import com.samskivert.servlet.user.UserExistsException;
import com.samskivert.servlet.user.UserRepository;
import com.samskivert.servlet.user.Username;

import com.samskivert.util.ArrayUtil;

import com.threerings.user.Log;
import com.threerings.user.OOOUser;
import com.threerings.user.OOOUserRepository;

/**
 * Whirled-specific table-compatible simulation of the parts of {@link OOOUserRepository} we want.
 */
public class MsoyOOOUserRepository extends DepotRepository
{
    public MsoyOOOUserRepository (ConnectionProvider conprov)
    {
        super(new PersistenceContext(UserRepository.USER_REPOSITORY_IDENT, conprov));
    }
    
    /**
     * Looks up a user by email address and optionally loads their machine identifier information.
     *
     * @return the user with the specified address or null if no such user exists.
     */
    public OOOUser loadUserByEmail (String email, boolean loadIdents)
        throws PersistenceException
    {
        OOOUser user = toUser(load(OOOUserRecord.class, new Where(OOOUserRecord.EMAIL_C, email)));
        if (user != null && loadIdents) {
            loadMachineIdents(user);
        }
        return user;
    }

    /**
     * Looks up a user by userid.
     *
     * @return the user with the specified user id or null if no user with that id exists.
     */
    public OOOUser loadUser (int userId)
        throws PersistenceException
    {
        return toUser(load(OOOUserRecord.class, userId));
    }

    /** Converts a {@link OOOUserRecord} to a {@link OOOUser}. */
    protected OOOUser toUser(OOOUserRecord record)
    {
        if (record == null) {
            return null;
        }
        OOOUser user = new OOOUser();
        user.affiliateTagId = record.affiliateTagId;
        user.created = record.created;
        user.email = record.email;
        user.flags = record.flags;
        user.password = record.password;
        user.realname = record.realname;
        user.shunLeft = record.shunLeft;
        user.siteId = record.siteId;
        user.spots = record.spots;
        user.tokens = record.tokens;
        user.userId = record.userId;
        user.username = record.username;
        user.yohoho = record.yohoho;
        return user;
    }

    /** Converts a {@link OOOUser} to a {@link OOOUserRecord}. */
    protected OOOUserRecord toRecord(OOOUser user)
    {
        if (user == null) {
            return null;
        }
        OOOUserRecord record = new OOOUserRecord();
        record.affiliateTagId = user.affiliateTagId;
        record.created = user.created;
        record.email = user.email;
        record.flags = user.flags;
        record.password = user.password;
        record.realname = user.realname;
        record.shunLeft = user.shunLeft;
        record.siteId = user.siteId;
        record.spots = user.spots;
        record.tokens = user.tokens;
        record.userId = user.userId;
        record.username = user.username;
        record.yohoho = user.yohoho;
        return record;
    }

    /**
     * Looks up a user by username.
     *
     * @return the user with the specified user id or null if no user with that id exists.
     */
    public OOOUser loadUser (String username)
        throws PersistenceException
    {
        return toUser(load(OOOUserRecord.class, new Where(OOOUserRecord.USERNAME_C, username)));
    }

    /**
     * Loads up the machine ident information for the supplied user.
     */
    public void loadMachineIdents (OOOUser user)
        throws PersistenceException
    {
        // fill in this user's known machine identifiers
        ArrayList<String> idents = new ArrayList<String>();
        Where where = new Where(UserIdentRecord.USER_ID_C, user.userId);
        for (UserIdentRecord record : findAll(UserIdentRecord.class, where)) {
            idents.add(record.machIdent);
        }
        user.machIdents = idents.toArray(new String[idents.size()]);
        // sort the idents in java to ensure correct collation
        Arrays.sort(user.machIdents);
    }

    /**
     * Checks whether or not the user in question should be allowed access.
     *
     * @param site the site for which we are validating the user.
     * @param newPlayer true if the user is attempting to create a new game account.
     *
     * @return {@link #ACCESS_GRANTED} if the account should be allowed access, {@link
     * #NEW_ACCOUNT_TAINTED} if this is the account's first session and they are logging in with a
     * tainted machine ident, {@link #ACCOUNT_BANNED} if this account is banned, {@link #DEADBEAT}
     * if this account needs to resolve an outstanding debt.
     */
    public int validateUser (int site, OOOUser user, String machIdent, boolean newPlayer)
        throws PersistenceException
    {
        // if this user's idents were not loaded, complain
        if (user.machIdents == OOOUser.IDENTS_NOT_LOADED) {
            Log.warning("Requested to validate user with unloaded idents " +
                        "[who=" + user.username + "].");
            Thread.dumpStack();
            // err on the side of not screwing our customers
            return OOOUserRepository.ACCESS_GRANTED;
        }

        // if we have never seen them before...
        if (user.machIdents == null) {
            // add their ident to the userobject and db
            user.machIdents = new String[] { machIdent };
            addUserIdent(user.userId, machIdent);

        } else if (Arrays.binarySearch(user.machIdents, machIdent) < 0) {
            // add the machIdent to the users list of associated idents
            user.machIdents = ArrayUtil.append(user.machIdents, machIdent);
            // and slap it in the db
            addUserIdent(user.userId, machIdent);
        }

        // if this is a banned user, mark that ident
        if (user.isBanned(site)) {
            addTaintedIdent(machIdent);
            return OOOUserRepository.ACCOUNT_BANNED;
        }

        // don't let those bastards grief us.
        if (newPlayer && (isTaintedIdent(machIdent)) ) {
            return OOOUserRepository.NEW_ACCOUNT_TAINTED;
        }

        // if the user has bounced a check or reversed payment, let them know
        if (user.isDeadbeat(site)) {
            return OOOUserRepository.DEADBEAT;
        }

        // you're all clear kid...
        return OOOUserRepository.ACCESS_GRANTED;
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
     * Updates a user that was previously fetched from the repository.
     */
    public void updateUser (OOOUser user)
        throws PersistenceException
    {
        update(toRecord(user));
    }

    /**
     * Checks to see if the specified machine identifier is tainted.
     */
    public boolean isTaintedIdent (String machIdent)
        throws PersistenceException
    {
        return load(TaintedIdentRecord.class, machIdent) != null;
    }

    /**
     * Store to the database that the passed in machIdent has been tainted by a banned player.
     */
    public void addTaintedIdent (String machIdent)
        throws PersistenceException
    {
        insert(new TaintedIdentRecord(machIdent));
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
            return 0 != updatePartial(OOOUserRecord.class, userId, new Object[] {
                OOOUserRecord.USERNAME, username
            });
        } catch (PersistenceException pe) {
            // TODO: Check for duplicate row
//                    if (liaison.isDuplicateRowException(sqe)) {
//                        throw new UserExistsException("error.user_exists");
//                    } else {
//                        throw sqe;
//                    }
            throw pe;
        }
    }

    /**
     * Creates a new user record in the repository with no auxiliary data.
     */
    public int createUser (
        Username username, Password password, String email, int siteId, int tagId)
        throws UserExistsException, PersistenceException
    {
        OOOUserRecord user = new OOOUserRecord();

        // fill in the base user information
        user.username = username.getUsername();
        user.password = password.getEncrypted();
        user.realname = "";
        user.email = email;
        user.created = new Date(System.currentTimeMillis());
        user.siteId = siteId;
        
        // fill in the ooo-specific user information
        user.tokens = new byte[0];
        user.spots = "";
        user.affiliateTagId = tagId;

        insert(user);

        insert(new HistoricalUserRecord(user.userId, username.getUsername(), user.created, siteId));

        return user.userId;
    }
    
    // documentation inherited
    protected void populateUser (OOOUser user, Username uname, Password pass, String email,
                                 int siteId, int tagId)
    {
        // fill in the base user information
        user.username = uname.getUsername();
        user.setPassword(pass);
        user.setRealName("");
        user.setEmail(email);
        user.created = new Date(System.currentTimeMillis());
        user.setSiteId(siteId);

        // fill in the ooo-specific user information
        user.tokens = new byte[0];
        user.spots = "";
        user.affiliateTagId = tagId;
    }

    /** The number of days in the past from now where we no longer
     * consider an account as 'recent' */
    protected static final int RECENT_ACCOUNT_CUTOFF = -3*30;

    /** The number of free accounts that can be created per machine. */
    protected static final int MAX_FREE_ACCOUNTS_PER_MACHINE = 2;
}
