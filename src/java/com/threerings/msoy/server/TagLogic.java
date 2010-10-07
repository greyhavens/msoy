//
// $Id$

package com.threerings.msoy.server;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.TagHistoryRecord;
import com.threerings.msoy.server.persist.TagNameRecord;
import com.threerings.msoy.server.persist.TagRepository;
import com.threerings.msoy.web.gwt.TagHistory;
import com.threerings.presents.annotation.BlockingThread;

/**
 * Services for tags and tagged objects (items and groups at the time of writing).
 */
@BlockingThread @Singleton
public class TagLogic
{
    public List<TagHistory> getTagHistory (int targetId, TagRepository repo, int offset, int rows)
        throws ServiceException
    {
        List<TagHistoryRecord> records = repo.getTagHistoryByTarget(targetId, offset, rows);

        Map<Integer, MemberName> names = _memberRepo.loadMemberNames(
            records, TagHistoryRecord.GET_MEMBER_ID);

        Map<Integer, TagNameRecord> tagNames = getTagNames(repo, records);

        List<TagHistory> list = Lists.newArrayList();
        for (TagHistoryRecord threc : records) {
            TagHistory history = new TagHistory();
            history.member = names.get(threc.memberId);
            history.action = threc.action;
            history.time = new Date(threc.time.getTime());

            if (threc.tagId != -1) {
                TagNameRecord tagRec = tagNames.get(threc.tagId);
                if (tagRec != null) {
                    history.tag = tagRec.tag;
                }
            }
            list.add(history);
        }
        return list;
    }

    protected Map<Integer, TagNameRecord> getTagNames (
        TagRepository repo, List<TagHistoryRecord> records)
    {
        Set<Integer> tagIds = Sets.newHashSet();
        for (TagHistoryRecord threc : records) {
            tagIds.add(threc.tagId);
        }
        Map<Integer, TagNameRecord> tagNames = repo.getTags(tagIds);
        return tagNames;
    }

    @Inject MemberRepository _memberRepo;
}
