//
// $Id$

package client.msgs;

import java.util.List;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.DataModel;

import com.threerings.msoy.fora.data.ForumThread;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.MsoyUI;
import client.util.RowPanel;

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

    public void displayGroupThreads (int groupId, ForumModels fmodels)
    {
        _groupId = groupId;
        setModel(fmodels.getGroupThreads(groupId), 0);
    }

    public void displayUnreadThreads (ForumModels fmodels, boolean refresh)
    {
        _groupId = 0;
        setModel(fmodels.getUnreadThreads(refresh), 0);
    }

    // @Override // from PagedGrid
    protected Widget createWidget (Object item)
    {
        return new ThreadSummaryPanel((ForumThread)item);
    }

    // @Override // from PagedGrid
    protected Widget createEmptyContents ()
    {
        if (_groupId != 0) {
            return super.createEmptyContents();
        }

        HTML empty = new HTML(CMsgs.mmsgs.noUnreadThreads());
        empty.setStyleName("Empty");
        return empty;
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
        _startThread = new Button(CMsgs.mmsgs.tlpStartNewThread(), new ClickListener() {
            public void onClick (Widget sender) {
                _parent.startNewThread(_groupId);
            }
        });
        _startThread.setEnabled(false);
        controls.setWidget(0, 0, _startThread);

        // add a button for refreshing our unread thread list
        _refresh = new Button(CMsgs.mmsgs.tlpRefresh(), new ClickListener() {
            public void onClick (Widget sender) {
                _parent.displayUnreadThreads(true);
            }
        });
        controls.setWidget(0, 1, _refresh);
    }

    // @Override // from PagedGrid
    protected void displayResults (int start, int count, List list)
    {
        super.displayResults(start, count, list);

        if (_model instanceof ForumModels.GroupThreads) { 
            _startThread.setVisible(true);
            _startThread.setEnabled(((ForumModels.GroupThreads)_model).canStartThread());
            _refresh.setVisible(false);
            _refresh.setEnabled(false);
        } else {
            // we're displaying unread threads
            _startThread.setVisible(false);
            _startThread.setEnabled(false);
            _refresh.setVisible(true);
            _refresh.setEnabled(true);
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
            String itype = (thread.hasUnreadMessages() ? "unread" : "read");
            setWidget(0, col, new Image("/images/msgs/" + itype + ".png"));
            getFlexCellFormatter().setStyleName(0, col++, "Status");

            // TODO: display flags icons next to subject
            Hyperlink subject = Application.createLink(
                thread.subject, Page.GROUP, Args.compose("t", thread.threadId));
            if (thread.flags != 0) {
                RowPanel bits = new RowPanel();
                if (thread.isAnnouncement()) {
                    bits.add(new Label(CMsgs.mmsgs.tlpAnnounce()));
                }
                bits.add(subject);
                for (int ii = 0; ii < FLAG_IMAGES.length; ii++) {
                    if ((thread.flags & (1 << ii)) != 0) {
                        bits.add(new Image("/images/msgs/" + FLAG_IMAGES[ii] + ".png"));
                    }
                }
                setWidget(0, col, bits);
            } else {
                setWidget(0, col, subject);
            }
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

    /** Our unread threads data model. */
    protected DataModel _unreadModel;

    /** A button for starting a new thread. */
    protected Button _startThread;

    /** A button for refreshing the current model. */
    protected Button _refresh;

    /** Used to format the most recent post date. */
    protected static SimpleDateFormat _pdate = new SimpleDateFormat("MMM dd, yyyy h:mm aa");

    /** The number of threads displayed per page (TODO: base this on browser height). */
    protected static final int THREADS_PER_PAGE = 10;

    /** Images displayed next to threads that have special flags. */
    protected static final String[] FLAG_IMAGES = { "announce", "sticky", "locked" };
}
