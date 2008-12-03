//
// $Id$

package com.threerings.msoy.fora.server;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.SceneRecord;
import com.threerings.msoy.server.MsoyEventLogger;
import com.threerings.msoy.server.ServerConfig;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.util.HTMLSanitizer;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.MemberCard;
import com.threerings.msoy.web.gwt.MessageUtil;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.GameRecord;
import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;

import com.threerings.msoy.game.server.persist.MsoyGameRepository;
import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.group.server.persist.GroupMembershipRecord;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.mail.server.MailLogic;
import com.threerings.msoy.person.server.persist.FeedRepository;
import com.threerings.msoy.person.util.FeedMessageType;
import com.threerings.msoy.underwire.server.SupportLogic;

import com.threerings.msoy.fora.gwt.ForumCodes;
import com.threerings.msoy.fora.gwt.ForumMessage;
import com.threerings.msoy.fora.gwt.ForumService;
import com.threerings.msoy.fora.gwt.ForumThread;
import com.threerings.msoy.fora.gwt.MessageTooLongException;
import com.threerings.msoy.fora.server.persist.ForumMessageRecord;
import com.threerings.msoy.fora.server.persist.ForumRepository;
import com.threerings.msoy.fora.server.persist.ForumThreadRecord;
import com.threerings.msoy.fora.server.persist.ReadTrackingRecord;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link ForumService}.
 */
