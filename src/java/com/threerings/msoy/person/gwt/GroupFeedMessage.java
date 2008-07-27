//
// $Id$

package com.threerings.msoy.person.gwt;

import com.threerings.msoy.data.all.GroupName;

/**
 * Contains data for a group-originated feed message.
 */
public class GroupFeedMessage extends FeedMessage
{
    /** The name of the group to which this message pertains. */
    public GroupName group;
}
