//
// $Id$

package com.threerings.msoy.fora.server.persist;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;

import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.Where;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.persist.MemberNameRecord;
import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.fora.data.Comment;

/**
 * Manages member comments on various and sundry things.
 */
public class CommentRepository extends DepotRepository
{
    public CommentRepository (PersistenceContext ctx, MemberRepository memRepo)
    {
        super(ctx);
        _memRepo = memRepo;
    }

    /**
     * Loads the most recent comments for the specified entity type and identifier.
     *
     * @param start the offset into the comments (in reverse time order) to load.
     * @param count the number of comments to load.
     */
    public List<Comment> loadComments (int entityType, int entityId, int start, int count)
        throws PersistenceException
    {
        // load up the specified comment set
        List<CommentRecord> records = findAll(
            CommentRecord.class,
            new Where(CommentRecord.ENTITY_TYPE_C, entityType,
                      CommentRecord.ENTITY_ID_C, entityId),
            OrderBy.descending(CommentRecord.POSTED_C),
            new Limit(start, count));

        ArrayList<Comment> results = new ArrayList<Comment>();
        if (records.size() == 0) {
            return results;
        }

        // resolve the member names for all commentors
        HashIntMap<MemberName> names = new HashIntMap<MemberName>();
        ArrayIntSet memIds = new ArrayIntSet();
        for (CommentRecord record : records) {
            memIds.add(record.memberId);
        }
        for (MemberNameRecord mnr : _memRepo.loadMemberNames(memIds.toIntArray())) {
            names.put(mnr.memberId, mnr.toMemberName());
        }

        // convert the results to runtime records
        for (CommentRecord record : records) {
            Comment comment = record.toComment(names);
            if (comment.commentor == null) {
                continue; // TODO: do we want to keep comments for deleted members? probably not.
            }
            results.add(comment);
        }

        return results;
    }

    /**
     * Loads a specific comment record.
     */
    public CommentRecord loadComment (int entityType, int entityId, long posted)
        throws PersistenceException
    {
        return load(CommentRecord.class,
                    CommentRecord.getKey(entityType, entityId, new Timestamp(posted)));
    }

    /**
     * Posts a new comment on the specified entity by the specified member.
     */
    public void postComment (int entityType, int entityId, int memberId, String text)
        throws PersistenceException
    {
        if (text.length() > Comment.MAX_TEXT_LENGTH) { // sanity check
            throw new PersistenceException(
                "Rejecting overlong comment [type=" + entityType + ", id=" + entityId +
                ", who=" + memberId + ", length=" + text.length() + "]");
        }

        CommentRecord record = new CommentRecord();
        record.entityType = entityType;
        record.entityId = entityId;
        record.posted = new Timestamp(System.currentTimeMillis());
        record.memberId = memberId;
        record.text = text;
        insert(record);
    }

    /**
     * Deletes the comment with the specified key.
     */
    public void deleteComment (int entityType, int entityId, long posted)
        throws PersistenceException
    {
        delete(CommentRecord.class,
               CommentRecord.getKey(entityType, entityId, new Timestamp(posted)));
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(CommentRecord.class);
    }

    protected MemberRepository _memRepo;
}
