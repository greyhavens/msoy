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
import com.threerings.msoy.web.gwt.Contest;

/**
 * Information for a contest as displayed on #me-contests where all contests are listed.
 */
public class ContestRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #contestId} field. */
    public static final String CONTEST_ID = "contestId";

    /** The qualified column identifier for the {@link #contestId} field. */
    public static final ColumnExp CONTEST_ID_C =
        new ColumnExp(ContestRecord.class, CONTEST_ID);

    /** The column identifier for the {@link #iconHash} field. */
    public static final String ICON_HASH = "iconHash";

    /** The qualified column identifier for the {@link #iconHash} field. */
    public static final ColumnExp ICON_HASH_C =
        new ColumnExp(ContestRecord.class, ICON_HASH);

    /** The column identifier for the {@link #iconMimeType} field. */
    public static final String ICON_MIME_TYPE = "iconMimeType";

    /** The qualified column identifier for the {@link #iconMimeType} field. */
    public static final ColumnExp ICON_MIME_TYPE_C =
        new ColumnExp(ContestRecord.class, ICON_MIME_TYPE);

    /** The column identifier for the {@link #iconConstraint} field. */
    public static final String ICON_CONSTRAINT = "iconConstraint";

    /** The qualified column identifier for the {@link #iconConstraint} field. */
    public static final ColumnExp ICON_CONSTRAINT_C =
        new ColumnExp(ContestRecord.class, ICON_CONSTRAINT);

    /** The column identifier for the {@link #name} field. */
    public static final String NAME = "name";

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(ContestRecord.class, NAME);

    /** The column identifier for the {@link #blurb} field. */
    public static final String BLURB = "blurb";

    /** The qualified column identifier for the {@link #blurb} field. */
    public static final ColumnExp BLURB_C =
        new ColumnExp(ContestRecord.class, BLURB);

    /** The column identifier for the {@link #status} field. */
    public static final String STATUS = "status";

    /** The qualified column identifier for the {@link #status} field. */
    public static final ColumnExp STATUS_C =
        new ColumnExp(ContestRecord.class, STATUS);

    /** The column identifier for the {@link #prizes} field. */
    public static final String PRIZES = "prizes";

    /** The qualified column identifier for the {@link #prizes} field. */
    public static final ColumnExp PRIZES_C =
        new ColumnExp(ContestRecord.class, PRIZES);

    /** The column identifier for the {@link #pastBlurb} field. */
    public static final String PAST_BLURB = "pastBlurb";

    /** The qualified column identifier for the {@link #pastBlurb} field. */
    public static final ColumnExp PAST_BLURB_C =
        new ColumnExp(ContestRecord.class, PAST_BLURB);

    /** The column identifier for the {@link #starts} field. */
    public static final String STARTS = "starts";

    /** The qualified column identifier for the {@link #starts} field. */
    public static final ColumnExp STARTS_C =
        new ColumnExp(ContestRecord.class, STARTS);

    /** The column identifier for the {@link #ends} field. */
    public static final String ENDS = "ends";

    /** The qualified column identifier for the {@link #ends} field. */
    public static final ColumnExp ENDS_C =
        new ColumnExp(ContestRecord.class, ENDS);
    // AUTO-GENERATED: FIELDS END

    /** Converts persistent records to runtime records. */
    public static final Function<ContestRecord, Contest> TO_CONTEST =
        new Function<ContestRecord, Contest>() {
        public Contest apply (ContestRecord record) {
            return record.toContest();
        }
    };

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /** This contest's unique identifier (and primary key). */
    @Id
    public String contestId;

    /** The hash code of the contest's icon. */
    @Column(nullable=true)
    public byte[] iconHash;

    /** The MIME type of icon image. */
    public byte iconMimeType;

    /** The size constraint on the icon media. */
    public byte iconConstraint;

    /** HTML for the name of the contest, optionally linking to another page */
    public String name;

    /** The html text of the contest blurb. */
    @Column(length=Contest.MAX_BLURB_LENGTH)
    public String blurb;

    /** The html text of the contest status. */
    public String status;

    /** The html text of the contest prizes. */
    @Column(length=Contest.MAX_PRIZES_LENGTH)
    public String prizes;

    /** HTML to display beside the name under "past contests" after the end date is reached. */
    @Column(length=Contest.MAX_PASTBLURB_LENGTH)
    public String pastBlurb;

    /** The time at which this contest starts. */
    public Timestamp starts;

    /** The time at which this contest is no longer valid. */
    public Timestamp ends;

    /**
     * Creates a persistent record from the supplied runtime record.
     */
    public static ContestRecord fromContest (Contest contest)
    {
        ContestRecord record = new ContestRecord();
        record.contestId = contest.contestId;
        if (contest.icon != null) {
            record.iconHash = contest.icon.hash;
            record.iconMimeType = contest.icon.mimeType;
            record.iconConstraint = contest.icon.constraint;
        }
        record.name = contest.name;
        record.blurb = contest.blurb;
        record.status = contest.status;
        record.prizes = contest.prizes;
        record.pastBlurb = contest.pastBlurb;
        record.starts = new Timestamp(contest.starts.getTime());
        record.ends = new Timestamp(contest.ends.getTime());
        return record;
    }

    /**
     * Creates a runtime record from this persistent record.
     */
    public Contest toContest ()
    {
        Contest contest = new Contest();
        contest.contestId = contestId;
        if (iconHash != null) {
            contest.icon = new MediaDesc(iconHash, iconMimeType, iconConstraint);
        }
        contest.name = name;
        contest.blurb = blurb;
        contest.status = status;
        contest.prizes = prizes;
        contest.pastBlurb = pastBlurb;
        contest.starts = new Date(starts.getTime());
        contest.ends = new Date(ends.getTime());
        return contest;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ContestRecord}
     * with the supplied key values.
     */
    public static Key<ContestRecord> getKey (String contestId)
    {
        return new Key<ContestRecord>(
                ContestRecord.class,
                new String[] { CONTEST_ID },
                new Comparable[] { contestId });
    }
    // AUTO-GENERATED: METHODS END
}
