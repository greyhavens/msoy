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

    public void displayGroupThreads (int groupId, boolean isManager)
    {
        ThreadListPanel threads = new ThreadListPanel(this);
        threads.displayGroupThreads(groupId, _fmodels);
        setContents(createHeader(CMsgs.mmsgs.groupThreadListHeader()), threads);
        _isManager = isManager;
    }

    public void displayUnreadThreads (boolean refresh)
    {
        ThreadListPanel threads = new ThreadListPanel(this);
        threads.displayUnreadThreads(_fmodels, refresh);
        setContents(createHeader(CMsgs.mmsgs.groupUnreadThreadsHeader()), threads);
    }

    public void startNewThread (int groupId)
    {
        setContents(CMsgs.mmsgs.ntpTitle(), new NewThreadPanel(groupId, _isManager));
    }

    /**
     * Called by the GroupView that contains us to enable our manager state once it knows whether
     * we're a manager in the group in question.
     */
    public void setIsManager (boolean isManager)
    {
        _isManager = isManager;
    }

    protected FlexTable createHeader (String title)
    {
        FlexTable header = new FlexTable();
        header.setCellSpacing(0);
        header.setCellPadding(0);
        header.setWidth("100%");
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
        displayGroupThreads(thread.groupId, _isManager);
    }

    protected void newThreadCanceled (int groupId)
    {
        displayGroupThreads(groupId, _isManager);
    }

    /** Our forum model cache. */
    protected ForumModels _fmodels;

    /** Whether or not we're a manager in the group we're displaying. */
    protected boolean _isManager;
}
