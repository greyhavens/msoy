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
import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.person.util.FeedMessageType;
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
import com.threerings.msoy.fora.server.persist.ReadTrackingRecord;

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
            Group group = getGroup(groupId);
            byte groupRank = getGroupRank(mrec, groupId);
            if (!group.checkAccess(groupRank, Group.ACCESS_READ, 0)) {
                throw new ServiceException(ForumCodes.E_ACCESS_DENIED);
            }

            // load up the requested set of threads
            List<ForumThreadRecord> thrrecs =
                MsoyServer.forumRepo.loadThreads(groupId, offset, count);

            // load up additional fiddly bits and create a result record
            ThreadResult result = toThreadResult(mrec, thrrecs);

            // fill in this caller's new thread starting privileges
            result.canStartThread = group.checkAccess(groupRank, Group.ACCESS_THREAD, 0);

            // fill in our manager status
            result.isManager = mrec.isSupport() || (groupRank == GroupMembership.RANK_MANAGER);

            // fill in our total thread count if needed
            if (needTotalCount) {
                result.threadCount = (result.threads.size() < count && offset == 0) ?
                    result.threads.size() : MsoyServer.forumRepo.loadThreadCount(groupId);
            }

            return result;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to load threads [for=" + who(mrec) +
                    ", gid=" + groupId + ", offset=" + offset + ", count=" + count + "].", pe);
            throw new ServiceException(ForumCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ForumService
    public ThreadResult loadUnreadThreads (WebIdent ident, int maximum)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        try {
            // load up said member's group memberships
            IntSet groupIds = new ArrayIntSet();
            for (GroupMembershipRecord gmr : MsoyServer.groupRepo.getMemberships(mrec.memberId)) {
                groupIds.add(gmr.groupId);
            }

            // load up the thread records
            List<ForumThreadRecord> thrrecs;
            if (groupIds.size() == 0) {
                thrrecs = Collections.emptyList();
            } else {
                thrrecs = MsoyServer.forumRepo.loadUnreadThreads(mrec.memberId, groupIds, maximum);
            }

            // load up additional fiddly bits and create a result record
            ThreadResult result = toThreadResult(mrec, thrrecs);

            // we cheat on the total count here with the idea that the client basically just asks
            // for "all" of the unread threads and we give them all as long as all is not
            // ridiculously many
            result.threadCount = result.threads.size();

            return result;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to load unread threads [for=" + who(mrec) +
                    ", max=" + maximum + "].", pe);
            throw new ServiceException(ForumCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ForumService
    public MessageResult loadMessages (WebIdent ident, int threadId, int lastReadPostId,
                                       int offset, int count, boolean needTotalCount)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser(ident);

        try {
            // make sure they have read access to this thread
            ForumThreadRecord ftr = MsoyServer.forumRepo.loadThread(threadId);
            if (ftr == null) {
                throw new ServiceException(ForumCodes.E_INVALID_THREAD);
            }
            Group group = getGroup(ftr.groupId);
            byte groupRank = getGroupRank(mrec, ftr.groupId);
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

            // convert the messages to runtime format
            MessageResult result = new MessageResult();
            List<ForumMessage> messages = Lists.newArrayList();
            int highestPostId = 0;
            for (ForumMessageRecord msgrec : msgrecs) {
                ForumMessage msg = msgrec.toForumMessage(cards);
                messages.add(msg);
                highestPostId = Math.max(highestPostId, msg.messageId);
            }
            result.messages = messages;
            result.group = group.getName();

            // fill in this caller's posting privileges
            result.canPostReply = group.checkAccess(groupRank, Group.ACCESS_POST, 0);

            // fill in our manager status
            result.isManager = mrec.isSupport() || (groupRank == GroupMembership.RANK_MANAGER);

            if (needTotalCount) {
                // convert the thread record to a runtime record if needed
                MemberCard mrpCard = cards.get(ftr.mostRecentPosterId);
                if (mrpCard == null) {
                    result.thread = ftr.toForumThread(resolveNames(Collections.singletonList(ftr)));
                } else {
                    IntMap<MemberName> names = IntMaps.newHashIntMap();
                    names.put(ftr.mostRecentPosterId, mrpCard.name);
                    result.thread = ftr.toForumThread(names);
                }
                // load up our last read post information
                if (mrec != null) {
                    for (ReadTrackingRecord rtr : MsoyServer.forumRepo.loadLastReadPostInfo(
                             mrec.memberId, Collections.singleton(threadId))) {
                        result.thread.lastReadPostId = rtr.lastReadPostId;
                        result.thread.lastReadPostIndex = rtr.lastReadPostIndex;
                        // since the client didn't have this info, we need to fill it in
                        lastReadPostId = rtr.lastReadPostId;
                    }
                }
            }

            // if this caller is authenticated, note that they've potentially updated their last
            // read message id
            if (mrec != null && highestPostId > lastReadPostId) {
                MsoyServer.forumRepo.noteLastReadPostId(    // highestPostIndex
                    mrec.memberId, threadId, highestPostId, offset + result.messages.size() - 1);
            }

            return result;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to load messages [for=" + who(mrec) +
                    ", tid=" + threadId + ", offset=" + offset + ", count=" + count + "].", pe);
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
            Group group = checkAccess(mrec, groupId, Group.ACCESS_THREAD, flags);

            // TODO: check first message contents

            // create the thread (and first post) in the database and return its runtime form
            ForumThread thread = MsoyServer.forumRepo.createThread(
                groupId, mrec.memberId, flags, subject, message).toForumThread(
                    Collections.singletonMap(mrec.memberId, mrec.getName()));

            // if the thread is an announcement thread, post a feed message about it
            if (thread.isAnnouncement()) {
                MsoyServer.feedRepo.publishGroupMessage(
                    groupId, FeedMessageType.GROUP_ANNOUNCEMENT,
                    group.name + "\t" + subject + "\t" + thread.threadId);
            }

            return thread;

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to create thread [for=" + who(mrec) +
                    ", gid=" + groupId + ", subject=" + subject + "].", pe);
            throw new ServiceException(ForumCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ForumService
    public void updateThreadFlags (WebIdent ident, int threadId, int flags)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        try {
            ForumThreadRecord ftr = MsoyServer.forumRepo.loadThread(threadId);
            if (ftr == null) {
                throw new ServiceException(ForumCodes.E_INVALID_THREAD);
            }

            // make sure they have access to both the old and new flags
            Group group = getGroup(ftr.groupId);
            byte groupRank = getGroupRank(mrec, ftr.groupId);
            if (!group.checkAccess(groupRank, Group.ACCESS_POST, ftr.flags) ||
                !group.checkAccess(groupRank, Group.ACCESS_POST, flags)) {
                throw new ServiceException(ForumCodes.E_ACCESS_DENIED);
            }

            // if we made it this far, then update the flags
            MsoyServer.forumRepo.updateThreadFlags(ftr.threadId, flags);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to update thread flags [for=" + who(mrec) +
                    ", tid=" + threadId + ", flags=" + flags + "].", pe);
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

            // TODO: check message contents

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
                    ", tid=" + threadId + ", irTo=" + inReplyTo + "].", pe);
            throw new ServiceException(ForumCodes.E_INTERNAL_ERROR);
        }
    }

    // from interface ForumService
    public void editMessage (WebIdent ident, int messageId, String message)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        try {
            // make sure they are the message author
            ForumMessageRecord fmr = MsoyServer.forumRepo.loadMessage(messageId);
            if (fmr == null) {
                throw new ServiceException(ForumCodes.E_INVALID_MESSAGE);
            }
            if (fmr.posterId != mrec.memberId) {
                throw new ServiceException(ForumCodes.E_ACCESS_DENIED);
            }

            // TODO: check message contents

            // if all is well then do the deed
            MsoyServer.forumRepo.updateMessage(messageId, message);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to edit message [for=" + who(mrec) +
                    ", mid=" + messageId + "].", pe);
            throw new ServiceException(ForumCodes.E_INTERNAL_ERROR);
        }

    }

    // from interface ForumService
    public void deleteMessage (WebIdent ident, int messageId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser(ident);

        try {
            // make sure they are the message author or a group admin or whirled support+
            ForumMessageRecord fmr = MsoyServer.forumRepo.loadMessage(messageId);
            if (fmr == null) {
                throw new ServiceException(ForumCodes.E_INVALID_MESSAGE);
            }
            if (!mrec.isSupport() && fmr.posterId != mrec.memberId) {
                ForumThreadRecord ftr = MsoyServer.forumRepo.loadThread(fmr.threadId);
                if (ftr == null) {
                    throw new ServiceException(ForumCodes.E_INVALID_THREAD);
                }
                if (getGroupRank(mrec, ftr.groupId) != GroupMembership.RANK_MANAGER) {
                    throw new ServiceException(ForumCodes.E_ACCESS_DENIED);
                }
            }

            // if all is well then do the deed
            MsoyServer.forumRepo.deleteMessage(messageId);

        } catch (PersistenceException pe) {
            log.log(Level.WARNING, "Failed to delete message [for=" + who(mrec) +
                    ", mid=" + messageId + "].", pe);
            throw new ServiceException(ForumCodes.E_INTERNAL_ERROR);
        }
    }

    /**
     * Loads the specified group, throwing an invalid group exception if it does not exist.
     */
    protected Group getGroup (int groupId)
        throws PersistenceException, ServiceException
    {
        GroupRecord grec = MsoyServer.groupRepo.loadGroup(groupId);
        if (grec == null) {
            throw new ServiceException(ForumCodes.E_INVALID_GROUP);
        }
        return grec.toGroupObject();
    }

    /**
     * Checks that the supplied member has the specified access in the specified group.
     */
    protected Group checkAccess (MemberRecord mrec, int groupId, int access, int flags)
        throws PersistenceException, ServiceException
    {
        Group group = getGroup(groupId);
        if (!group.checkAccess(getGroupRank(mrec, groupId), access, flags)) {
            throw new ServiceException(ForumCodes.E_ACCESS_DENIED);
        }
        return group;
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

    /**
     * Converts a list of threads to a {@link ThreadResult}, looking up the last poster names and
     * filling in other bits.
     */
    protected ThreadResult toThreadResult (MemberRecord mrec, List<ForumThreadRecord> thrrecs)
        throws PersistenceException
    {
        // enumerate the last-posters and create member names for them
        IntMap<MemberName> names = resolveNames(thrrecs);

        // finally convert the threads to runtime format and return them
        ThreadResult result = new ThreadResult();
        IntMap<ForumThread> thrmap = IntMaps.newHashIntMap();
        List<ForumThread> threads = Lists.newArrayList();
        for (ForumThreadRecord thrrec : thrrecs) {
            ForumThread ftr = thrrec.toForumThread(names);
            thrmap.put(ftr.threadId, ftr);
            threads.add(ftr);
        }
        result.threads = threads;

        // fill in the last read post information if the member is logged in
        if (mrec != null) {
            IntSet threadIds = new ArrayIntSet();
            for (ForumThreadRecord thrrec : thrrecs) {
                threadIds.add(thrrec.threadId);
            }
            if (threadIds.size() > 0) {
                for (ReadTrackingRecord rtr :
                         MsoyServer.forumRepo.loadLastReadPostInfo(mrec.memberId, threadIds)) {
                    ForumThread ftr = thrmap.get(rtr.threadId);
                    if (ftr != null) { // shouldn't be null but let's not have a cow
                        ftr.lastReadPostId = rtr.lastReadPostId;
                        ftr.lastReadPostIndex = rtr.lastReadPostIndex;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Resolves the names of the posters of the supplied threads.
     */
    protected IntMap<MemberName> resolveNames (List<ForumThreadRecord> thrrecs)
        throws PersistenceException
    {
        IntSet posters = new ArrayIntSet();
        for (ForumThreadRecord thrrec : thrrecs) {
            posters.add(thrrec.mostRecentPosterId);
        }
        IntMap<MemberName> names = IntMaps.newHashIntMap();
        if (posters.size() > 0) {
            for (MemberNameRecord mnrec : MsoyServer.memberRepo.loadMemberNames(posters)) {
                names.put(mnrec.memberId, mnrec.toMemberName());
            }
        }
        return names;
    }
}
