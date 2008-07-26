//
// $Id$

package client.msgs;

import com.threerings.msoy.fora.gwt.Issue;

/**
 * A helper class for translating the type, category, priority and state messages of an issue.
 */
public class IssueMsgs
{
    /**
     * Returns the translated type string.
     */
    public static String typeMsg (Issue issue, MsgsMessages msgs)
    {
        return typeMsg(issue.type, msgs);
    }

    /**
     * Returns the translated state string.
     */
    public static String stateMsg (Issue issue, MsgsMessages msgs)
    {
        return stateMsg(issue.state, msgs);
    }

    /**
     * Returns the translated priority string.
     */
    public static String priorityMsg (Issue issue, MsgsMessages msgs)
    {
        return priorityMsg(issue.priority, msgs);
    }

    /**
     * Returns the translated category string.
     */
    public static String categoryMsg (Issue issue, MsgsMessages msgs)
    {
        return categoryMsg(issue.category, msgs);
    }

    /**
     * Returns the translated type string.
     */
    public static String typeMsg (int type, MsgsMessages msgs)
    {
        switch (type) {
        case Issue.TYPE_BUG:
            return msgs.iBugReport();
        case Issue.TYPE_FEATURE:
            return msgs.iFeature();
        default:
            return "";
        }
    }

    /**
     * Returns the translated state string.
     */
    public static String stateMsg (int state, MsgsMessages msgs)
    {
        switch (state) {
        case Issue.STATE_OPEN:
            return msgs.iOpen();
        case Issue.STATE_RESOLVED:
            return msgs.iResolved();
        case Issue.STATE_IGNORED:
            return msgs.iIgnored();
        default:
            return "";
        }
    }

    /**
     * Returns the translated priority string.
     */
    public static String priorityMsg (int priority, MsgsMessages msgs)
    {
        switch (priority) {
        case Issue.PRIORITY_LOW:
            return msgs.iLow();
        case Issue.PRIORITY_MEDIUM:
            return msgs.iMedium();
        case Issue.PRIORITY_HIGH:
            return msgs.iHigh();
        default:
            return "";
        }
    }

    /**
     * Returns the translated category string.
     */
    public static String categoryMsg (int category, MsgsMessages msgs)
    {
        switch (category) {
        case Issue.CAT_NONE:
            return msgs.iNoCategory();
        default:
            return "";
        }
    }
}
