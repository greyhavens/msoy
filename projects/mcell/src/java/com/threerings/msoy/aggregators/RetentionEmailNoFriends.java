//
// $Id$

package com.threerings.msoy.aggregators;

import com.google.common.collect.Lists;

import com.threerings.panopticon.aggregator.hadoop.Aggregator;

import com.threerings.msoy.spam.server.SpamLogic.Bucket;

/**
 * Processes retention mail events for users that had no friends or only inactive friends at the
 * time of the mailing.
 */
@Aggregator(output="msoy.RetentionEmailResponse.NoFriends")
public class RetentionEmailNoFriends extends RetentionEmailBucketed
{
    /**
     * Creates a new aggregator for retention mailings to users with no friends or only inactive
     * friends.
     */
    public RetentionEmailNoFriends ()
    {
        super(Lists.newArrayList(Bucket.HAS_NO_FRIENDS, Bucket.HAS_INACTIVE_FRIENDS));
    }
}
