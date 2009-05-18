//
// $Id$

package client.msgs;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.util.DataModel;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.fora.gwt.ForumService;
import com.threerings.msoy.fora.gwt.ForumServiceAsync;
import com.threerings.msoy.fora.gwt.ForumThread;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MiniNowLoadingWidget;
import client.ui.MsoyUI;
import client.ui.SearchBox;
import client.util.ArrayUtil;
import client.util.InfoCallback;
import client.util.Link;
import client.util.ServiceUtil;

/**
 * Displays a list of threads. Subclasses determine the specifics of accessing the threads on the
 * server, performing searches and display customizations.
 */
public abstract class ThreadListPanel extends PagedGrid<ForumThread>
    implements SearchBox.Listener
{
    /**
     * Creates a new thread list panel.
     * @param baseArgs used when redirecting the browser to a new page or the results of a search
     */
    protected ThreadListPanel (ForumPanel parent, ForumModels fmodels, Object[] baseArgs)
    {
        super(MsoyUI.computeRows(USED_HEIGHT, THREAD_HEIGHT, 10), 1, NAV_ON_BOTTOM);
        addStyleName("dottedGrid");
        setWidth("100%");
        _parent = parent;
        _fmodels = fmodels;
        _baseArgs = baseArgs;
    }

    /**
     * Goes directly to the given page of the given search. If the search string is empty, go to
     * the given page of the thread list. This is called by the forum panel in response to a
     * history change.
     */
    public void setPage (String query, int page)
    {
        setPage(query, page, true);
    }

    // from interface SearchBox.Listener
    public void search (String search)
    {
        // when the user searches, display results via the url
        Link.go(Pages.GROUPS, compose(search));
    }

    // from interface SearchBox.Listener
    public void clearSearch ()
    {
        // when the users clears a search, return to the groups page via the url
        // TODO: store the most recent page number and return to it here
        Link.go(Pages.GROUPS, compose());
    }

    /**
     * Gets the "native" model for the thread list, i.e. for when we are not viewing the results of
     * a search.
     */
    protected abstract DataModel<ForumThread> getThreadListModel();

    /**
     * Performs a search on the contents of this thread list using the current value of
     * {@link #_query}. This will normally involve a call to a model from {@link ForumModels}.
     */
    protected abstract void doSearch (AsyncCallback<List<ForumThread>> callback);

    /**
     * Same as {@link #setPage(String, int)}, but optionally forces the model to be reset to that
     * provided by the subclass implementation, {@link #getThreadListModel()}.
     */
    protected void setPage (String query, int page, boolean force)
    {
        query = query.trim();
        if (!force && _model != null && _query.equals(query)) {
            displayPage(page, false);

        } else {
            if ((_query = query).length() == 0) {
                setModel(getThreadListModel(), page);

            } else {
                doSearch(new InfoCallback<List<ForumThread>>() {
                    public void onSuccess (List<ForumThread> threads) {
                        setModel(new SimpleDataModel<ForumThread>(threads), 0);
                    }
                });
            }
        }
    }

    /**
     * Rebuilds our current page, including getting the model again.
     */
    protected void refresh ()
    {
        setPage(_query, getPage(), true);
    }

    /**
     * Shortcut for concatenating our base args with some more args and {@link Args#compose}'ing
     * the result.
     */
    protected Args compose (Object... moreArgs)
    {
        return Args.compose(ArrayUtil.concatenate(_baseArgs, moreArgs, ArrayUtil.OBJECT_TYPE));
    }

    @Override // from PagedGrid
    protected Widget createWidget (ForumThread thread)
    {
        return createThreadSummaryPanel(thread);
    }

    @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return _query.length() > 0 ? _mmsgs.noMatchingThreads() : _mmsgs.noThreads();
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

    @Override
    protected void displayPageFromClick (int page)
    {
        // route the page request through the url
        Link.go(Pages.GROUPS, compose(_query, page));
    }

    /**
     * Creates a thread summary line for a thread item. Subclasses can override to put in more
     * widgets.
     */
    protected ThreadSummaryPanel createThreadSummaryPanel (ForumThread thread)
    {
        return new ThreadSummaryPanel(thread);
    }

    /**
     * Creates a link to the given group.
     */
    protected static Widget makeGroupLink (ForumThread thread)
    {
        return Link.create(_mmsgs.tlpFromGroup(thread.group.toString()),
            "GroupName", Pages.GROUPS, "f", thread.group.getGroupId());
    }

    /**
     * Summary panel for a single thread item.
     */
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

            FlowPanel post = MsoyUI.createFlowPanel("LastPost");
            post.add(new Label(MsoyUI.formatDateTime(thread.mostRecentPostTime)));
            Widget by = Link.create(
                _mmsgs.tlpBy(thread.mostRecentPoster.toString()),
                Pages.GROUPS, thread.getMostRecentPostArgs());
            by.setTitle(_mmsgs.tlpLastTip());
            post.add(by);
            setWidget(0, col++, post, 1, "LPCell");
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
                toThread = Link.create(
                    thread.subject, Pages.GROUPS, thread.getFirstUnreadPostArgs());
                toThread.setTitle(_mmsgs.tlpFirstUnreadTip());
            } else {
                toThread = Link.create(thread.subject, Pages.GROUPS, thread.getFirstPostArgs());
            }
            bits.add(toThread);
        }

        /**
         * Adds an ignore button. Used by the unread variants to show the ignore button inline in
         * the thread display.
         */
        protected Image addIgnoreButton (final ForumThread thread)
        {
            Image ignoreThread = MsoyUI.createImage("/images/msgs/ignore.png", "Ignore");
            ignoreThread.setTitle(_mmsgs.ignoreThreadTip());
            int col = getCellCount(0);
            getFlexCellFormatter().setHorizontalAlignment(0, col, HasAlignment.ALIGN_RIGHT);
            setWidget(0, col++, ignoreThread, 1, "IgnoreThread");
            return ignoreThread;
        }
    }

    /** The forum panel in which we're hosted. */
    protected ForumPanel _parent;

    /** Provides access to our forum models. */
    protected ForumModels _fmodels;

    /** The query we are currently looking at. */
    protected String _query = ""; 

    /** The first (prepended) arguments for accessing this page. */
    protected Object[] _baseArgs;

    protected static final MsgsMessages _mmsgs = (MsgsMessages)GWT.create(MsgsMessages.class);
    protected static final ForumServiceAsync _forumsvc = (ForumServiceAsync)
        ServiceUtil.bind(GWT.create(ForumService.class), ForumService.ENTRY_POINT);

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
