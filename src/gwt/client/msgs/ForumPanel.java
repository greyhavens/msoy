//
// $Id$

package client.msgs;

import com.threerings.msoy.fora.data.ForumThread;

import client.util.MsoyUI;

/**
 * Displays forum threads and messages.
 */
public class ForumPanel extends TitledListPanel
{
    public ForumPanel (String prefix)
    {
        _prefix = prefix;
    }

    public void displayGroupThreads (int groupId)
    {
        ThreadListPanel threads = new ThreadListPanel(this);
        threads.displayGroupThreads(groupId);
        setContents(CMsgs.mmsgs.groupThreadListHeader(), threads, false);
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

    protected String _prefix;
}
