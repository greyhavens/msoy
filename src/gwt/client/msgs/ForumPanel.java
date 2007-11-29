//
// $Id$

package client.msgs;

import com.google.gwt.user.client.ui.FlexTable;

import com.threerings.msoy.fora.data.ForumThread;

import client.util.HashIntMap;
import client.util.MsoyUI;

/**
 * Displays forum threads and messages.
 */
public class ForumPanel extends TitledListPanel
{
    public ForumPanel (HashIntMap gmodels)
    {
        _gmodels = gmodels;
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
        threads.displayGroupThreads(groupId, _gmodels);
        setContents(header, threads);
    }

    public void startNewThread (int groupId)
    {
        setContents(CMsgs.mmsgs.startNewThread(), new NewThreadPanel(groupId), false);
    }

    protected void newThreadPosted (ForumThread thread)
    {
        MsoyUI.info(CMsgs.mmsgs.msgNewThreadPosted());
        // if we already have this model loaded (we should), let it know about the new thread
        ForumModels.GroupThreads gmodel = (ForumModels.GroupThreads)_gmodels.get(thread.groupId);
        if (gmodel != null) {
            gmodel.prependItem(thread);
        }
        displayGroupThreads(thread.groupId);
    }

    /** A cache of group thread models. */
    protected HashIntMap _gmodels;
}
