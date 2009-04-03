//
// $Id$

package client.msgs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.fora.gwt.ForumService;
import com.threerings.msoy.fora.gwt.ForumServiceAsync;
import com.threerings.msoy.fora.gwt.ForumThread;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MiniNowLoadingWidget;
import client.ui.MsoyUI;
import client.ui.SearchBox;
import client.util.Link;
import client.util.ServiceUtil;

/**
 * Displays a list of threads. Suitable for subclassing to override what kind of threads are
 * displayed.
 */
public abstract class ThreadListPanel extends PagedGrid<ForumThread>
    implements SearchBox.Listener
{
    public ThreadListPanel (ForumPanel parent, ForumModels fmodels)
    {
        super(MsoyUI.computeRows(USED_HEIGHT, THREAD_HEIGHT, 10), 1, NAV_ON_BOTTOM);
        addStyleName("dottedGrid");
        setWidth("100%");
        _parent = parent;
        _fmodels = fmodels;
    }

    @Override // from PagedGrid
    protected Widget createWidget (ForumThread thread)
    {
        return createThreadSummaryPanel(thread);
    }

    @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return _mmsgs.noThreads();
    }

    @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return true; // we always show our navigation for consistency
    }

    @Override // from PagedWidget
    protected Widget getNowLoadingWidget ()
    {
        return new MiniNowLoadingWidget();
    }

    protected ThreadSummaryPanel createThreadSummaryPanel (ForumThread thread)
    {
        return new ThreadSummaryPanel(thread);
    }

    protected class ThreadSummaryPanel extends SmartTable
    {
        public ThreadSummaryPanel (ForumThread thread)
        {
            super("threadSummaryPanel", 0, 0);

            int col = 0;
            Image statusImage = new Image();
            if (thread.hasUnreadMessages()) {
                statusImage.setUrl("/images/msgs/unread.png");
                statusImage.setTitle(_mmsgs.tlpStatusUnreadTip());
            } else {
                statusImage.setUrl("/images/msgs/read.png");
                statusImage.setTitle(_mmsgs.tlpStatusReadTip());
            }
            setWidget(0, col++, statusImage, 1, "Status");

            FlowPanel bits = MsoyUI.createFlowPanel("Subject");
            addSubjectBits(bits, thread);
            getFlexCellFormatter().setHorizontalAlignment(0, col, HasAlignment.ALIGN_LEFT);
            setWidget(0, col++, bits);

            setText(0, col++, String.valueOf(thread.posts), 1, "Posts");

            FlowPanel mrp = MsoyUI.createFlowPanel("LastPost");
            mrp.add(new Label(MsoyUI.formatDateTime(thread.mostRecentPostTime)));
            Widget latest = Link.create(
                _mmsgs.tlpBy(thread.mostRecentPoster.toString()),
                Pages.GROUPS, threadArgs(thread.threadId, thread.posts-1, thread.mostRecentPostId));
            latest.setTitle(_mmsgs.tlpLastTip());
            mrp.add(latest);
            setWidget(0, col++, mrp, 1, "LPCell");
        }

        protected void addSubjectBits (FlowPanel bits, ForumThread thread)
        {
            for (int ii = 0; ii < FLAG_IMAGES.length; ii++) {
                if ((thread.flags & (1 << ii)) != 0) {
                    Image image = MsoyUI.createImage(
                        "/images/msgs/" + FLAG_IMAGES[ii] + ".png", "inline");
                    image.setTitle(FLAG_TIPS[ii]);
                    bits.add(image);
                }
            }

            Widget toThread;
            if (thread.hasUnreadMessages()) {
                // this is slightly hacky but, we track the index of the last read post, but we
                // really want to send you to the first unread post, but we don't know what the id
                // of that post is, so we send you to the page that contains the post after your
                // last read post but we tell the page to scroll to your last read post; so if your
                // first unread post is on the same page as your last read post, you see the one
                // you last read and the first unread below it, if your first unread post is the
                // first post on a page, you just go to that page without scrolling to any message
                // (but since your first unread post is first, that's basically what you want)
                int pidx = thread.lastReadPostIndex+1;
                String args = threadArgs(thread.threadId, pidx, thread.lastReadPostId);
                toThread = Link.create(thread.subject, Pages.GROUPS, args);
                toThread.setTitle(_mmsgs.tlpFirstUnreadTip());
            } else {
                String args = threadArgs(thread.threadId, 0, 0);
                toThread = Link.create(thread.subject, Pages.GROUPS, args);
            }
            bits.add(toThread);
        }
    }

    protected static String threadArgs (int threadId, int msgIndex, int msgId)
    {
        Object[] args = new Object[msgId > 0 ? 4 : 2];
        args[0] = "t";
        args[1] = threadId;
        if (msgId > 0) {
            args[2] = (msgIndex / MessagesPanel.MESSAGES_PER_PAGE);
            args[3] = msgId;
        }
        return Args.compose(args);
    }

    /** The forum panel in which we're hosted. */
    protected ForumPanel _parent;

    /** Provides access to our forum models. */
    protected ForumModels _fmodels;

    protected static final MsgsMessages _mmsgs = (MsgsMessages)GWT.create(MsgsMessages.class);
    protected static final ForumServiceAsync _forumsvc = (ForumServiceAsync)
        ServiceUtil.bind(GWT.create(ForumService.class), ForumService.ENTRY_POINT);

    /** The maximum number of thread search results. */
    protected static final int MAX_RESULTS = 20;

    /** The height used by fixed interface elements (including our header and footer and the
     * FrameHeader). */
    protected static final int USED_HEIGHT = 123;

    /** The height of a thread summary row. */
    protected static final int THREAD_HEIGHT = 34;

    /** Images displayed next to threads that have special flags. */
    protected static final String[] FLAG_IMAGES = { "announce", "sticky", "locked" };

    /** Tooltips for our image icons. */
    protected static final String[] FLAG_TIPS = {
        _mmsgs.tlpAnnounceTip(), _mmsgs.tlpStickyTip(), _mmsgs.tlpLockedTip()
    };
}
