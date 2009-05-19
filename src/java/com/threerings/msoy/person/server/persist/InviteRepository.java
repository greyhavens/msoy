//
// $Id$

package com.threerings.msoy.person.server.persist;

import java.sql.Timestamp;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.operator.Conditionals.In;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.server.persist.MemberRecord;

import static com.threerings.msoy.Log.log;

/**
 * Handles invitation related bits.
 */
@Singleton @BlockingThread
public class InviteRepository extends DepotRepository
{
    @Inject public InviteRepository (PersistenceContext ctx)
    {
        super(ctx);
    }

    /**
     * Add a new invitation. Also decrements the available invitation count for the inviterId and
     * increments the number of invites sent, iff the inviterId is non-zero.
     */
    public void addInvite (String inviteeEmail, int inviterId, String inviteId)
    {
        insert(new InvitationRecord(inviteeEmail, inviterId, inviteId));

        if (inviterId > 0) {
            InviterRecord inviterRec = load(InviterRecord.class, inviterId);
            if (inviterRec == null) {
                inviterRec = new InviterRecord();
                inviterRec.memberId = inviterId;
            }
            inviterRec.invitesSent++;
            store(inviterRec);
        }
    }

    /**
     * Check if the invitation is available for use, or has been claimed already. Returns null if
     * it has already been claimed, an invite record if not.
     */
    public InvitationRecord inviteAvailable (String inviteId)
    {
        InvitationRecord rec = load(
            InvitationRecord.class, new Where(InvitationRecord.INVITE_ID, inviteId));
        return (rec == null || rec.inviteeId != 0) ? null : rec;
    }

    /**
     * Update the invitation indicated with the new memberId.
     */
    public void linkInvite (String inviteId, MemberRecord member)
    {
        InvitationRecord invRec = load(InvitationRecord.class, inviteId);
        invRec.inviteeId = member.memberId;
        update(invRec, InvitationRecord.INVITEE_ID);
    }

    /**
     * Get a list of the invites that this user has already sent out that have not yet been
     * accepted.
     */
    public List<InvitationRecord> loadPendingInvites (int memberId)
    {
        return findAll(
            InvitationRecord.class,
            new Where(InvitationRecord.INVITER_ID, memberId,
                      InvitationRecord.INVITEE_ID, 0));
    }

    /**
     * Return the InvitationRecord that corresponds to the given unique code.
     */
    public InvitationRecord loadInvite (String inviteId, boolean markViewed)
    {
        InvitationRecord invRec = load(InvitationRecord.class, inviteId);
        if (invRec != null && invRec.viewed == null) {
            invRec.viewed = new Timestamp((new java.util.Date()).getTime());
            update(invRec, InvitationRecord.VIEWED);
        }
        return invRec;
    }

    /**
     * Return the InvitationRecord that corresponds to the given inviter
     */
    public InvitationRecord loadInvite (String inviteeEmail, int inviterId)
    {
        // TODO: This does a row scan on email after using ixInviter. Should be OK, but let's check.
        return load(InvitationRecord.class, new Where(
            InvitationRecord.INVITEE_EMAIL, inviteeEmail,
            InvitationRecord.INVITER_ID, inviterId));
    }

    /**
     * Generates a new unique invitation id.
     */
    public String generateInviteId ()
    {
        return _inviteIdGen.generate();
    }

    /**
     * Gets a new unique random id for use with game invitations.
     */
    public String generateGameInviteId ()
    {
        return _gameInviteIdGen.generate();
    }

    /**
     * Adds a new game invite sent to the given user with the given id.
     */
    public void addGameInvite (String inviteeEmail, String inviteId)
    {
        GameInvitationRecord invite = new GameInvitationRecord();
        invite.inviteeEmail = inviteeEmail;
        invite.inviteId = inviteId;
        insert(invite);
    }

    /**
     * Returns the game invitation record that corresponds to the given id.
     */
    public GameInvitationRecord loadGameInvite (String inviteId)
    {
        return load(GameInvitationRecord.class,
            new Where(GameInvitationRecord.INVITE_ID, inviteId));
    }

    /**
     * Returns the game invitation record that corresponds to the given invitee.
     */
    public GameInvitationRecord loadGameInviteByEmail (String inviteeEmail)
    {
        return load(GameInvitationRecord.class, inviteeEmail);
    }

    /**
     * Deletes all data associated with the supplied member. This is done as a part of purging a
     * member's account.
     */
    public void purgeMembers (Collection<Integer> memberIds)
    {
        deleteAll(InviterRecord.class, new Where(new In(InviterRecord.MEMBER_ID, memberIds)));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(InvitationRecord.class);
        classes.add(InviterRecord.class);
        classes.add(GameInvitationRecord.class);
    }

    protected static abstract class InviteIDGen
    {
        public InviteIDGen (String name) {
            _name = name;
        }

        public String generate () {
            // find a free invite id
            String inviteId;
            int tries = 0;
            while (exists(inviteId = randomId())) {
                tries++;
            }
            if (tries > 5) {
                log.warning(_name + " inviteId space is getting saturated", "tries", tries);
            }
            return inviteId;
        }

        protected abstract boolean exists (String id);

        protected static String randomId () {
            String rand = "";
            for (int ii = 0; ii < ID_LENGTH; ii++) {
                rand += ID_CHARACTERS.charAt((int)(Math.random() * ID_CHARACTERS.length()));
            }
            return rand;
        }

        protected String _name;

        protected static final int ID_LENGTH = 10;
        protected static final String ID_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ01234567890";
    }

    /** Generator for normal invitations. */
    protected InviteIDGen _inviteIdGen = new InviteIDGen("InvitationRecord") {
        @Override protected boolean exists (String id) {
            return loadInvite(id, false) != null;
        }
    };

    /** Generator for normal game invitations. */
    protected InviteIDGen _gameInviteIdGen = new InviteIDGen("GameInvitationRecord") {
        @Override protected boolean exists (String id) {
            return loadGameInvite(id) != null;
        }
    };
}
