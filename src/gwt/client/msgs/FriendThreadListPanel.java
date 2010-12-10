//
// $Id$

package client.msgs;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.util.DataModel;

import com.threerings.msoy.fora.gwt.ForumThread;

import client.ui.MsoyUI;
import client.util.ClickCallback;

/**
 * Thread list panel for showing a list of friend threads.
 */
public class FriendThreadListPanel extends ThreadListPanel
{
    public FriendThreadListPanel (ForumPanel parent, ForumModels fmodels)
    {
        super(parent, fmodels, new Object[] {"friends"});
    }

    @Override // from ThreadListPanel
    protected DataModel<ForumThread> doSearch (String query)
    {
        // TODO: this is not called because the search box is disabled in ForumPanel
        return null;
    }

    @Override // from ThreadListPanel
    protected DataModel<ForumThread> getThreadListModel ()
    {
        return _fmodels.getUnreadFriendsThreads(false);
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
        _refresh = new Button(_mmsgs.tlpRefresh(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                _fmodels.getUnreadFriendsThreads(true); // refresh
                refresh();
            }
        });
        controls.setWidget(0, 1, _refresh);
    }

    @Override
    protected ThreadSummaryPanel createThreadSummaryPanel (ForumThread thread)
    {
        return new UnreadFriendThreadSummaryPanel(thread);
    }

    protected class UnreadFriendThreadSummaryPanel extends ThreadSummaryPanel
    {
        protected UnreadFriendThreadSummaryPanel (final ForumThread thread)
        {
            super(thread);

            // add the ignore button, refresh our threads afterwards
            new ClickCallback<Void>(addIgnoreButton(thread)) {
                @Override protected boolean callService () {
                    _forumsvc.ignoreThread(thread.threadId, this);
                    return true;
                }
                @Override protected boolean gotResult (Void result) {
                    MsoyUI.info(_mmsgs.threadIgnored());
                    setModel(_fmodels.getUnreadFriendsThreads(true), getPage());
                    return false;
                }
            };
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
