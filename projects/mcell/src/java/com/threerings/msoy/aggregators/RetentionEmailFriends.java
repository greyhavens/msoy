//
// $Id$

package com.threerings.msoy.aggregators;

import java.util.Collections;

import com.threerings.panopticon.aggregator.hadoop.Aggregator;

import com.threerings.msoy.spam.server.SpamLogic.Bucket;

/**
 * Processes retention mail events for users that had some active friends at the time of the
 * mailing.
 */
@Aggregator(output="msoy.RetentionEmailResponse.Friends")
public class RetentionEmailFriends extends RetentionEmailBucketed
{
    /**
     * Creates a new aggregator for retention mailings to users with friends.
     */
    public RetentionEmailFriends ()
    {
        super(Collections.singletonList(Bucket.HAS_PERSONAL_EVENTS));
    }
}
