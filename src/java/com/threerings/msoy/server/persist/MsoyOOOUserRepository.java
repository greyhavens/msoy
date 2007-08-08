//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Date;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.DuplicateKeyException;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.Where;

import com.samskivert.servlet.user.UserExistsException;
import com.samskivert.servlet.user.Username;

import com.threerings.user.OOOUser;
import com.threerings.user.OOOUserRepository;

/**
 * Whirled-specific table-compatible simulation of the parts of {@link OOOUserRepository} we want.
 */
public class MsoyOOOUserRepository extends DepotRepository
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
    public OOOUserRecord loadUser (int userId)
        throws PersistenceException
    {
        return load(OOOUserRecord.class, userId);
    }

    /**
     * Looks up a user by username.
     *
     * @return the user with the specified user id or null if no user with that id exists.
     */
    public OOOUserRecord loadUser (String username)
        throws PersistenceException
    {
        return load(OOOUserRecord.class, new Where(OOOUserRecord.USERNAME_C, username));
    }

    /**
     * Looks up a user by email address.
     *
     * @return the user with the specified address or null if no such user exists.
     */
    public OOOUserRecord loadUserByEmail (String email)
        throws PersistenceException
    {
        return load(OOOUserRecord.class, new Where(OOOUserRecord.EMAIL_C, email));
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

    /**
     * Store to the database that the passed in machIdent has been tainted by a banned player.
     */
    public void addTaintedIdent (String machIdent)
        throws PersistenceException
    {
        insert(new TaintedIdentRecord(machIdent));
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

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(OOOUserRecord.class);
        classes.add(UserIdentRecord.class);
        classes.add(TaintedIdentRecord.class);
        classes.add(HistoricalUserRecord.class);
    }
}
