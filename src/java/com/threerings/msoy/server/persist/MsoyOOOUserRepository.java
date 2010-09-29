//
// $Id$

package com.threerings.msoy.server.persist;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.Ops;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.expression.SQLExpression;
import com.samskivert.depot.clause.Join;
import com.samskivert.depot.clause.Where;

import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.threerings.user.OOOUser;
import com.threerings.user.depot.DepotUserRepository;
import com.threerings.user.depot.OOOUserRecord;
import com.threerings.user.depot.UserIdentRecord;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.underwire.server.persist.SupportRepository;

import com.threerings.msoy.data.all.MemberMailUtil;

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
        delete(OOOUserRecord.getKey(userId));
        deleteAll(UserIdentRecord.class, new Where(UserIdentRecord.USER_ID, userId));
    }

    /**
     * Deletes all data associated with the supplied members (except the OOOUserRecord which is
     * only disabled). This is done as a part of purging member accounts.
     */
    public void purgeMembers (Collection<String> emails)
    {
        // map these emails to user ids
        Map<Integer, String> idmap = Maps.newHashMap();
        Set<Integer> guestIds = Sets.newHashSet();
        for (OOOUserRecord record : findAll(OOOUserRecord.class,
                                            new Where(OOOUserRecord.EMAIL.in(emails)))) {
            idmap.put(record.userId, record.email);
            // track permaguest ids separately
            if (MemberMailUtil.isPermaguest(record.email)) {
                guestIds.add(record.userId);
            }
        }

        // delete ident mapping records for permaguests but leave them for members
        if (!guestIds.isEmpty()) {
            deleteAll(UserIdentRecord.class, new Where(UserIdentRecord.USER_ID.in(guestIds)));
        }

        // change the username and email to deleted thunks and null out their password
        // TODO: should we just delete the permaguest user records?
        for (Map.Entry<Integer, String> entry : idmap.entrySet()) {
            // change foo@bar.com to deleted:id:foo:bar.com
            String thunk = StringUtil.truncate(
                "deleted:"+ entry.getKey() + ":" + entry.getValue().replace("@", ":"), 128);
            updatePartial(OOOUserRecord.getKey(entry.getKey()),
                          OOOUserRecord.USERNAME, thunk,
                          OOOUserRecord.EMAIL, thunk,
                          OOOUserRecord.PASSWORD, "");
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
        SQLExpression joinCondition = Ops.and(
                MemberRecord.MEMBER_ID.eq(memberId),
                OOOUserRecord.EMAIL.eq(MemberRecord.ACCOUNT_NAME));
        return toUser(load(OOOUserRecord.class, new Join(MemberRecord.class, joinCondition)));
    }

    @Override // documentation inherited
    public List<Tuple<Integer, String>> getUsersOfMachIdent (String machIdent)
    {
        // We need to return the email as our "username" for underwire
        List<Tuple<Integer,String>> users = Lists.newArrayList();
        Join join = new Join(UserIdentRecord.class, Ops.and(
                                 OOOUserRecord.USER_ID.eq(UserIdentRecord.USER_ID),
                                 UserIdentRecord.MACH_IDENT.eq(machIdent)));
        for (OOOUserRecord record : findAll(OOOUserRecord.class, join)) {
            users.add(new Tuple<Integer,String>(record.userId, record.email));
        }
        return users;
    }

    @Override // documentation inherited
    public List<Tuple<Integer, String>> getUsersOfMachIdents (String[] idents)
    {
        // We need to return the email as our "username" for underwire
        List<Tuple<Integer,String>> users = Lists.newArrayList();
        Join join = new Join(UserIdentRecord.class, Ops.and(
                                 OOOUserRecord.USER_ID.eq(UserIdentRecord.USER_ID),
                                 UserIdentRecord.MACH_IDENT.in(Arrays.asList(idents))));
        for (OOOUserRecord record : findAll(OOOUserRecord.class, join)) {
            users.add(new Tuple<Integer,String>(record.userId, record.email));
        }
        return users;
    }

    @Override // documentation inherited
    public String[] getUsernames (String email)
    {
        // We need to return the email as our "username" for underwire
        List<String> usernames = Lists.newArrayList();
        Where where = new Where(OOOUserRecord.EMAIL, email);
        for (OOOUserRecord record : findAll(OOOUserRecord.class, where)) {
            usernames.add(record.email);
        }
        return usernames.toArray(new String[usernames.size()]);
    }
}
