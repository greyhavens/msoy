//
// $Id$

package client.issues;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.fora.gwt.Issue;

/**
 * A helper class for translating the type, category, priority and state messages of an issue.
 */
public class IssueMsgs
{
    /**
     * Returns the translated type string.
     */
    public static String typeMsg (Issue issue)
    {
        return typeMsg(issue.type);
    }

    /**
     * Returns the translated state string.
     */
    public static String stateMsg (Issue issue)
    {
        return stateMsg(issue.state);
    }

    /**
     * Returns the translated priority string.
     */
    public static String priorityMsg (Issue issue)
    {
        return priorityMsg(issue.priority);
    }

    /**
     * Returns the translated category string.
     */
    public static String categoryMsg (Issue issue)
    {
        return categoryMsg(issue.category);
    }

    /**
     * Returns the translated type string.
     */
    public static String typeMsg (int type)
    {
        switch (type) {
        case Issue.TYPE_BUG:
            return _msgs.iBugReport();
        case Issue.TYPE_FEATURE:
            return _msgs.iFeature();
        default:
            return "";
        }
    }

    /**
     * Returns the translated state string.
     */
    public static String stateMsg (int state)
    {
        switch (state) {
        case Issue.STATE_OPEN:
            return _msgs.iOpen();
        case Issue.STATE_RESOLVED:
            return _msgs.iResolved();
        case Issue.STATE_IGNORED:
            return _msgs.iIgnored();
        case Issue.STATE_POSTPONED:
            return _msgs.iPostponed();
        default:
            return "";
        }
    }

    /**
     * Returns the translated priority string.
     */
    public static String priorityMsg (int priority)
    {
        switch (priority) {
        case Issue.PRIORITY_LOW:
            return _msgs.iLow();
        case Issue.PRIORITY_MEDIUM:
            return _msgs.iMedium();
        case Issue.PRIORITY_HIGH:
            return _msgs.iHigh();
        default:
            return "";
        }
    }

    /**
     * Returns the translated category string.
     */
    public static String categoryMsg (int category)
    {
        switch (category) {
        case Issue.CAT_NONE:
            return _msgs.iNoCategory();
        default:
            return "";
        }
    }

    protected static final IssuesMessages _msgs = (IssuesMessages)GWT.create(IssuesMessages.class);
}
