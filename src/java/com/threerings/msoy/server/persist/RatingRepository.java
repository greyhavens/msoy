//
// $Id$

package com.threerings.msoy.server.persist;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.samskivert.util.StringUtil;
import com.samskivert.util.Tuple;

import com.samskivert.depot.CacheInvalidator;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.DuplicateKeyException;
import com.samskivert.depot.Key;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.clause.FieldDefinition;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Limit;
import com.samskivert.depot.clause.OrderBy;
import com.samskivert.depot.clause.QueryClause;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.RatingResult;
import com.threerings.presents.annotation.BlockingThread;

import static com.threerings.msoy.Log.log;

/**
 * Manages the persistent side of rating of things in Whirled.
 */
@BlockingThread
public abstract class RatingRepository extends DepotRepository
{
    @Entity @Computed
    public static class RatingExtractionRecord extends PersistentRecord {
        public int ratingCount;
        public int ratingSum;
    }

    /**
     * Creates a tag repository for the supplied tag and tag history record classes.
     */
    public RatingRepository (PersistenceContext ctx, ColumnExp id,
        ColumnExp ratingSum, ColumnExp ratingCount)
    {
        super(ctx);

        _targetId = id;
        _ratingSum = ratingSum;
        _ratingCount = ratingCount;
    }

    /** Used to coerce RatingRecord derivations of {@link #getRatingClass}. */
    public static Class<RatingRecord> coerceRating (Class<? extends RatingRecord> clazz)
    {
        @SuppressWarnings("unchecked") Class<RatingRecord> cclazz = (Class<RatingRecord>)clazz;
        return cclazz;
    }

    // TODO: Doc me
    public Tuple<RatingResult, Boolean> rate (int targetId, int memberId, byte rating)
    {
        // Clamp the rating within bounds
        rating = (byte)Math.max(1, Math.min(rating, 5));

        RatingExtractionRecord targetRec = load(RatingExtractionRecord.class,
            new FromOverride(getTargetClass()),
            new Where(_targetId, targetId),
            new FieldDefinition("ratingCount", _ratingCount),
            new FieldDefinition("ratingSum", _ratingSum));

        if (targetRec == null) {
            throw new IllegalArgumentException(
                "Asked to rate a non-existent record [targetId=" + targetId + "]");
        }

        RatingRecord ratingRec = load(
            getRatingClass(), CacheStrategy.NONE, getRatingKey(targetId, memberId));

        boolean newRating;
        int deltaSum;

        if (ratingRec != null) {
            deltaSum = rating - ratingRec.rating;
            newRating = false;
            if (deltaSum != 0) {
                ratingRec.rating = rating;
                update(ratingRec);
            }

        } else {
            try {
                ratingRec = getRatingClass().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            deltaSum = rating;
            newRating = true;

            ratingRec.targetId = targetId;
            ratingRec.memberId = memberId;
            ratingRec.rating = rating;
            ratingRec.timestamp = new Timestamp(System.currentTimeMillis());

            try {
                insert(ratingRec);
            } catch (DuplicateKeyException dke) {
                // this happens, but rarely; it means another thread is surely rating this precise
                // same item for this precise same member, so let's just yield to that thread and
                // return a value
                log.warning("Another thread inserted a rating just before us?",
                    "class", StringUtil.shortClassName(getRatingClass()),
                    "targetId", targetId, "memberId", memberId, "rating", rating);
                return Tuple.newTuple(new RatingResult(
                    targetRec.ratingSum, targetRec.ratingCount), newRating);
            }
        }

        if (deltaSum != 0) {
            targetRec.ratingSum += deltaSum;
            if (newRating) {
                targetRec.ratingCount ++;
                updatePartial(getTargetKey(targetId),
                              _ratingSum, targetRec.ratingSum, _ratingCount, targetRec.ratingCount);
            } else {
                updatePartial(getTargetKey(targetId), _ratingSum, targetRec.ratingSum);
            }
        }

        return Tuple.newTuple(new RatingResult(
            targetRec.ratingSum, targetRec.ratingCount), newRating);
    }

    /**
     * Purges all ratings for the specified target.
     */
    public void deleteRatings (int targetId)
    {
        deleteAll(getRatingClass(),
                  new Where(getRatingColumn(RatingRecord.TARGET_ID), targetId), null);
    }

    // TODO: Doc me
    public void reassignRatings (final int oldTargetId, int newTargetId)
    {
        // TODO: this cache eviction might be slow :)
        updatePartial(
            getRatingClass(), new Where(getRatingColumn(RatingRecord.TARGET_ID), oldTargetId),
            new CacheInvalidator.TraverseWithFilter<RatingRecord>(getRatingClass()) {
                @Override public boolean testForEviction (Serializable key, RatingRecord record) {
                    return (record.targetId == oldTargetId);
                }
            }, RatingRecord.TARGET_ID, newTargetId);
    }

    /**
     * Returns the rating given to the specified target by the specified member or 0 if they've
     * never rated the target.
     */
    public byte getRating (int targetId, int memberId)
    {
        RatingRecord record = load(getRatingKey(targetId, memberId));
        return (record == null) ? (byte)0 : record.rating;
    }

    /**
     * Returns all the rating records for the specified target.
     */
    public List<RatingRecord> getRatings (int targetId, int offset, int count)
    {
        List<QueryClause> clauses = Lists.newArrayList(
            new Where(getRatingColumn(RatingRecord.TARGET_ID), targetId),
            OrderBy.descending(getRatingColumn(RatingRecord.TIMESTAMP)));
        if (count > 0) {
            clauses.add(new Limit(offset, count));
        }
        return findAll(getRatingClass(), clauses);
    }

    /**
     * Deletes all data associated with the supplied members. This is done as a part of purging
     * member accounts.
     */
    public void purgeMembers (Collection<Integer> memberIds)
    {
        Where where = new Where(getRatingColumn(RatingRecord.MEMBER_ID).in(memberIds));

        for (RatingRecord rec : findAll(getRatingClass(), where)) {
            int targetId = rec.targetId;

            RatingExtractionRecord targetRec = load(RatingExtractionRecord.class,
                new FromOverride(getTargetClass()),
                new Where(_targetId, targetId),
                new FieldDefinition("ratingCount", _ratingCount),
                new FieldDefinition("ratingSum", _ratingSum));

            if (targetRec == null) {
                log.warning("Asked to nuke rating for a non-existent record", "target", targetId);
                continue;
            }

            updatePartial(getTargetKey(targetId),
                _ratingSum, targetRec.ratingSum - rec.rating,
                _ratingCount, targetRec.ratingCount - 1);
            delete(rec);
        }
    }

    /** Exports the specific rating class used by this repository. */
    protected abstract Class<RatingRecord> getRatingClass ();

    /** Exports the specific target (item or room) rated by this repository. */
    protected abstract Class<? extends PersistentRecord> getTargetClass ();

    protected ColumnExp getRatingColumn (ColumnExp col)
    {
        return new ColumnExp(getRatingClass(), col.name);
    }

    protected Key<RatingRecord> getRatingKey (int targetId, int memberId)
    {
        return Key.newKey(getRatingClass(),getRatingColumn(RatingRecord.TARGET_ID), targetId,
                          getRatingColumn(RatingRecord.MEMBER_ID), memberId);
    }

    protected Key<? extends PersistentRecord> getTargetKey (int targetId)
    {
        return Key.newKey(getTargetClass(), _targetId, targetId);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(getRatingClass());
    }

    protected ColumnExp _targetId, _ratingSum, _ratingCount;
}
