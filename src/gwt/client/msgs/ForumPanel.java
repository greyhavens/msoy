//
// $Id$

package client.msgs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.fora.data.ForumThread;

import client.images.msgs.MsgsImages;
import client.shell.Frame;
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

    public void displayGroupThreads (int groupId)
    {
        ThreadListPanel threads = new ThreadListPanel(this);
        threads.displayGroupThreads(groupId, _fmodels);
        setContents(createHeader(groupId, CMsgs.mmsgs.groupThreadListHeader(), threads), threads);

        // set up a callback to configure our page title when we learn this group's name
        _fmodels.getGroupThreads(groupId).setGotGroupName(new AsyncCallback() {
            public void onSuccess (Object result) {
                Frame.setTitle(result.toString());
            }
            public void onFailure (Throwable error) { /* not used */ }
        });
    }

    public void displayUnreadThreads (boolean refresh)
    {
        ThreadListPanel threads = new ThreadListPanel(this);
        threads.displayUnreadThreads(_fmodels, refresh);
        setContents(createHeader(0, CMsgs.mmsgs.groupUnreadThreadsHeader(), null), threads);
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
        header.setText(0, col++, title, 1, "Title");
        if (listener != null) {
            header.setWidget(0, col++, new SearchBox(listener), 1, "Search");
        }
        header.setText(0, col++, CMsgs.mmsgs.groupThreadPosts(), 1, "Posts");
        header.setText(0, col++, CMsgs.mmsgs.groupThreadLastPost(), 1, "LastPost");
        return header;
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

    /** Our action icon images. */
    protected static MsgsImages _images = (MsgsImages)GWT.create(MsgsImages.class);
}
