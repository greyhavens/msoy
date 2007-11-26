//
// $Id$

package client.msgs;

import java.util.List;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.PagedGrid;

import com.threerings.msoy.fora.data.ForumThread;

import client.shell.Application;
import client.shell.Page;
import client.shell.Args;
import client.util.MsoyUI;

/**
 * Displays a list of threads.
 */
public class ThreadListPanel extends PagedGrid
{
    public ThreadListPanel (ForumPanel parent)
    {
        super(THREADS_PER_PAGE, 1, NAV_ON_BOTTOM);
        addStyleName("dottedGrid");
        setWidth("100%");
        _parent = parent;
    }

    public void displayGroupThreads (int groupId)
    {
        _groupId = groupId;
        setModel(new ForumModels.GroupThreads(groupId), 0);
    }

    // @Override // from PagedGrid
    protected Widget createWidget (Object item)
    {
        return new ThreadSummaryPanel((ForumThread)item);
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

        // add a button for starting a new thread that will optionally be enabled later
        _startThread = new Button(CMsgs.mmsgs.startNewThread(), new ClickListener() {
            public void onClick (Widget sender) {
                _parent.startNewThread(_groupId);
            }
        });
        _startThread.setEnabled(false);
        controls.setWidget(0, 0, _startThread);
    }

    // @Override // from PagedGrid
    protected void displayResults (int start, int count, List list)
    {
        super.displayResults(start, count, list);

        if (_model instanceof ForumModels.GroupThreads) { 
            _startThread.setVisible(true);
            _startThread.setEnabled(((ForumModels.GroupThreads)_model).canStartThread());
        } else {
            _startThread.setVisible(false);
            _startThread.setEnabled(false);
        }
    }

    protected class ThreadSummaryPanel extends FlexTable
    {
        public ThreadSummaryPanel (ForumThread thread)
        {
            setStyleName("threadSummaryPanel");
            setCellPadding(0);
            setCellSpacing(0);

            int col = 0;
            setText(0, col, "" + thread.flags);
            getFlexCellFormatter().setStyleName(0, col++, "Flags");

            setWidget(0, col, Application.createLink(thread.subject, Page.GROUP,
                                                   Args.compose("t", thread.threadId)));
            getFlexCellFormatter().setStyleName(0, col++, "Subject");

            setText(0, col, "" + thread.posts);
            getFlexCellFormatter().setStyleName(0, col++, "Posts");

            setHTML(0, col, _pdate.format(thread.mostRecentPostTime) + "<br/>By: " +
                    thread.mostRecentPoster);
            getFlexCellFormatter().setStyleName(0, col++, "LastPost");
        }
    }

    /** The forum panel in which we're hosted. */
    protected ForumPanel _parent;

    /** Contains the id of the group whose threads we are displaying or zero. */
    protected int _groupId;

    /** A button for starting a new thread. */
    protected Button _startThread;

    protected static SimpleDateFormat _pdate = new SimpleDateFormat("MMM dd, yyyy h:mm aa");

    protected static final int THREADS_PER_PAGE = 10;
}
