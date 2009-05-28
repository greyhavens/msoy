//
// $Id$

package com.threerings.msoy.admin.gwt;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains a summary of the results of a particular A/B test.
 */
public class ABTestSummary extends ABTest
    implements IsSerializable
{
    public static class Group implements IsSerializable
    {
        /** The id of the group in question. */
        public int group;

        /** The number of visitors assigned to this group. */
        public int assigned;

        /** The number of visitors that eventually registered. */
        public int registered;

        /** The number of visitors that "returned". */
        public int retained;

        /** The count of visitors in this group that took actions. */
        public Map<String, Integer> actions;
    }

    /** Data on the groups that participated in this test. */
    public List<Group> groups;
}
