//
// $Id$

package com.threerings.msoy.server.persist;

import java.io.Serializable;
import java.util.Set;

import com.samskivert.util.Tuple;

import com.samskivert.depot.CacheInvalidator;
import com.samskivert.depot.DepotRepository;
import com.samskivert.depot.PersistenceContext;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Computed;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.clause.FieldDefinition;
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.RatingResult;
import com.threerings.presents.annotation.BlockingThread;

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

        _id = id;
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
        int deltaSum;
        boolean newRating;
        
        RatingExtractionRecord targetRec = load(RatingExtractionRecord.class,
            new FromOverride(getTargetClass()),
            new Where(_id, targetId),
            new FieldDefinition("ratingCount", _ratingCount),
            new FieldDefinition("ratingSum", _ratingSum));

        if (targetRec == null) {
            throw new IllegalArgumentException(
                "Asked to rate a non-existent record [targetId=" + targetId + "]");
        }

        RatingRecord ratingRec = load(getRatingClass(),
            getRatingColumn(RatingRecord.TARGET_ID), targetId,
            getRatingColumn(RatingRecord.MEMBER_ID), memberId);

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
            insert(ratingRec);
        }

        if (deltaSum != 0) {
            targetRec.ratingSum += deltaSum;
            if (newRating) {
                targetRec.ratingCount ++;
                updatePartial(getTargetClass(), targetId,
                    _ratingSum, targetRec.ratingSum,
                    _ratingCount, targetRec.ratingCount);
            } else {
                updatePartial(getTargetClass(), targetId, _ratingSum, targetRec.ratingSum);
            }
        }
        
        RatingResult result = new RatingResult(targetRec.ratingSum, targetRec.ratingCount);
        return Tuple.newTuple(result, newRating);
    }

    
    
    // TODO: Doc me
    public void deleteRatings (int targetId)
    {
        deleteAll(getRatingClass(), new Where(getRatingColumn(RatingRecord.TARGET_ID), targetId));
    }

    // TODO: Doc me
    public void reassignRatings (final int oldTargetId, int newTargetId)
    {
        // TODO: this cache eviction might be slow :)
        updatePartial(getRatingClass(), new Where(getRatingColumn(RatingRecord.TARGET_ID), oldTargetId),
                      new CacheInvalidator.TraverseWithFilter<RatingRecord>(getRatingClass()) {
                          @Override
                          public boolean testForEviction (Serializable key, RatingRecord record) {
                              return (record.targetId == oldTargetId);
                          }
                      }, RatingRecord.TARGET_ID, newTargetId);
    }

    /**
     * Returns the rating given to the specified target by the specified member or 0 if they've never
     * rated the target.
     */
    public byte getRating (int targetId, int memberId)
    {
        RatingRecord record = load(
            getRatingClass(), RatingRecord.TARGET_ID, targetId, RatingRecord.MEMBER_ID, memberId);
        return (record == null) ? (byte)0 : record.rating;
    }

    /** Exports the specific rating class used by this repository. */
    protected abstract Class<RatingRecord> getRatingClass ();

    /** Exports the specific target (item or room) rated by this repository. */
    protected abstract Class<? extends PersistentRecord> getTargetClass ();

    protected ColumnExp getRatingColumn (ColumnExp col)
    {
        return new ColumnExp(getRatingClass(), col.name);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(getRatingClass());
    }

    protected ColumnExp _id, _ratingSum, _ratingCount;
}
