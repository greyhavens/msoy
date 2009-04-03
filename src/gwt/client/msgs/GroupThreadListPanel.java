//
// $Id$

package client.msgs;

import java.util.List;

import client.ui.MsoyUI;
import client.util.InfoCallback;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.fora.gwt.ForumThread;

/**
 * Overrides and adds functionality to the threads list for displaying group threads.
 */
public class GroupThreadListPanel extends ThreadListPanel
{
    public GroupThreadListPanel (ForumPanel parent, ForumModels fmodels, int groupId)
    {
        super(parent, fmodels);
        _groupId = groupId;
        setModel(_fmodels.getGroupThreads(groupId), 0);
    }

    // from interface SearchBox.Listener
    public void search (String search)
    {
        _forumsvc.findThreads(_groupId, search, MAX_RESULTS,
            new InfoCallback<List<ForumThread>>() {
            public void onSuccess (List<ForumThread> threads) {
                setModel(new SimpleDataModel<ForumThread>(threads), 0);
            }
        });
    }

    // from interface SearchBox.Listener
    public void clearSearch ()
    {
        setModel(_fmodels.getGroupThreads(_groupId), 0);
    }

    @Override // from PagedGrid
    protected void addCustomControls (FlexTable controls)
    {
        super.addCustomControls(controls);

        // add a button for starting a new thread that will optionally be enabled later
        _startThread = new Button(_mmsgs.tlpStartNewThread(), new ClickListener() {
            public void onClick (Widget sender) {
                if (MsoyUI.requireValidated()) {
                    _parent.startNewThread(_groupId);
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
