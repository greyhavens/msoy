//
// $Id$

package com.threerings.msoy.fora.gwt;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.Args;

/**
 * Contains information on a forum thread.
 */
public class ForumThread
    implements IsSerializable
{
    /** The length in characters of the longest allowable subject. */
    public static final int MAX_SUBJECT_LENGTH = 128;

    /** The number of messages displayed on a page. This has to be static for now in order to
     * support links that go to a specific message. */
    public static final int MESSAGES_PER_PAGE = 10;

    /** A flag indicating this is an announcement thread. */
    public static final int FLAG_ANNOUNCEMENT = 0x1 << 0;

    /** A flag indicating this is a sticky thread. */
    public static final int FLAG_STICKY = 0x1 << 1;

    /** A flag indicating this is a locked thread. */
    public static final int FLAG_LOCKED = 0x2 << 1;

    /** A unique identifier for this forum thread. */
    public int threadId;

    /** The group to which this forum thread belongs. */
    public GroupName group;

    /** Flags indicating attributes of this thread: {@link #FLAG_ANNOUNCEMENT}, etc. */
    public int flags;

    /** The subject of this thread. */
    public String subject;

    /** The id of the most recent message posted to this thread. */
    public int mostRecentPostId;

    /** The time at which the most recent message was posted to this thread. */
    public Date mostRecentPostTime;

    /** The author of the message most recently posted to this thread. */
    public MemberName mostRecentPoster;

    /** The number of posts in this thread. */
    public int posts;

    /** The requesting member's last read post id or 0. */
    public int lastReadPostId;

    /** The requesting member's last read post index or 0. */
    public int lastReadPostIndex;

    /** Details of the original post - not always required */
    public ForumMessage firstPost;

    /**
     * Returns true if this is an announcement thread.
     */
    public boolean isAnnouncement ()
    {
        return (flags & FLAG_ANNOUNCEMENT) != 0;
    }

    /**
     * Returns true if this is a sticky thread.
     */
    public boolean isSticky ()
    {
        return (flags & FLAG_STICKY) != 0;
    }

    /**
     * Returns true if this thread is locked.
     */
    public boolean isLocked ()
    {
        return (flags & FLAG_LOCKED) != 0;
    }

    /**
     * Returns true if this thread has unread messages (from the standpoint of the user that loaded
     * the thread).
     */
    public boolean hasUnreadMessages ()
    {
        return (lastReadPostId < mostRecentPostId);
    }

    /**
     * Gets the token arguments that will link to the most recent post.
     */
    public Args getMostRecentPostArgs ()
    {
        return getArgs(posts-1, mostRecentPostId);
    }

    /**
     * Gets the token arguments that will link to the first unread post.     
     */
    public Args getFirstUnreadPostArgs ()
    {
        // this is slightly hacky but, we track the index of the last read post, but we
        // really want to send you to the first unread post, but we don't know what the id
        // of that post is, so we send you to the page that contains the post after your
        // last read post but we tell the page to scroll to your last read post; so if your
        // first unread post is on the same page as your last read post, you see the one
        // you last read and the first unread below it, if your first unread post is the
        // first post on a page, you just go to that page without scrolling to any message
        // (but since your first unread post is first, that's basically what you want)
        // Also, do not exceed the number of posts lest we show an empty page. This can only
        // happen as far as I know in the case of an earlier read post being deleted and the
        // new number of posts being a multiple of the number per page.
        int pidx = Math.min(lastReadPostIndex+1, posts-1);
        return getArgs(pidx, lastReadPostId);
    }

    /**
     * Gets the token arguments that will link to the first post in the thread.
     */
    public Args getFirstPostArgs ()
    {
        return getArgs(0, 0);
    }

    /**
     * Get the token arguments that will link to a message with the given index and id.
     */
    public Args getPostArgs (int messageIndex, int messageId)
    {
        return getArgs(messageIndex, messageId);
    }

    // from Object
    public int hashCode ()
    {
        return threadId;
    }

    // from Object
    public boolean equals (Object other)
    {
        return (other instanceof ForumThread) && ((ForumThread)other).threadId == threadId;
    }

    protected Args getArgs (int msgIndex, int msgId)
    {
        Object[] args = new Object[msgId > 0 ? 4 : 2];
        args[0] = "t";
        args[1] = threadId;
        if (msgId > 0) {
            args[2] = (msgIndex / MESSAGES_PER_PAGE);
            args[3] = msgId;
        }
        return Args.compose(args);
    }
}
