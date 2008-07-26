//
// $Id$

package client.msgs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.fora.gwt.ForumThread;

import com.threerings.msoy.data.all.GroupName;

import client.images.msgs.MsgsImages;

import client.shell.Args;
import client.shell.Frame;
import client.shell.Page;

import client.util.Link;
import client.util.MsoyUI;
import client.util.SearchBox;

/**
 * Displays forum threads and messages.
 */
public class ForumPanel extends TitledListPanel
{
    public ForumPanel (ForumModels fmodels)
    {
        _fmodels = fmodels;
    }

    /**
     * Display threads from a single group
     */
    public void displayGroupThreads (final int groupId)
    {
        ThreadListPanel threads = new ThreadListPanel(this);
        threads.displayGroupThreads(groupId, _fmodels);
        setContents(createHeader(groupId, CMsgs.mmsgs.groupThreadListHeader(""), threads), threads);

        // set up a callback to configure our page title when we learn this group's name
        _fmodels.getGroupThreads(groupId).addGotNameListener(new AsyncCallback<GroupName>() {
            public void onSuccess (GroupName result) {
                Frame.setTitle(result.toString());
                setGroupTitle(groupId, result.toString());
            }
            public void onFailure (Throwable error) { /* not used */ }
        });
    }

    /**
     * Display unread threads from all groups
     */
    public void displayUnreadThreads (boolean refresh)
    {
        ThreadListPanel threads = new ThreadListPanel(this);
        threads.displayUnreadThreads(_fmodels, refresh);
        setContents(createHeader(0, CMsgs.mmsgs.msgUnreadThreadsHeader(), null), threads);
    }

    public void startNewThread (int groupId)
    {
        ForumModels.GroupThreads gthreads = _fmodels.getGroupThreads(groupId);
        setContents(CMsgs.mmsgs.ntpTitle(), new NewThreadPanel(groupId, gthreads.isManager()));
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
        _title = MsoyUI.createSimplePanel("TitleBox", new Label(title));
        header.setWidget(0, col++, _title, 1, "Title");

        if (listener != null) {
            header.setWidget(0, col++, new SearchBox(listener), 1, "Search");
        }
        header.setText(0, col++, CMsgs.mmsgs.groupThreadPosts(), 1, "Posts");
        header.setText(0, col++, CMsgs.mmsgs.groupThreadLastPost(), 1, "LastPost");
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
            String title = CMsgs.mmsgs.groupThreadListHeader(groupName);
            Label titleLink = MsoyUI.createActionLabel(
                title, Link.createListener(Page.WHIRLEDS, Args.compose("d", groupId)));
            _title.setWidget(titleLink);
        }
    }

    protected void newThreadPosted (ForumThread thread)
    {
        MsoyUI.info(CMsgs.mmsgs.msgNewThreadPosted());
        _fmodels.newThreadPosted(thread);
        displayGroupThreads(thread.group.getGroupId());
    }

    protected void newThreadCanceled (int groupId)
    {
        displayGroupThreads(groupId);
    }

    /** Our forum model cache. */
    protected ForumModels _fmodels;

    /** Title for the page, set to group name after data load */
    protected SimplePanel _title;

    /** Our action icon images. */
    protected static MsgsImages _images = (MsgsImages)GWT.create(MsgsImages.class);
}
