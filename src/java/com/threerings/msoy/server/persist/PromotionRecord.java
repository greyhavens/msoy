//
// $Id$

package com.threerings.msoy.server.persist;

import java.sql.Timestamp;
import java.util.Date;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.web.gwt.Promotion;

/**
 * Contains information on a promotion shown to people on Whirled in various places.
 */
public class PromotionRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #promoId} field. */
    public static final String PROMO_ID = "promoId";

    /** The qualified column identifier for the {@link #promoId} field. */
    public static final ColumnExp PROMO_ID_C =
        new ColumnExp(PromotionRecord.class, PROMO_ID);

    /** The column identifier for the {@link #iconHash} field. */
    public static final String ICON_HASH = "iconHash";

    /** The qualified column identifier for the {@link #iconHash} field. */
    public static final ColumnExp ICON_HASH_C =
        new ColumnExp(PromotionRecord.class, ICON_HASH);

    /** The column identifier for the {@link #iconMimeType} field. */
    public static final String ICON_MIME_TYPE = "iconMimeType";

    /** The qualified column identifier for the {@link #iconMimeType} field. */
    public static final ColumnExp ICON_MIME_TYPE_C =
        new ColumnExp(PromotionRecord.class, ICON_MIME_TYPE);

    /** The column identifier for the {@link #iconConstraint} field. */
    public static final String ICON_CONSTRAINT = "iconConstraint";

    /** The qualified column identifier for the {@link #iconConstraint} field. */
    public static final ColumnExp ICON_CONSTRAINT_C =
        new ColumnExp(PromotionRecord.class, ICON_CONSTRAINT);

    /** The column identifier for the {@link #blurb} field. */
    public static final String BLURB = "blurb";

    /** The qualified column identifier for the {@link #blurb} field. */
    public static final ColumnExp BLURB_C =
        new ColumnExp(PromotionRecord.class, BLURB);

    /** The column identifier for the {@link #starts} field. */
    public static final String STARTS = "starts";

    /** The qualified column identifier for the {@link #starts} field. */
    public static final ColumnExp STARTS_C =
        new ColumnExp(PromotionRecord.class, STARTS);

    /** The column identifier for the {@link #ends} field. */
    public static final String ENDS = "ends";

    /** The qualified column identifier for the {@link #ends} field. */
    public static final ColumnExp ENDS_C =
        new ColumnExp(PromotionRecord.class, ENDS);
    // AUTO-GENERATED: FIELDS END

    /** Converts persistent records to runtime records. */
    public static final Function<PromotionRecord, Promotion> TO_PROMOTION =
        new Function<PromotionRecord, Promotion>() {
        public Promotion apply (PromotionRecord record) {
            return record.toPromotion();
        }
    };

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    @Id /** This promotions unique identifier (and primary key). */
    public String promoId;

    /** The hash code of the promotion's icon. */
    @Column(nullable=true)
    public byte[] iconHash;

    /** The MIME type of icon image. */
    public byte iconMimeType;

    /** The size constraint on the icon media. */
    public byte iconConstraint;

    /** The text of the promotion blurb. */
    public String blurb;

    /** The time at which this promotion starts. */
    public Timestamp starts;

    /** The time at which this promotion is no longer valid. */
    public Timestamp ends;

    /**
     * Creates a persistent record from the supplied runtime record.
     */
    public static PromotionRecord fromPromotion (Promotion promo)
    {
        PromotionRecord record = new PromotionRecord();
        record.promoId = promo.promoId;
        if (promo.icon != null) {
            record.iconHash = promo.icon.hash;
            record.iconMimeType = promo.icon.mimeType;
            record.iconConstraint = promo.icon.constraint;
        }
        record.blurb = promo.blurb;
        record.starts = new Timestamp(promo.starts.getTime());
        record.ends = new Timestamp(promo.ends.getTime());
        return record;
    }

    /**
     * Creates a runtime record from this persistent record.
     */
    public Promotion toPromotion ()
    {
        Promotion promo = new Promotion();
        promo.promoId = promoId;
        if (iconHash != null) {
            promo.icon = new MediaDesc(iconHash, iconMimeType, iconConstraint);
        }
        promo.blurb = blurb;
        promo.starts = new Date(starts.getTime());
        promo.ends = new Date(ends.getTime());
        return promo;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link PromotionRecord}
     * with the supplied key values.
     */
    public static Key<PromotionRecord> getKey (String promoId)
    {
        return new Key<PromotionRecord>(
                PromotionRecord.class,
                new String[] { PROMO_ID },
                new Comparable[] { promoId });
    }
    // AUTO-GENERATED: METHODS END
}
