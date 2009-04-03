//
// $Id$

package client.msgs;

import java.util.List;

import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.InfoCallback;
import client.util.Link;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import com.threerings.msoy.fora.gwt.ForumThread;

/**
 * Overrides and adds functionality to the threads list for displaying unread threads.
 */
public class UnreadThreadListPanel extends ThreadListPanel
{
    public UnreadThreadListPanel (ForumPanel parent, ForumModels fmodels)
    {
        super(parent, fmodels);
        setModel(fmodels.getUnreadThreads(false), 0);
    }

    // from interface SearchBox.Listener
    public void search (String search)
    {
        _forumsvc.findUnreadThreads(search, MAX_RESULTS, new InfoCallback<List<ForumThread>>() {
            public void onSuccess (List<ForumThread> threads) {
                setModel(new SimpleDataModel<ForumThread>(threads), 0);
            }
        });
    }

    // from interface SearchBox.Listener
    public void clearSearch ()
    {
        setModel(_fmodels.getUnreadThreads(true), 0);
    }

    @Override // from PagedGrid
    protected Widget createEmptyContents ()
    {
        return MsoyUI.createHTML(_mmsgs.noUnreadThreads(), "Empty");
    }

    @Override // from PagedGrid
    protected void addCustomControls (FlexTable controls)
    {
        super.addCustomControls(controls);

        // add a button for refreshing our unread thread list
        _refresh = new Button(_mmsgs.tlpRefresh(), new ClickListener() {
            public void onClick (Widget sender) {
                _fmodels.getUnreadThreads(true); // refresh
                _parent.displayUnreadThreads();
            }
        });
        controls.setWidget(0, 1, _refresh);
    }

    @Override
    protected ThreadSummaryPanel createThreadSummaryPanel (ForumThread thread)
    {
        return new UnreadThreadSummaryPanel(thread);
    }

    protected class UnreadThreadSummaryPanel extends ThreadSummaryPanel
    {
        protected UnreadThreadSummaryPanel (final ForumThread thread)
        {
            super(thread);

            // add an ignore button
            Image ignoreThread = MsoyUI.createImage("/images/msgs/ignore.png", "Ignore");
            ignoreThread.setTitle(_mmsgs.ignoreThreadTip());
            new ClickCallback<Void>(ignoreThread) {
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

            int col = getCellCount(0);
            getFlexCellFormatter().setHorizontalAlignment(0, col, HasAlignment.ALIGN_RIGHT);
            setWidget(0, col++, ignoreThread, 1, "IgnoreThread");
        }

        @Override
        protected void addSubjectBits (FlowPanel bits, ForumThread thread)
        {
            super.addSubjectBits(bits, thread);

            // we're displaying unread threads from many groups, so display the group name w/ link
            Widget groupLink = Link.create(_mmsgs.tlpFromGroup(thread.group.toString()),
                "GroupName", Pages.GROUPS, Args.compose("f", thread.group.getGroupId()));
            bits.add(groupLink);
        }
    }

    /** A button for refreshing the current model. */
    protected Button _refresh;
}
