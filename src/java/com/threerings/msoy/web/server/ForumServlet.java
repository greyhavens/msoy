//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Collections;
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
    public ThreadResult loadThreads (WebIdent ident, int groupId, int offset, int count,
                                     boolean needTotalCount)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser(ident);

        try {
            // make sure they have read access to this thread
            GroupRecord grec = MsoyServer.groupRepo.loadGroup(groupId);
            if (grec == null) {
                throw new ServiceException(ForumCodes.E_INVALID_GROUP);
            }
            byte groupRank = getGroupRank(mrec, groupId);
            Group group = grec.toGroupObject();
            if (!group.checkAccess(groupRank, Group.ACCESS_READ, 0)) {
                throw new ServiceException(ForumCodes.E_ACCESS_DENIED);
            }

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
            ThreadResult result = new ThreadResult();
            List<ForumThread> threads = Lists.newArrayList();
            for (ForumThreadRecord thrrec : thrrecs) {
                threads.add(thrrec.toForumThread(names));
            }
            result.threads = threads;

            // fill in this caller's new thread starting privileges
            result.canStartThread = group.checkAccess(groupRank, Group.ACCESS_THREAD, 0);

            // fill in our total thread count if needed
            if (needTotalCount) {
                result.threadCount = (threads.size() < count && offset == 0) ?
                    threads.size() : MsoyServer.forumRepo.loadThreadCount(groupId);
            }

            return result;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to load threads [for=" + who(mrec) +
                    ", gid=" + groupId + ", offset=" + offset + ", count=" + count + "].");
            throw new ServiceException(ForumCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ForumService
    public MessageResult loadMessages (WebIdent ident, int threadId, int offset, int count,
                                       boolean needTotalCount)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser(ident);

        try {
            // make sure they have read access to this thread
            ForumThreadRecord ftr = MsoyServer.forumRepo.loadThread(threadId);
            if (ftr == null) {
                throw new ServiceException(ForumCodes.E_INVALID_THREAD);
            }
            GroupRecord grec = MsoyServer.groupRepo.loadGroup(ftr.groupId);
            if (grec == null) {
                throw new ServiceException(ForumCodes.E_INVALID_GROUP);
            }
            byte groupRank = getGroupRank(mrec, ftr.groupId);
            Group group = grec.toGroupObject();
            if (!group.checkAccess(groupRank, Group.ACCESS_READ, 0)) {
                throw new ServiceException(ForumCodes.E_ACCESS_DENIED);
            }

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
            MessageResult result = new MessageResult();
            List<ForumMessage> messages = Lists.newArrayList();
            for (ForumMessageRecord msgrec : msgrecs) {
                messages.add(msgrec.toForumMessage(cards));
            }
            result.messages = messages;

            // fill in this caller's posting privileges
            result.canPostMessage = group.checkAccess(groupRank, Group.ACCESS_POST, 0);

            // fill in our total message count if needed
            if (needTotalCount) {
                result.messageCount = (messages.size() < count && offset == 0) ?
                    messages.size() : MsoyServer.forumRepo.loadMessageCount(threadId);
            }

            return result;

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

            // create the thread (and first post) in the database and return its runtime form
            return MsoyServer.forumRepo.createThread(
                groupId, mrec.memberId, flags, subject, message).toForumThread(
                    Collections.singletonMap(mrec.memberId, mrec.getName()));

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

        try {
            // make sure they're allowed to post a message to this thread's group
            ForumThreadRecord ftr = MsoyServer.forumRepo.loadThread(threadId);
            if (ftr == null) {
                throw new ServiceException(ForumCodes.E_INVALID_THREAD);
            }
            checkAccess(mrec, ftr.groupId, Group.ACCESS_POST, ftr.flags);

            // create the message in the database and return its runtime form
            ForumMessageRecord fmr = MsoyServer.forumRepo.postMessage(
                threadId, mrec.memberId, inReplyTo, message);

            // load up the member card for the poster
            IntMap<MemberCard> cards = IntMaps.newHashIntMap();
            for (MemberCardRecord mcrec : MsoyServer.memberRepo.loadMemberCards(
                     Collections.singleton(mrec.memberId))) {
                cards.put(mcrec.memberId, mcrec.toMemberCard());
            }

            // and create and return the runtime record for the post
            return fmr.toForumMessage(cards);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to post message [for=" + who(mrec) +
                    ", tid=" + threadId + ", irTo=" + inReplyTo + "].");
            throw new ServiceException(ForumCodes.E_INTERNAL_ERROR);
        }
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
            if (mrec.isAdmin()) { // admins are always treated as managers
                return GroupMembership.RANK_MANAGER;
            }
            GroupMembershipRecord grm = MsoyServer.groupRepo.getMembership(groupId, mrec.memberId);
            if (grm != null) {
                rank = grm.rank;
            }
        }
        return rank;
    }
}
