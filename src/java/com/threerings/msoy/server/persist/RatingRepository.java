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
import com.samskivert.depot.clause.FromOverride;
import com.samskivert.depot.clause.Where;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.presents.annotation.BlockingThread;

/**
 * Manages the persistent side of rating of things in Whirled.
 */
@BlockingThread
public abstract class RatingRepository extends DepotRepository
{
    @Entity @Computed
    public static class RatingAverageRecord extends PersistentRecord {
        @Computed(fieldDefinition="count(*)")
        public int count;
        @Computed(fieldDefinition="avg(" + RatingRecord.RATING + ")")
        public float average;
    }

    /**
     * Creates a tag repository for the supplied tag and tag history record classes.
     */
    public RatingRepository (PersistenceContext ctx, String rating, String ratingCount)
    {
        super(ctx);

        _rating = rating;
        _ratingCount = ratingCount;
    }

    /** Used to coerce RatingRecord derivations of {@link #getRatingClass}. */
    public static Class<RatingRecord> coerceRating (Class<? extends RatingRecord> clazz)
    {
        @SuppressWarnings("unchecked") Class<RatingRecord> cclazz = (Class<RatingRecord>)clazz;
        return cclazz;
    }

    // TODO: Doc me
    public Tuple<RatingAverageRecord, Boolean> rate (int targetId, int memberId, byte rating)
    {
        // Clamp the rating within bounds
        rating = (byte)Math.max(1, Math.min(rating, 5));

        RatingRecord record;
        try {
            record = getRatingClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        record.targetId = targetId;
        record.memberId = memberId;
        record.rating = rating;
        boolean firstTime = store(record);

        RatingAverageRecord newRating = createAverageRecord(targetId);

        // and then smack the new value into the item using yummy depot code
        updatePartial(
            getTargetClass(), targetId, _rating, newRating.average, _ratingCount, newRating.count);

        return new Tuple<RatingAverageRecord, Boolean>(newRating, firstTime);
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

    public RatingAverageRecord createAverageRecord (int targetId)
    {
        RatingAverageRecord record = load(RatingAverageRecord.class,
            new FromOverride(getRatingClass()),
            new Where(getRatingColumn(RatingRecord.TARGET_ID), targetId));
        return record;
    }

    /** Exports the specific rating class used by this repository. */
    protected abstract Class<RatingRecord> getRatingClass ();

    /** Exports the specific target (item or room) rated by this repository. */
    protected abstract Class<? extends PersistentRecord> getTargetClass ();

    protected ColumnExp getRatingColumn (String cname)
    {
        return new ColumnExp(getRatingClass(), cname);
    }

    @Override // from DepotRepository
    protected void getManagedRecords (Set<Class<? extends PersistentRecord>> classes)
    {
        classes.add(getRatingClass());
    }

    protected String _rating, _ratingCount;
}
