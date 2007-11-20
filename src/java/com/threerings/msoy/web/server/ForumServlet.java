//
// $Id$

package com.threerings.msoy.web.server;

import java.util.List;
import java.util.logging.Level;

import com.google.common.collect.Lists;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberNameRecord;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.group.data.Group;
import com.threerings.msoy.group.data.GroupMembership;
import com.threerings.msoy.group.server.persist.GroupMembershipRecord;
import com.threerings.msoy.group.server.persist.GroupRecord;

import com.threerings.msoy.fora.data.ForumCodes;
import com.threerings.msoy.fora.data.ForumMessage;
import com.threerings.msoy.fora.data.ForumThread;
import com.threerings.msoy.fora.server.persist.ForumMessageRecord;
import com.threerings.msoy.fora.server.persist.ForumThreadRecord;

import com.threerings.msoy.web.client.ForumService;
import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link ForumService}.
 */
public class ForumServlet extends MsoyServiceServlet
    implements ForumService
{
    // from interface ForumService
    public List loadThreads (WebIdent ident, int groupId, int offset, int count)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser(ident);

        try {
            // make sure they have read access to this thread
            checkAccess(mrec, groupId, Group.ACCESS_READ, 0);

            // load up the requested set of threads
            List<ForumThreadRecord> thrrecs =
                MsoyServer.forumRepo.loadThreads(groupId, offset, count);

            // enumerate the last-posters and create member names for them
            IntMap<MemberName> names = IntMaps.newHashIntMap();
            IntSet posters = new ArrayIntSet();
            for (ForumThreadRecord thrrec : thrrecs) {
                posters.add(thrrec.mostRecentPosterId);
            }
            for (MemberNameRecord mnrec : MsoyServer.memberRepo.loadMemberNames(posters)) {
                names.put(mnrec.memberId, mnrec.toMemberName());
            }

            // finally convert the threads to runtime format and return them
            List<ForumThread> threads = Lists.newArrayList();
            for (ForumThreadRecord thrrec : thrrecs) {
                threads.add(thrrec.toForumThread(names));
            }
            return threads;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to load threads [for=" + who(mrec) +
                    ", gid=" + groupId + ", offset=" + offset + ", count=" + count + "].");
            throw new ServiceException(ForumCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ForumService
    public List loadMessages (WebIdent ident, int threadId, int offset, int count)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser(ident);

        try {
            // make sure they have read access to this thread
            ForumThreadRecord ftr = MsoyServer.forumRepo.loadThread(threadId);
            if (ftr == null) {
                throw new ServiceException(ForumCodes.E_INVALID_THREAD);
            }
            checkAccess(mrec, ftr.groupId, Group.ACCESS_READ, 0);

            // load up the requested set of messages
            List<ForumMessageRecord> msgrecs =
                MsoyServer.forumRepo.loadMessages(threadId, offset, count);

            // enumerate the posters and create member cards for them
            IntMap<MemberCard> cards = IntMaps.newHashIntMap();
            IntSet posters = new ArrayIntSet();
            for (ForumMessageRecord msgrec : msgrecs) {
                posters.add(msgrec.posterId);
            }
            for (MemberCardRecord mcrec : MsoyServer.memberRepo.loadMemberCards(posters)) {
                cards.put(mcrec.memberId, mcrec.toMemberCard());
            }

            // finally convert the messages to runtime format and return them
            List<ForumMessage> messages = Lists.newArrayList();
            for (ForumMessageRecord msgrec : msgrecs) {
                messages.add(msgrec.toForumMessage(cards));
            }

            return messages;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to load messages [for=" + who(mrec) +
                    ", tid=" + threadId + ", offset=" + offset + ", count=" + count + "].");
            throw new ServiceException(ForumCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ForumService
    public ForumThread createThread (WebIdent ident, int groupId, int flags,
                                     String subject, String message)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        try {
            // make sure they're allowed to create a thread in this group
            checkAccess(mrec, groupId, Group.ACCESS_THREAD, flags);

            return null;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to create thread [for=" + who(mrec) +
                    ", gid=" + groupId + ", subject=" + subject + "].");
            throw new ServiceException(ForumCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ForumService
    public ForumMessage postMessage (WebIdent ident, int threadId, int inReplyTo, String message)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        return null;
    }

    // from interface ForumService
    public void editMessage (WebIdent ident, int messageId, String message)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

    }

    // from interface ForumService
    public void deleteMessage (WebIdent ident, int messageId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

    }

    /**
     * Checks that the supplied member has the specified access in the specified group.
     */
    protected void checkAccess (MemberRecord mrec, int groupId, int access, int flags)
        throws PersistenceException, ServiceException
    {
        GroupRecord grec = MsoyServer.groupRepo.loadGroup(groupId);
        if (grec == null) {
            throw new ServiceException(ForumCodes.E_INVALID_GROUP);
        }
        if (!grec.toGroupObject().checkAccess(getGroupRank(mrec, groupId), access, flags)) {
            throw new ServiceException(ForumCodes.E_ACCESS_DENIED);
        }
    }

    /**
     * Determines the rank of the supplied member in the specified group. If the member is null or
     * not a member of the specified group, rank-non-member will be returned.
     */
    protected byte getGroupRank (MemberRecord mrec, int groupId)
        throws PersistenceException
    {
        byte rank = GroupMembership.RANK_NON_MEMBER;
        if (mrec != null) {
            GroupMembershipRecord grm = MsoyServer.groupRepo.getMembership(groupId, mrec.memberId);
            if (grm != null) {
                rank = grm.rank;
            }
        }
        return rank;
    }
}
