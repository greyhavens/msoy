//
// $Id$

package client.msgs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.fora.gwt.ForumThread;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.images.msgs.MsgsImages;
import client.shell.CShell;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.ui.SearchBox;
import client.util.Link;

/**
 * Displays group threads, unread threads, or new thread starter.
 */
public class ForumPanel extends TitledListPanel
{
    /** Possible modes of the panel. */
    public enum Mode
    {
        /** Viewing the user's unread threads. */
        UNREAD,

        /** Viewing the threads of a group. */
        GROUPS,

        /** Viewing threads with unread posts from friends. */
        FRIENDS,

        /** Starting a new thread. */
        NEW_THREAD
    }

    /**
     * Creates a new panel in the given mode with the given group id. For UNREAD mode, the groupId
     * should always be 0. For GROUPS and UNREAD modes, the panel will not be functional until
     * {@link #setPage(String, int)} is called for the first time.
     */
    public ForumPanel (ForumModels fmodels, Mode mode, final int groupId)
    {
        _fmodels = fmodels;
        _mode = mode;
        _groupId = groupId;
        switch (mode) {
        case GROUPS:
            _threads = new GroupThreadListPanel(this, _fmodels, groupId);
            setContents(createHeader(groupId, _mmsgs.groupThreadListHeader(), _threads), _threads);

            // set up a callback to configure our page title when we learn this group's name
            _fmodels.getGroupThreads(groupId).addGotNameListener(new AsyncCallback<GroupName>() {
                public void onSuccess (GroupName result) {
                    CShell.frame.setTitle(result.toString());
                    setGroupTitle(groupId, result.toString());
                }
                public void onFailure (Throwable error) { /* not used */ }
            });
            break;
        case UNREAD:
            _threads = new UnreadThreadListPanel(this, _fmodels);
            setContents(createHeader(0, _mmsgs.msgUnreadThreadsHeader(), _threads), _threads);
            break;
        case FRIENDS:
            _threads = new FriendThreadListPanel(this, _fmodels);
            setContents(createHeader(0, _mmsgs.msgUnreadThreadsHeader(), _threads), _threads);
            break;
        case NEW_THREAD:
            startNewThread(groupId);
            break;
        }
    }

    /**
     * Determines if this panel is in the given mode for the given group id.
     */
    public boolean isInMode (Mode mode, int groupId)
    {
        return _mode == mode && _groupId == groupId;
    }

    /**
     * Goes to the given page of the given search results. If the query is blank, the full thread
     * list is shown.
     */
    public void setPage (String query, int page)
    {
        switch (_mode) {
        case GROUPS:
        case FRIENDS:
        case UNREAD:
            _threads.setPage(query, page);
        }
    }

    protected void startNewThread (final int groupId)
    {
        if (!MsoyUI.requireValidated()) {
            setContents(_mmsgs.ntpTitle(), MsoyUI.createLabel(_cmsgs.requiresValidated(), null));
            return;
        }

        final ForumModels.GroupThreads gthreads = _fmodels.getGroupThreads(groupId);
        if (gthreads.canStartThread()) {
            setContents(_mmsgs.ntpTitle(), new NewThreadPanel(groupId, gthreads.isManager(),
                                                              gthreads.isAnnounce()));
            return;
        }

        if (gthreads.isFetched()) {
            setContents(_mmsgs.ntpTitle(), MsoyUI.createLabel(
                _mmsgs.errNoPermissionsToPost(), null));
            return;
        }

        gthreads.doFetch(new AsyncCallback<Void>() {
            public void onSuccess (Void result) {
                startNewThread(groupId);
            }
            public void onFailure (Throwable caught) {} // not used
        });
    }

    protected SmartTable createHeader (int groupId, String title, SearchBox.Listener listener)
    {
        SmartTable header = new SmartTable(0, 0);
        header.setWidth("100%");
        int col = 0;
        if (groupId > 0) {
            Anchor rss = new Anchor("/rss/" + groupId, "", "_blank");
            rss.setHTML(_images.rss().getHTML());
            header.setWidget(0, col++, rss, 1, "RSS");
        }
        // default title may be overwritten later
        _title = MsoyUI.createSimplePanel(new Label(title), "TitleBox");
        header.setWidget(0, col++, _title, 1, "Title");

        if (listener != null) {
            header.setWidget(0, col++, new SearchBox(listener), 1, "Search");
        }
        header.setText(0, col++, _mmsgs.groupThreadPosts(), 1, "Posts");
        header.setText(0, col++, _mode == Mode.FRIENDS_POSTS ? _mmsgs.groupThreadFriendPost() :
            _mmsgs.groupThreadLastPost(), 1, "LastPost");
        if (groupId == 0) {
            header.setText(0, col++, "", 1, "IgnoreThread");
        }
        return header;
    }

    /**
     * After _fmodels is filled, override the default title with the group name and a link to it.
     */
    protected void setGroupTitle (int groupId, String groupName)
    {
        if (groupId > 0) {
            _title.setWidget(Link.groupView(groupName, groupId));
        }
    }

    protected void newThreadPosted (ForumThread thread)
    {
        MsoyUI.info(_mmsgs.msgNewThreadPosted());
        _fmodels.newThreadPosted(thread);
        // go back to page 0 so the new thread will show up
        Link.go(Pages.GROUPS, Args.compose("f", _groupId));
    }

    protected void newThreadCanceled (int groupId)
    {
        History.back();
    }

    /** Our forum model cache. */
    protected ForumModels _fmodels;

    /** The mode we're in. */
    protected Mode _mode;

    /** The group we're looking at, if any. */
    protected int _groupId;

    /** The current list of threads, if any. */
    protected ThreadListPanel<?> _threads;

    /** Title for the page, set to group name after data load */
    protected SimplePanel _title;

    protected static final MsgsImages _images = (MsgsImages)GWT.create(MsgsImages.class);
    protected static final MsgsMessages _mmsgs = (MsgsMessages)GWT.create(MsgsMessages.class);
    protected static final ShellMessages _cmsgs = (ShellMessages)GWT.create(ShellMessages.class);
}
