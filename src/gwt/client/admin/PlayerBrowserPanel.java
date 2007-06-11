//
// $Id$

package client.admin;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Displays the various services available to support and admin personnel.
 */
public class PlayerBrowserPanel extends DockPanel
{
    public PlayerBrowserPanel ()
    {
        _playerLists = new ArrayList();

        // first, load up the list of players that don't have an inviterId (defaults to 0)
        CAdmin.adminsvc.getPlayerList(CAdmin.ident, 0, new AsyncCallback() {
            public void onSuccess (Object result) {
                displayList((List)result);
            }
            public void onFailure (Throwable cause) {
                add(new Label(CAdmin.serverError(cause)), DockPanel.NORTH);
            }
        });
    }

    protected void displayList (List players) 
    {
        VerticalPanel panel;
        if (_secondaryList != null) {
            remove(_primaryList);
            _primaryList = _secondaryList;
            add(_primaryList, DockPanel.WEST);
            panel = _secondaryList = new VerticalPanel();
            add(panel = _secondaryList = new VerticalPanel(), DockPanel.EAST);
        } else {
            add(panel = _primaryList = new VerticalPanel(), DockPanel.WEST);
        }
    }

    // ArrayList<VerticalPanel>
    protected ArrayList _playerLists;
    protected VerticalPanel _primaryList;
    protected VerticalPanel _secondaryList;
}
