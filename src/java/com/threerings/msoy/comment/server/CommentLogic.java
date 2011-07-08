//
// $Id$

package com.threerings.msoy.comment.server;

import java.util.Set;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.server.persist.MemberCardRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.comment.data.all.Comment;
import com.threerings.msoy.comment.data.all.CommentType;
import com.threerings.msoy.comment.server.persist.CommentRecord;
import com.threerings.msoy.comment.server.persist.CommentRepository;

import com.threerings.msoy.web.gwt.MemberCard;

@BlockingThread @Singleton
public class CommentLogic
{
    public List<Comment> loadComments (CommentType etype, int eid, int offset, int count)
        // throws ServiceException
    {
        List<CommentRecord> records = _commentRepo.loadComments(etype.toByte(), eid, offset, count, false);

        // resolve the member cards for all commentors
        Set<Integer> memIds = Sets.newHashSet();
        for (CommentRecord record : records) {
            memIds.add(record.memberId);
        }
        Map<Integer, MemberCard> cards = MemberCardRecord.toMap(_memberRepo.loadMemberCards(memIds));

        // convert the comment records to runtime records
        Map<Long, Comment> comments = Maps.newTreeMap();
        for (CommentRecord record : records) {
            Comment comment = record.toComment(cards);
            if (comment.commentor == null) {
                continue; // this member was deleted, shouldn't happen
            }
            if (comment.isReply()) {
                Comment subject = comments.get(comment.replyTo);
                if (subject != null) {
                    subject.replies.add(comment);
                } else {
                    // Errr...
                }
            } else {
                comments.put(comment.posted, comment);
            }
        }

        return Lists.reverse(Lists.newArrayList(comments.values()));
    }

    @Inject protected CommentRepository _commentRepo;
    @Inject protected MemberRepository _memberRepo;
}
