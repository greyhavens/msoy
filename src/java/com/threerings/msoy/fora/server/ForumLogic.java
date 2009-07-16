//
// $Id$

package com.threerings.msoy.fora.server;

import java.util.List;
import java.util.Map;
import java.util.Collections;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.IntMap;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.web.gwt.MemberCard;

import com.threerings.msoy.fora.gwt.ForumMessage;
import com.threerings.msoy.fora.gwt.ForumService;
import com.threerings.msoy.fora.gwt.ForumThread;
import com.threerings.msoy.fora.server.persist.ForumMessageRecord;
import com.threerings.msoy.fora.server.persist.ForumRepository;
import com.threerings.msoy.fora.server.persist.ForumThreadRecord;
import com.threerings.msoy.fora.server.persist.ReadTrackingRecord;
import com.threerings.msoy.group.server.GroupLogic;
import com.threerings.msoy.group.server.persist.GroupRepository;

/**
 * Contains forum services that are used by servlets and other blocking thread code.
 */
@BlockingThread @Singleton
public class ForumLogic
{
    /**
     * Converts a list of threads to a {@link ForumService.ThreadResult}, looking up the last
     * poster names and filling in other bits.
     *
     * @param needLastReadPost If true, lastReadPostId/Index will be included
     * @param needFirstPost If true, includes {@link ForumMessage} of the original post
     */
    public List<ForumThread> resolveThreads (
        MemberRecord mrec, List<ForumThreadRecord> thrrecs, Map<Integer, GroupName> groups,
        boolean needLastReadPost, boolean needFirstPost)
    {
        // enumerate the last-posters and create member names for them
        IntMap<MemberName> names = _memberRepo.loadMemberNames(
            thrrecs, ForumThreadRecord.GET_MOST_RECENT_POSTER_ID);

        // convert the threads to runtime format
        Map<Integer,ForumThread> thrmap = Maps.newLinkedHashMap();
        for (ForumThreadRecord ftr : thrrecs) {
            ForumThread thread = ftr.toForumThread(names, groups);
            // include details on the original post for each thread if required
            if (needFirstPost) {
                ForumMessageRecord fmr = _forumRepo.loadMessages(thread.threadId, 0, 1).get(0);
                MemberCard card = _memberRepo.loadMemberCard(fmr.posterId, false);
                thread.firstPost = fmr.toForumMessage(Collections.singletonMap(fmr.posterId, card));
            }
            thrmap.put(ftr.threadId, thread);
        }

        // fill in the last read post information if the member is logged in
        if (needLastReadPost && mrec != null && thrmap.size() > 0) {
            for (ReadTrackingRecord rtr : _forumRepo.loadLastReadPostInfo(
                     mrec.memberId, thrmap.keySet())) {
                ForumThread ftr = thrmap.get(rtr.threadId);
                if (ftr != null) { // shouldn't be null but let's not have a cow
                    ftr.lastReadPostId = rtr.lastReadPostId;
                    ftr.lastReadPostIndex = rtr.lastReadPostIndex;
                }
            }
        }

        return Lists.newArrayList(thrmap.values());
    }

    /**
     * Loads up to a maximum number threads that contain unread posts by the given member's friends.
     */
    public List<ForumThread> loadUnreadFriendThreads (MemberRecord mrec, int maximum)
    {
        // load up the meta data of unread posts by friends
        List<ForumThreadRecord> threads = _forumRepo.loadUnreadFriendThreads(mrec.memberId,
            _memberRepo.loadFriendIds(mrec.memberId), _groupLogic.getHiddenGroupIds(
                mrec.memberId, null), maximum);

        IntMap<GroupName> groupNames = _groupRepo.loadGroupNames(
            threads, new Function<ForumThreadRecord, Integer> () {
                @Override public Integer apply (ForumThreadRecord record) {
                    return record.groupId;
                }
            });

        return resolveThreads(mrec, threads, groupNames, true, false);
    }

    @Inject protected ForumRepository _forumRepo;
    @Inject protected GroupLogic _groupLogic;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected MemberRepository _memberRepo;
}
