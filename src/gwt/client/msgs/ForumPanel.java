//
// $Id$

package client.msgs;

import com.google.gwt.user.client.ui.FlexTable;

import com.threerings.msoy.fora.data.ForumThread;

import client.util.MsoyUI;

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
        setContents(createHeader(CMsgs.mmsgs.groupThreadListHeader()), threads);
    }

    public void displayUnreadThreads (boolean refresh)
    {
        ThreadListPanel threads = new ThreadListPanel(this);
        threads.displayUnreadThreads(_fmodels, refresh);
        setContents(createHeader(CMsgs.mmsgs.groupUnreadThreadsHeader()), threads);
    }

    public void startNewThread (int groupId)
    {
        setContents(CMsgs.mmsgs.startNewThread(), new NewThreadPanel(groupId), false);
    }

    protected FlexTable createHeader (String title)
    {
        FlexTable header = new FlexTable();
        header.setStyleName("Header");
        header.setCellSpacing(0);
        header.setCellPadding(0);
        header.setText(0, 0, title);
        header.getFlexCellFormatter().setStyleName(0, 0, "Title");
        header.setText(0, 1, CMsgs.mmsgs.groupThreadPosts());
        header.getFlexCellFormatter().setStyleName(0, 1, "Posts");
        header.setText(0, 2, CMsgs.mmsgs.groupThreadLastPost());
        header.getFlexCellFormatter().setStyleName(0, 2, "LastPost");
        return header;
    }

    protected void newThreadPosted (ForumThread thread)
    {
        MsoyUI.info(CMsgs.mmsgs.msgNewThreadPosted());
        _fmodels.newThreadPosted(thread);
        displayGroupThreads(thread.groupId);
    }

    /** Our forum model cache. */
    protected ForumModels _fmodels;
}
