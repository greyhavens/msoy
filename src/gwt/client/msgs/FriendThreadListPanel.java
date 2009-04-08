//
// $Id$

package client.msgs;

import java.util.Date;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.util.DataModel;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.fora.gwt.ForumThread;
import com.threerings.msoy.fora.gwt.ForumService.FriendThread;

import client.ui.MsoyUI;

/**
 * Thread list panel for showing a list of friend threads.
 */
public class FriendThreadListPanel extends ThreadListPanel<FriendThread>
{
    public FriendThreadListPanel (ForumPanel parent, ForumModels fmodels)
    {
        super(parent, fmodels, new Object[] {"funread"});
    }

    @Override // from ThreadListPanel
    protected void doSearch (AsyncCallback<List<FriendThread>> callback)
    {
        // TODO
    }

    @Override // from ThreadListPanel
    protected ForumThread getThread (FriendThread item)
    {
        return item.thread;
    }

    @Override // from ThreadListPanel
    protected DataModel<FriendThread> getThreadListModel ()
    {
        return _fmodels.getUnreadFriendsThreads(false);
    }

    @Override // from ThreadListPanel
    protected MemberName getPoster (FriendThread item)
    {
        return item.friendName;
    }

    @Override // from ThreadListPanel
    protected Date getPostTime (FriendThread item)
    {
        return item.friendPostTime;
    }

    @Override // from PagedGrid
    protected Widget createEmptyContents ()
    {
        return _query.length() > 0 ? super.createEmptyContents() :
            MsoyUI.createHTML(_mmsgs.noUnreadFriendThreads(), "Empty");
    }

    @Override // from PagedGrid
    protected void addCustomControls (FlexTable controls)
    {
        super.addCustomControls(controls);

        // add a button for refreshing our unread thread list
        _refresh = new Button(_mmsgs.tlpRefresh(), new ClickListener() {
            public void onClick (Widget sender) {
                _fmodels.getUnreadFriendsThreads(true); // refresh
                refresh();
            }
        });
        controls.setWidget(0, 1, _refresh);
    }

    @Override
    protected ThreadSummaryPanel createThreadSummaryPanel (FriendThread item)
    {
        return new UnreadFriendThreadSummaryPanel(item);
    }

    protected class UnreadFriendThreadSummaryPanel extends ThreadSummaryPanel
    {
        protected UnreadFriendThreadSummaryPanel (FriendThread ft)
        {
            super(ft);
        }

        @Override
        protected void addSubjectBits (FlowPanel bits, ForumThread thread)
        {
            super.addSubjectBits(bits, thread);

            // we're displaying unread threads from many groups, so display the group name w/ link
            bits.add(makeGroupLink(thread));
        }
    }

    /** A button for refreshing the current model. */
    protected Button _refresh;
}
