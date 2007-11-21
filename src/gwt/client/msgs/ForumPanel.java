//
// $Id$

package client.msgs;

import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Displays forum threads and messages.
 */
public class ForumPanel extends VerticalPanel
{
    public ForumPanel ()
    {
    }

    public void displayGroupThreads (int groupId)
    {
        clear();
        ThreadListPanel threads = new ThreadListPanel();
        threads.displayGroupThreads(groupId);
        add(threads);
    }
}
