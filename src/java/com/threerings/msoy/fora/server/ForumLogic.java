//
// $Id$

package com.threerings.msoy.fora.server;

import java.util.List;
import java.util.Map;
import java.util.Collections;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.util.IntMap;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.web.data.MemberCard;

import com.threerings.msoy.fora.gwt.ForumMessage;
import com.threerings.msoy.fora.gwt.ForumThread;
import com.threerings.msoy.fora.gwt.ForumService.ThreadResult;
import com.threerings.msoy.fora.server.persist.ForumMessageRecord;
import com.threerings.msoy.fora.server.persist.ForumRepository;
import com.threerings.msoy.fora.server.persist.ForumThreadRecord;
import com.threerings.msoy.fora.server.persist.ReadTrackingRecord;

/**
 * Contains forum services that are used by servlets and other blocking thread code.
 */
@BlockingThread @Singleton
public class ForumLogic
{
    /**
     * Converts a list of threads to a {@link ThreadResult}, looking up the last poster names and
     * filling in other bits.
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
                ForumMessageRecord messageRec =
                    _forumRepo.loadMessages(thread.threadId, 0, 1).get(0);
                List<MemberCardRecord> memberRecords = _memberRepo.loadMemberCards(
                    Collections.singleton(messageRec.posterId));
                if (memberRecords.size() > 0) {
                    MemberCard memberCard = memberRecords.get(0).toMemberCard();
                    thread.firstPost = messageRec.toForumMessage(Collections.singletonMap(
                        messageRec.posterId, memberCard));
                }
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

    @Inject protected MemberRepository _memberRepo;
    @Inject protected ForumRepository _forumRepo;
}
