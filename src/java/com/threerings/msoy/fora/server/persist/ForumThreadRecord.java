//
// $Id$

package com.threerings.msoy.fora.server.persist;

import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.FullTextIndex;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.GenerationType;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.fora.gwt.ForumThread;

/**
 * Contains information on a forum thread.
 */
@Entity(fullTextIndices={
    @FullTextIndex(name=ForumThreadRecord.FTS_SUBJECT, fields={ "subject" })
})
public class ForumThreadRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<ForumThreadRecord> _R = ForumThreadRecord.class;
    public static final ColumnExp THREAD_ID = colexp(_R, "threadId");
    public static final ColumnExp GROUP_ID = colexp(_R, "groupId");
    public static final ColumnExp FLAGS = colexp(_R, "flags");
    public static final ColumnExp SUBJECT = colexp(_R, "subject");
    public static final ColumnExp MOST_RECENT_POST_ID = colexp(_R, "mostRecentPostId");
    public static final ColumnExp MOST_RECENT_POST_TIME = colexp(_R, "mostRecentPostTime");
    public static final ColumnExp MOST_RECENT_POSTER_ID = colexp(_R, "mostRecentPosterId");
    public static final ColumnExp POSTS = colexp(_R, "posts");
    public static final ColumnExp STICKY = colexp(_R, "sticky");
    // AUTO-GENERATED: FIELDS END

    /** The identifier for the full text search index on {@link #subject} */
    public static final String FTS_SUBJECT = "SUBJECT";

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 6;

    /** Provides the {@link #mostRecentPosterId} of a record. */
    public static final Function<ForumThreadRecord,Integer> GET_MOST_RECENT_POSTER_ID =
        new Function<ForumThreadRecord,Integer>() {
        public Integer apply (ForumThreadRecord record) {
            return record.mostRecentPosterId;
        }
    };

    /** A unique identifier for this forum thread. */
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    public int threadId;

    /** The id of the group to which this forum thread belongs. */
    @Index(name="ixGroupId")
    public int groupId;

    /** Flags indicating attributes of this thread: {@link ForumThread#FLAG_ANNOUNCEMENT}, etc. */
    public int flags;

    /** The subject of this thread. */
    @Column(length=ForumThread.MAX_SUBJECT_LENGTH)
    public String subject;

    /** The id of the most recent message posted to this thread. */
    @Index(name="ixMostRecentPostId")
    public int mostRecentPostId;

    /** The time at which the most recent message was posted to this thread. */
    public Timestamp mostRecentPostTime;

    /** The member id of the author of the message most recently posted to this thread. */
    public int mostRecentPosterId;

    /** The number of posts in this thread. */
    public int posts;

    /** Whether or not this thread is sticky. Used for sorting. */
    @Index(name="ixSticky")
    public boolean sticky;

    /**
     * Converts this persistent record to a runtime record.
     *
     * @param members a mapping from memberId to {@link MemberName} that should contain a mapping
     * for {@link #mostRecentPosterId}.
     */
    public ForumThread toForumThread (Map<Integer,MemberName> members, Map<Integer,GroupName> groups)
    {
        ForumThread record = new ForumThread();
        record.threadId = threadId;
        record.group = groups.get(groupId);
        record.flags = flags;
        record.subject = subject;
        record.mostRecentPostId = mostRecentPostId;
        record.mostRecentPostTime = new Date(mostRecentPostTime.getTime());
        record.mostRecentPoster = members.get(mostRecentPosterId);
        record.posts = posts;
        // sticky is only used for database sorting
        return record;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link ForumThreadRecord}
     * with the supplied key values.
     */
    public static Key<ForumThreadRecord> getKey (int threadId)
    {
        return new Key<ForumThreadRecord>(
                ForumThreadRecord.class,
                new ColumnExp[] { THREAD_ID },
                new Comparable[] { threadId });
    }
    // AUTO-GENERATED: METHODS END
}
