//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import com.samskivert.util.Tuple;

import com.samskivert.jdbc.DatabaseLiaison;
import com.samskivert.jdbc.JDBCUtil;
import com.samskivert.jdbc.depot.CacheInvalidator;
import com.samskivert.jdbc.depot.DepotRepository;
import com.samskivert.jdbc.depot.Modifier;
import com.samskivert.jdbc.depot.PersistenceContext;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Computed;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.clause.FromOverride;
import com.samskivert.jdbc.depot.clause.GroupBy;
import com.samskivert.jdbc.depot.clause.Join;
import com.samskivert.jdbc.depot.clause.Limit;
import com.samskivert.jdbc.depot.clause.OrderBy;
import com.samskivert.jdbc.depot.clause.Where;
import com.samskivert.jdbc.depot.expression.ColumnExp;
import com.samskivert.jdbc.depot.operator.Conditionals.In;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.web.gwt.TagHistory;

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
        @Computed(fieldDefinition="sum(" + RatingRecord.RATING + ")")
        public int sum;
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

    // TODO: Doc me
    public Tuple<Float, Boolean> rate (int targetId, int memberId, byte rating)
    {
        RatingRecord record;
        try {
            record = getRatingClass().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        record.targetId = targetId;
        record.memberId = memberId;
        record.rating = rating;
        boolean newRater = store(record);

        RatingAverageRecord average =
            load(RatingAverageRecord.class,
                 new FromOverride(getRatingClass()),
                 new Where(getRatingColumn(RatingRecord.TARGET_ID), targetId));

        float newRating = (average.count == 0) ? 0f : average.sum/(float)average.count;
        // and then smack the new value into the item using yummy depot code
        updatePartial(getTargetClass(), targetId, _rating, newRating, _ratingCount, average.count);

        // TODO: Move this stuff into isStrongRating() or something
        float oldRating = (average.count < 2) ? 0f :
            (average.sum - rating)/(float)(average.count - 1);
        boolean newSolid =
            (average.count == MIN_SOLID_RATINGS && newRating >= 4) ||
            (average.count > MIN_SOLID_RATINGS && newRating >= 4 && (!newRater || oldRating < 4));

        return new Tuple<Float, Boolean>(newRating, newSolid);
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

    // TODO: This probably doesn't belong here
    protected static final int MIN_SOLID_RATINGS = 20;
}
