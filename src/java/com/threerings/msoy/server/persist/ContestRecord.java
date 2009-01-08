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
    public static final Class<ContestRecord> _R = ContestRecord.class;
    public static final ColumnExp CONTEST_ID = colexp(_R, "contestId");
    public static final ColumnExp ICON_HASH = colexp(_R, "iconHash");
    public static final ColumnExp ICON_MIME_TYPE = colexp(_R, "iconMimeType");
    public static final ColumnExp ICON_CONSTRAINT = colexp(_R, "iconConstraint");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp BLURB = colexp(_R, "blurb");
    public static final ColumnExp STATUS = colexp(_R, "status");
    public static final ColumnExp PRIZES = colexp(_R, "prizes");
    public static final ColumnExp PAST_BLURB = colexp(_R, "pastBlurb");
    public static final ColumnExp STARTS = colexp(_R, "starts");
    public static final ColumnExp ENDS = colexp(_R, "ends");
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
                new ColumnExp[] { CONTEST_ID },
                new Comparable[] { contestId });
    }
    // AUTO-GENERATED: METHODS END
}
