//
// $Id$

package com.threerings.msoy.web.server;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.fora.data.ForumThread;
import com.threerings.msoy.fora.server.persist.ForumMessageRecord;
import com.threerings.msoy.fora.server.persist.ForumRepository;
import com.threerings.msoy.fora.server.persist.ForumThreadRecord;
import com.threerings.msoy.fora.server.persist.ReadTrackingRecord;
import com.threerings.msoy.web.client.ForumService.ThreadResult;
import com.threerings.msoy.web.data.MemberCard;

/**
 * Contains forum-related utility methods used by servlets.
 */
public class ForumUtil
{
    /**
     * Converts a list of threads to a {@link ThreadResult}, looking up the last poster names and
     * filling in other bits.
     * @param needLastReadPost If true, lastReadPostId/Index will be included
     * @param needFirstPost If true, includes {@link ForumMessage} of the original post
     */
    public static List<ForumThread> resolveThreads (
        MemberRecord mrec, List<ForumThreadRecord> thrrecs, Map<Integer, GroupName> groups, 
        boolean needLastReadPost, boolean needFirstPost)
        throws PersistenceException
    {
        // enumerate the last-posters and create member names for them
        IntMap<MemberName> names = resolveNames(thrrecs);

        // convert the threads to runtime format
        Map<Integer,ForumThread> thrmap = Maps.newLinkedHashMap();
        for (ForumThreadRecord ftr : thrrecs) {
            ForumThread thread = ftr.toForumThread(names, groups);
            
            // include details on the original post for each thread if required
            if (needFirstPost) {               
                ForumMessageRecord messageRec = _forumRepo.loadMessages(thread.threadId, 0, 1).get(0);
                List<MemberCardRecord> memberRecords = MsoyServer.memberRepo.loadMemberCards(
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
    
    /**
     * Resolves the names of the posters of the supplied threads.
     */
    protected static IntMap<MemberName> resolveNames (List<ForumThreadRecord> thrrecs)
        throws PersistenceException
    {
        IntSet posters = new ArrayIntSet();
        for (ForumThreadRecord thrrec : thrrecs) {
            posters.add(thrrec.mostRecentPosterId);
        }
        IntMap<MemberName> names = IntMaps.newHashIntMap();
        if (posters.size() > 0) {
            for (MemberName name : MsoyServer.memberRepo.loadMemberNames(posters)) {
                names.put(name.getMemberId(), name);
            }
        }
        return names;
    }
    
    /** Shortcut to forum repository */
    protected static ForumRepository _forumRepo = MsoyServer.forumRepo;
}
