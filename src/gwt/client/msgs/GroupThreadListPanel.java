//
// $Id$

package client.msgs;

import java.util.List;

import client.ui.MsoyUI;
import client.util.Link;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.util.DataModel;

import com.threerings.msoy.fora.gwt.ForumThread;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

/**
 * Overrides and adds functionality to the threads list for displaying group threads.
 */
public class GroupThreadListPanel extends ThreadListPanel<ForumThread>
{
    public GroupThreadListPanel (ForumPanel parent, ForumModels fmodels, int groupId)
    {
        super(parent, fmodels, new Object[] {"f", groupId});
        _groupId = groupId;
    }

    @Override // from ThreadListPanel
    protected ForumThread getThread (ForumThread item)
    {
        return item;
    }

    @Override // from ThreadListPanel
    protected void doSearch (AsyncCallback<List<ForumThread>> callback)
    {
        _fmodels.searchGroupThreads(_groupId, _query, callback);
    }

    @Override // from ThreadListPanel
    protected DataModel<ForumThread> getThreadListModel ()
    {
        return _fmodels.getGroupThreads(_groupId);
    }

    @Override // from PagedGrid
    protected void addCustomControls (FlexTable controls)
    {
        super.addCustomControls(controls);

        // add a button for starting a new thread that will optionally be enabled later
        _startThread = new Button(_mmsgs.tlpStartNewThread(), new ClickListener() {
            public void onClick (Widget sender) {
                if (MsoyUI.requireValidated()) {
                    Link.go(Pages.GROUPS, Args.compose("p", _groupId));
                }
            }
        });
        _startThread.setEnabled(false);
        controls.setWidget(0, 0, _startThread);
    }

    @Override // from PagedGrid
    protected void displayResults (int start, int count, List<ForumThread> list)
    {
        super.displayResults(start, count, list);
        _startThread.setEnabled(((ForumModels.GroupThreads)_model).canStartThread());
    }

    /** Contains the id of the group whose threads we are displaying or zero. */
    protected int _groupId;

    /** A button for starting a new thread. */
    protected Button _startThread;
}
