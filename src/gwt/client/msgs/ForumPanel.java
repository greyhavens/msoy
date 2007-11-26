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
    public ForumPanel ()
    {
    }

    public void displayGroupThreads (int groupId)
    {
        FlexTable header = new FlexTable();
        header.setStyleName("Header");
        header.setCellSpacing(0);
        header.setCellPadding(0);
        header.setText(0, 0, CMsgs.mmsgs.groupThreadListHeader());
        header.getFlexCellFormatter().setStyleName(0, 0, "Title");
        header.setText(0, 1, CMsgs.mmsgs.groupThreadPosts());
        header.getFlexCellFormatter().setStyleName(0, 1, "Posts");
        header.setText(0, 2, CMsgs.mmsgs.groupThreadLastPost());
        header.getFlexCellFormatter().setStyleName(0, 2, "LastPost");

        ThreadListPanel threads = new ThreadListPanel(this);
        threads.displayGroupThreads(groupId);
        setContents(header, threads);
    }

    public void startNewThread (int groupId)
    {
        setContents(CMsgs.mmsgs.startNewThread(), new NewThreadPanel(groupId), false);
    }

    protected void newThreadPosted (ForumThread thread)
    {
        MsoyUI.info(CMsgs.mmsgs.msgNewThreadPosted());
        // TODO: add it to our local model and reuse our cached model
        displayGroupThreads(thread.groupId);
    }
}
