//
// $Id$

package com.threerings.msoy.server;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.RatingHistoryResult;
import com.threerings.msoy.data.all.RatingHistoryResult.RatingHistoryEntry;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.RatingRecord;
import com.threerings.msoy.server.persist.RatingRepository;
import com.threerings.web.gwt.ServiceException;
import com.threerings.presents.annotation.BlockingThread;

/**
 * Services for ratings and rated objects (items, rooms and games at the time of writing).
 */
@BlockingThread @Singleton
public class RatingLogic
{
    public RatingHistoryResult getRatingHistory (
        int targetId, RatingRepository repo, int offset, int rows)
        throws ServiceException
    {
        List<RatingRecord> records = repo.getRatings(targetId, offset, rows);

        Map<Integer, MemberName> names =
            _memberRepo.loadMemberNames(records, RatingRecord.GET_MEMBER_ID);

        List<RatingHistoryEntry> entries = Lists.newArrayList();
        for (RatingRecord rec : records) {
            MemberName name = names.get(rec.memberId);
            if (name == null) {
                name = new MemberName("Unknown #" + rec.memberId, rec.memberId);
            }
            entries.add(new RatingHistoryEntry(
                name, rec.rating, new Date(rec.timestamp.getTime())));
        }
        return new RatingHistoryResult(entries);
    }

    @Inject MemberRepository _memberRepo;
}