public class ForumServlet extends MsoyServiceServlet
    implements ForumService
{
    // from interface ForumService
    public ThreadResult loadUnreadThreads (int maximum)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        // load up said member's group memberships
        Map<Integer, GroupName> groups = Maps.newHashMap();
        for (GroupCard card : _groupRepo.getMemberGroups(mrec.memberId, true)) {
            groups.put(card.name.getGroupId(), card.name);
        }

        // load up the thread records
        List<ForumThreadRecord> thrrecs;
        if (groups.size() == 0) {
            thrrecs = Collections.emptyList();
        } else {
            thrrecs = _forumRepo.loadUnreadThreads(mrec.memberId, groups.keySet(), maximum);
        }

        // load up additional fiddly bits and create a result record
        ThreadResult result = new ThreadResult();
        result.threads = _forumLogic.resolveThreads(mrec, thrrecs, groups, true, false);

        // we cheat on the total count here with the idea that the client basically just asks
        // for "all" of the unread threads and we give them all as long as all is not
        // ridiculously many
        result.threadCount = result.threads.size();

        return result;
    }

    // from interface ForumService
    public ThreadResult loadThreads (int groupId, int offset, int count, boolean needTotalCount)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();

        // make sure they have read access to this group
        Group group = getGroup(groupId);
        byte groupRank = getGroupRank(mrec, groupId);
        if (!group.checkAccess(groupRank, Group.ACCESS_READ, 0)) {
            throw new ServiceException(ForumCodes.E_ACCESS_DENIED);
        }

        // load up the requested set of threads
        List<ForumThreadRecord> thrrecs = _forumRepo.loadThreads(groupId, offset, count);

        // load up additional fiddly bits and create a result record
        ThreadResult result = new ThreadResult();
        Map<Integer,GroupName> gmap = Collections.singletonMap(group.groupId, group.getName());
        result.threads = _forumLogic.resolveThreads(mrec, thrrecs, gmap, true, false);

        // fill in this caller's new thread starting privileges
        result.canStartThread = (mrec != null) &&
            group.checkAccess(groupRank, Group.ACCESS_THREAD, 0);

        // fill in our manager and announce status
        result.isManager = (mrec != null && mrec.isSupport()) ||
            (groupRank == GroupMembership.RANK_MANAGER);
        result.isAnnounce = (groupId == ServerConfig.getAnnounceGroupId());

        // fill in our total thread count if needed
        if (needTotalCount) {
            result.threadCount = (result.threads.size() < count && offset == 0) ?
                result.threads.size() : _forumRepo.loadThreadCount(groupId);
        }

        return result;
    }

    // from interface ForumService
    public List<ForumThread> findThreads (int groupId, String search, int limit)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();

        // make sure they have read access to this group
        Group group = getGroup(groupId);
        if (!group.checkAccess(getGroupRank(mrec, groupId), Group.ACCESS_READ, 0)) {
            throw new ServiceException(ForumCodes.E_ACCESS_DENIED);
        }

        Map<Integer,GroupName> gmap = Collections.singletonMap(group.groupId, group.getName());
        return _forumLogic.resolveThreads(
            mrec, _forumRepo.findThreads(groupId, search, limit), gmap, true, false);
    }

    // from interface ForumService
    public MessageResult loadMessages (int threadId, int lastReadPostId, int offset, int count,
                                       boolean needTotalCount)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();

        // make sure they have read access to this thread
        ForumThreadRecord ftr = _forumRepo.loadThread(threadId);
        if (ftr == null) {
            throw new ServiceException(ForumCodes.E_INVALID_THREAD);
        }
        Group group = getGroup(ftr.groupId);
        byte rank = getGroupRank(mrec, ftr.groupId);
        if (!group.checkAccess(rank, Group.ACCESS_READ, 0)) {
            throw new ServiceException(ForumCodes.E_ACCESS_DENIED);
        }

        MessageResult result = new MessageResult();

        // fill in this caller's posting privileges and manager status
        result.canPostReply = (mrec != null) && group.checkAccess(rank, Group.ACCESS_POST, 0);
        result.isManager = (mrec != null && mrec.isSupport()) ||
            (rank == GroupMembership.RANK_MANAGER);

        // load up the messages, convert to runtime records, compute highest post id
        List<ForumMessage> messages = resolveMessages(
            _forumRepo.loadMessages(threadId, offset, count));
        int highestPostId = 0;
        for (ForumMessage msg : messages) {
            highestPostId = Math.max(highestPostId, msg.messageId);
        }
        result.messages = messages;

        if (needTotalCount) {
            // convert the thread record to a runtime record if needed
            result.thread = ftr.toForumThread(
                Collections.singletonMap(ftr.mostRecentPosterId,
                                         // we don't need their display name here
                                         new MemberName("", ftr.mostRecentPosterId)),
                Collections.singletonMap(group.groupId, group.getName()));

            // load up our last read post information
            if (mrec != null) {
                for (ReadTrackingRecord rtr : _forumRepo.loadLastReadPostInfo(
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
            _forumRepo.noteLastReadPostId(              // highestPostIndex
                mrec.memberId, threadId, highestPostId, offset + result.messages.size() - 1);
        }

        // log this event for metrics purposes
        final int memberId = (mrec != null) ? mrec.memberId : MsoyEventLogger.UNKNOWN_MEMBER_ID;
        final String tracker = (mrec != null) ? mrec.visitorId : getVisitorTracker();
        _eventLog.forumMessageRead(memberId, tracker);

        return result;
    }

    // from interface ForumService
    public List<ForumMessage> findMessages (int threadId, String search, int limit)
        throws ServiceException
    {
        MemberRecord mrec = getAuthedUser();

        // make sure they have read access to this thread
        ForumThreadRecord ftr = _forumRepo.loadThread(threadId);
        if (ftr == null) {
            throw new ServiceException(ForumCodes.E_INVALID_THREAD);
        }
        Group group = getGroup(ftr.groupId);
        byte rank = getGroupRank(mrec, ftr.groupId);
        if (!group.checkAccess(rank, Group.ACCESS_READ, 0)) {
            throw new ServiceException(ForumCodes.E_ACCESS_DENIED);
        }

        // do the search and return the results
        return resolveMessages(_forumRepo.findMessages(threadId, search, limit));
    }

    // from interface ForumService
    public ForumThread createThread (int groupId, int flags, boolean spam,
                                     String subject, String message)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        // make sure they're allowed to create a thread in this group
        Group group = checkAccess(mrec, groupId, Group.ACCESS_THREAD, flags);

        // sanity check our spam argument (the client should prevent this)
        if (spam && (groupId != ServerConfig.getAnnounceGroupId() || !mrec.isSupport())) {
            throw new ServiceException(ForumCodes.E_ACCESS_DENIED);
        }

        // sanitize this message's HTML, expand Whirled URLs and do length checking
        message = processMessage(message);

        // create the thread (and first post) in the database and return its runtime form
        ForumThread thread = _forumRepo.createThread(
            groupId, mrec.memberId, flags, subject, message).toForumThread(
                Collections.singletonMap(mrec.memberId, mrec.getName()),
                Collections.singletonMap(group.groupId, group.getName()));

        // log this event for metrics purposes
        _eventLog.forumMessagePosted(mrec.memberId, mrec.visitorId, thread.threadId, thread.posts);

        // mark this thread as read by the poster
        _forumRepo.noteLastReadPostId(
            mrec.memberId, thread.threadId, thread.mostRecentPostId, 1);

        // if we're posting to the announcement group, add a global feed post about it
        if (groupId == ServerConfig.getAnnounceGroupId()) {
            _feedRepo.publishGlobalMessage(
                FeedMessageType.GLOBAL_ANNOUNCEMENT, subject + "\t" + thread.threadId);

            // spam our players with this message if requested
            if (spam) {
                log.info("Spamming players with forum post", "for", mrec.who(), "subject", subject);
                _mailLogic.spamPlayers(subject, MessageUtil.expandMessage(message));
            }

        // otherwise, if the thread is an announcement thread, post a feed message about it
        } else if (thread.isAnnouncement()) {
            _feedRepo.publishGroupMessage(
                groupId, FeedMessageType.GROUP_ANNOUNCEMENT,
                group.name + "\t" + subject + "\t" + thread.threadId + "\t" 
                    + MediaDesc.mdToString(group.getLogo()));
        }

        return thread;
    }

    // from interface ForumService
    public void updateThreadFlags (int threadId, int flags)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        ForumThreadRecord ftr = _forumRepo.loadThread(threadId);
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
        _forumRepo.updateThreadFlags(ftr.threadId, flags);
    }

    // from interface ForumService
    public void ignoreThread (int threadId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        _forumRepo.noteLastReadPostId(mrec.memberId, threadId, Integer.MAX_VALUE, 0);
    }

    // from interface ForumService
    public ForumMessage postMessage (int threadId, int inReplyTo, String message)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        // make sure they're allowed to post a message to this thread's group
        ForumThreadRecord ftr = _forumRepo.loadThread(threadId);
        if (ftr == null) {
            throw new ServiceException(ForumCodes.E_INVALID_THREAD);
        }
        checkAccess(mrec, ftr.groupId, Group.ACCESS_POST, ftr.flags);

        // sanitize this message's HTML, expand Whirled URLs and do length checking
        message = processMessage(message);

        // create the message in the database and return its runtime form
        ForumMessageRecord fmr = _forumRepo.postMessage(
            ftr, mrec.memberId, inReplyTo, message);

        // log event for metrics purposes
        _eventLog.forumMessagePosted(mrec.memberId, mrec.visitorId, fmr.threadId, ftr.posts);

        // mark this thread as read by the poster
        _forumRepo.noteLastReadPostId(mrec.memberId, ftr.threadId, fmr.messageId, ftr.posts+1);

        // and create and return the runtime record for the post
        return fmr.toForumMessage(getCardsMap(mrec.memberId));
    }

    // from interface ForumService
    public ForumMessage editMessage (int messageId, String message)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        // make sure they are the message author
        ForumMessageRecord fmr = _forumRepo.loadMessage(messageId);
        if (fmr == null) {
            throw new ServiceException(ForumCodes.E_INVALID_MESSAGE);
        }
        if (!mrec.isSupport() && fmr.posterId != mrec.memberId) {
            throw new ServiceException(ForumCodes.E_ACCESS_DENIED);
        }

        // sanitize this message's HTML, expand Whirled URLs and do length checking
        message = processMessage(message);

        // if all is well then do the deed
        fmr.lastEdited = _forumRepo.updateMessage(messageId, message);

        // update the message record and create and return the runtime record for the post
        fmr.message = message;
        return fmr.toForumMessage(getCardsMap(mrec.memberId));
    }

    // from interface ForumService
    public void deleteMessage (int messageId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        // make sure they are the message author or a group admin or whirled support+
        ForumMessageRecord fmr = _forumRepo.loadMessage(messageId);
        if (fmr == null) {
            throw new ServiceException(ForumCodes.E_INVALID_MESSAGE);
        }
        if (!mrec.isSupport() && fmr.posterId != mrec.memberId) {
            ForumThreadRecord ftr = _forumRepo.loadThread(fmr.threadId);
            if (ftr == null) {
                throw new ServiceException(ForumCodes.E_INVALID_THREAD);
            }
            if (getGroupRank(mrec, ftr.groupId) != GroupMembership.RANK_MANAGER) {
                throw new ServiceException(ForumCodes.E_ACCESS_DENIED);
            }
        }

        // if all is well then do the deed
        _forumRepo.deleteMessage(messageId);
    }

    // from interface ForumService
    public void complainMessage (String complaint, int messageId)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();

        // load up the message details
        ForumMessageRecord fmr = _forumRepo.loadMessage(messageId);
        if (fmr == null) {
            throw new ServiceException(ForumCodes.E_INVALID_MESSAGE);
        }
        _supportLogic.addMessageComplaint(
            mrec.getName(), fmr.posterId, fmr.message, complaint,
            ServerConfig.getServerURL() + "#whirleds-t_" + fmr.threadId);
    }

    // from interface ForumService
    public void sendPreviewEmail (String subject, String message, boolean includeProbeList)
        throws ServiceException
    {
        MemberRecord mrec = requireSupportUser();
        // first do our URL expansions
        message = processMessage(message);
        // and then actually format those expansions
        message = MessageUtil.expandMessage(message);
        // and jam the whole thing into an email
        _mailLogic.previewSpam(mrec.memberId, mrec.accountName, subject, message, includeProbeList);
    }

    protected List<ForumMessage> resolveMessages (List<ForumMessageRecord> msgrecs)
    {
        // enumerate the posters and create member cards for them
        IntMap<MemberCard> cards = IntMaps.newHashIntMap();
        IntSet posters = new ArrayIntSet();
        for (ForumMessageRecord msgrec : msgrecs) {
            posters.add(msgrec.posterId);
        }
        for (MemberCardRecord mcrec : _memberRepo.loadMemberCards(posters)) {
            cards.put(mcrec.memberId, mcrec.toMemberCard());
        }

        // convert the messages to runtime format
        List<ForumMessage> messages = Lists.newArrayList();
        for (ForumMessageRecord msgrec : msgrecs) {
            messages.add(msgrec.toForumMessage(cards));
        }
        return messages;
    }

    /**
     * Loads the specified group, throwing an invalid group exception if it does not exist.
     */
    protected Group getGroup (int groupId)
        throws ServiceException
    {
        GroupRecord grec = _groupRepo.loadGroup(groupId);
        if (grec == null) {
            throw new ServiceException(ForumCodes.E_INVALID_GROUP);
        }
        return grec.toGroupObject();
    }

    /**
     * Checks that the supplied member has the specified access in the specified group.
     */
    protected Group checkAccess (MemberRecord mrec, int groupId, int access, int flags)
        throws ServiceException
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
    {
        byte rank = GroupMembership.RANK_NON_MEMBER;
        if (mrec != null) {
            if (mrec.isSupport()) { // support+ is always treated as managers
                return GroupMembership.RANK_MANAGER;
            }
            GroupMembershipRecord grm = _groupRepo.getMembership(groupId, mrec.memberId);
            if (grm != null) {
                rank = grm.rank;
            }
        }
        return rank;
    }

    /**
     * Helper function for getting a map of member cards for posting and editing a message.
     */
    protected IntMap<MemberCard> getCardsMap (int memberId)
    {
        IntMap<MemberCard> cards = IntMaps.newHashIntMap();
        for (MemberCardRecord mrec : _memberRepo.loadMemberCards(Collections.singleton(memberId))) {
            cards.put(mrec.memberId, mrec.toMemberCard());
        }
        return cards;
    }

    /**
     * Runs the supplied HTML message through our sanitizer, expands Whirled URLs (looking up
     * metadata for items, whirleds and games) and finally rechecks that the length of the message
     * is within our limits. The sanitizer and/or URL expansion might make the message slightly
     * longer.
     */
    protected String processMessage (String message)
        throws ServiceException
    {
        // freak out immediately if we're too long before we even sanitize
        if (message.length() > HTMLSanitizer.MAX_PRE_SANITIZE_LENGTH) {
            throw new MessageTooLongException(message.length());
        }

        // run the HTML through our sanitizer
        message = HTMLSanitizer.sanitize(message);

        // now look for URL expansions
        StringBuffer expbuf = new StringBuffer();
        Matcher m = _urlPattern.matcher(message);
        while (m.find()) {
            m.appendReplacement(expbuf, convertToken(m.group(2), m.group()));
        }
        m.appendTail(expbuf);
        message = expbuf.toString();

        if (message.length() > ForumMessage.MAX_MESSAGE_LENGTH) {
            throw new MessageTooLongException(message.length());
        }
        return message;
    }

    protected String convertToken (String token, String original)
    {
        int didx = token.indexOf("-");
        if (didx == -1) {
            return original; // hrm, bogosity
        }

        Pages page;
        try {
            page = Enum.valueOf(Pages.class, token.substring(0, didx).toUpperCase());
        } catch (Exception e) {
            return original; // hrm, bogosity
        }
        Args args = Args.fromToken(token.substring(didx+1));

        try {
            switch (page) {
            case SHOP:
                // handle shop listings
                if (args.get(0, "").equals("l")) {
                    return makeBoxedItem(token, (byte)args.get(1, 0), args.get(2, 0));
                }
                break;
            case GAMES:
                // handle game detail page links
                if (args.get(0, "").equals("d")) {
                    return makeBoxedGame(token, args.get(1, 0));
                }
                break;
            case GROUPS:
                // handle whirled detail page links
                if (args.get(0, "").equals("d")) {
                    return makeBoxedWhirled(token, args.get(1, 0));
                }
                break;
            case WORLD:
                // handle scene links
                if (args.isPrefixedId(0, "s")) {
                    return makeBoxedScene(token, args.getPrefixedId(0, "s", 0));

                } else if (args.isPrefixedId(0, "m")) {
                    return makeBoxedMemberHome(token, args.getPrefixedId(0, "m", 0));
                }
                break;
            }
        } catch (Exception e) {
            log.warning("Failed to box page", "token", token, e);
        }

        return original; // hrm, bogosity
        // return "_URL_" + token;
    }

    protected String makeBoxedItem (String token, byte type, int catalogId)
        throws ServiceException
    {
        ItemRepository<ItemRecord> repo = _itemLogic.getRepository(type);
        CatalogRecord crec = repo.loadListing(catalogId, true);
        Item item = (crec == null) ? null : crec.item.toItem();
        return (item == null) ? null : MessageUtil.makeBox(token, item.getThumbnailMedia(),
                                                           MediaDesc.THUMBNAIL_SIZE, item.name);
    }

    protected String makeBoxedGame (String token, int gameId)
    {
        GameRecord grec = _gameRepo.loadGameRecord(gameId);
        return (grec == null) ? null :
            MessageUtil.makeBox(token, ((Game)grec.toItem()).getShotMedia(),
                                MediaDesc.GAME_SHOT_SIZE, grec.name);
    }

    protected String makeBoxedWhirled (String token, int groupId)
    {
        GroupRecord grec = _groupRepo.loadGroup(groupId);
        return (grec == null) ? null :
            MessageUtil.makeBox(token, grec.toLogo(), MediaDesc.THUMBNAIL_SIZE, grec.name);
    }

    protected String makeBoxedScene (String token, int sceneId)
    {
        SceneRecord srec = _sceneRepo.loadScene(sceneId);
        return (srec == null) ? null : MessageUtil.makeBox(
            token, srec.getThumbnail(), MediaDesc.SNAPSHOT_THUMB_SIZE, srec.name);
    }

    protected String makeBoxedMemberHome (String token, int memberId)
    {
        MemberRecord memrec = _memberRepo.loadMember(memberId);
        return (memrec == null) ? null : makeBoxedScene(token, memrec.homeSceneId);
    }

    protected Pattern _urlPattern = Pattern.compile(
        "(" + Pattern.quote(ServerConfig.getServerURL()) + "#)([-a-z0-9_]+)(<br/>)?");

    // dependencies
    @Inject protected MsoyEventLogger _eventLog;
    @Inject protected ForumLogic _forumLogic;
    @Inject protected ForumRepository _forumRepo;
    @Inject protected SupportLogic _supportLogic;
    @Inject protected MailLogic _mailLogic;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected MemberRepository _memberRepo;
    @Inject protected FeedRepository _feedRepo;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MsoyGameRepository _gameRepo;
    @Inject protected MsoySceneRepository _sceneRepo;
}
