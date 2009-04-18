//
// $Id$

package client.msgs;

import java.util.List;

import client.ui.MsoyUI;
import client.util.ClickCallback;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.util.DataModel;

import com.threerings.msoy.fora.gwt.ForumThread;

/**
 * Overrides and adds functionality to the threads list for displaying unread threads.
 */
public class UnreadThreadListPanel extends ThreadListPanel
{
    public UnreadThreadListPanel (ForumPanel parent, ForumModels fmodels)
    {
        super(parent, fmodels, new String[] {"unread"});
    }

    @Override // from ThreadListPanel
    protected void doSearch (AsyncCallback<List<ForumThread>> callback)
    {
        _fmodels.searchUnreadThreads(_query, callback);
    }

    @Override // from ThreadListPanel
    protected DataModel<ForumThread> getThreadListModel ()
    {
        return _fmodels.getUnreadThreads(false);
    }

    @Override // from PagedGrid
    protected Widget createEmptyContents ()
    {
        return _query.length() > 0 ? super.createEmptyContents() :
            MsoyUI.createHTML(_mmsgs.noUnreadThreads(), "Empty");
    }

    @Override // from PagedGrid
    protected void addCustomControls (FlexTable controls)
    {
        super.addCustomControls(controls);

        // add a button for refreshing our unread thread list
        _refresh = new Button(_mmsgs.tlpRefresh(), new ClickListener() {
            public void onClick (Widget sender) {
                _fmodels.getUnreadThreads(true); // refresh
                refresh();
            }
        });
        controls.setWidget(0, 1, _refresh);
    }

    @Override // from ThreadListPanel
    protected ThreadSummaryPanel createThreadSummaryPanel (ForumThread thread)
    {
        return new UnreadThreadSummaryPanel(thread);
    }

    protected class UnreadThreadSummaryPanel extends ThreadSummaryPanel
    {
        protected UnreadThreadSummaryPanel (final ForumThread thread)
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
                    setModel(_fmodels.getUnreadThreads(true), getPage());
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
