//
// $Id$

package client.people;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import client.shell.Args;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.FlashClients;
import client.util.Link;

/**
 * Displays controls that can be used to search for people in Whirled.
 */
public class SearchControls extends SmartTable
    implements ClickListener
{
    public SearchControls ()
    {
        super("searchControls", 0, 5);

        PushButton action;
        if (CPeople.isGuest()) {
            action = MsoyUI.createButton(MsoyUI.LONG_THIN, CPeople.msgs.searchJoin(),
                                         Link.createListener(Pages.ACCOUNT, "create"));
        } else {
            action = MsoyUI.createButton(MsoyUI.LONG_THIN, CPeople.msgs.searchInvite(),
                                         Link.createListener(Pages.PEOPLE, "invites"));
        }
        setWidget(0, 0, action, 1, "Action");
        getFlexCellFormatter().setRowSpan(0, 0, 2);

        FlowPanel bits = new FlowPanel();
        bits.add(MsoyUI.createLabel(CPeople.msgs.searchTitle(), "Title"));
        bits.add(MsoyUI.createLabel(CPeople.msgs.searchTip(), "Tip"));
        setWidget(0, 1, bits, 2, null);

        setWidget(1, 0, _search = MsoyUI.createTextBox("", -1, -1), 1, null);
        _search.setWidth("200px");
        _search.addKeyboardListener(new KeyboardListenerAdapter() {
            public void onKeyPress (Widget sender, char charCode, int modifiers) {
                DeferredCommand.addCommand(new Command() {
                    public void execute () {
                        _go.setEnabled(getQuery().length() != 0);
                    }
                });
            }
        });
        _search.addKeyboardListener(new EnterClickAdapter(this));
        setWidget(1, 1, _go = new Button(CPeople.msgs.searchGo(), this));
        _go.setEnabled(false);

        setWidget(1, 2, WidgetUtil.makeShim(15, 15));
    }

    public void setSearch (String query)
    {
        _search.setText(query);
    }

    // from interface ClickListener
    public void onClick (Widget sender)
    {
        FlashClients.tutorialEvent("friendsSought");
        Link.go(Pages.PEOPLE, Args.compose("search", "0", getQuery()));
    }

    @Override // from Widget
    protected void onAttach ()
    {
        super.onAttach();
        _search.setFocus(true);
    }

    protected String getQuery ()
    {
        return _search.getText().trim();
    }

    protected TextBox _search;
    protected Button _go;
}
