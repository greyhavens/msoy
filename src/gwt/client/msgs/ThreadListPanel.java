//
// $Id$

package client.msgs;

import java.util.List;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;

import com.threerings.msoy.fora.data.ForumThread;

/**
 * Displays a list of threads.
 */
public class ThreadListPanel extends PagedGrid
{
    public ThreadListPanel ()
    {
        super(THREADS_PER_PAGE, 1, NAV_ON_BOTTOM);
        addStyleName("dottedGrid");
    }

    public void displayGroupThreads (int groupId)
    {
        setModel(new ForumModels.GroupThreads(groupId), 0);
    }

    // @Override // from PagedGrid
    protected Widget createWidget (Object item)
    {
        return new ThreadSummaryPanel(this, (ForumThread)item);
    }

    // @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return CMsgs.mmsgs.noThreads();
    }

    // @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return true; // we always show our navigation for consistency
    }

    // @Override // from PagedGrid
    protected void addCustomControls (FlexTable controls)
    {
        super.addCustomControls(controls);

        // if we're displaying a single group's threads...
        if (_model instanceof ForumModels.GroupThreads) {
            // ...add a button for starting a new thread that will optionally be enabled later
            _startThread = new Button(CMsgs.mmsgs.startNewThread(), new ClickListener() {
                public void onClick (Widget sender) {
                }
            });
            _startThread.setEnabled(false);
            controls.setWidget(0, 0, _startThread);
        }
    }

    // @Override // from PagedGrid
    protected void displayResults (int start, int count, List list)
    {
        super.displayResults(start, count, list);

        if (_model instanceof ForumModels.GroupThreads) {
            _startThread.setEnabled(((ForumModels.GroupThreads)_model).canStartThread());
        }
    }

    protected static class ThreadSummaryPanel extends FlexTable
    {
        public ThreadSummaryPanel (ThreadListPanel parent, ForumThread thread)
        {
            setText(0, 0, "" + thread.threadId);
            setText(0, 1, "" + thread.groupId);
            setText(0, 2, "" + thread.flags);
            setText(0, 3, thread.subject);
            setText(0, 4, "" + thread.mostRecentPostId);
            setText(0, 5, thread.mostRecentPostTime.toString());
            setText(0, 6, "" + thread.mostRecentPoster.toString());
        }
    }

    /** A button for starting a new thread. May be null. */
    protected Button _startThread;

    protected static final int THREADS_PER_PAGE = 20;
}
