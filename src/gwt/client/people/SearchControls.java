//
// $Id$

package client.people;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.FlashClients;
import client.util.MsoyUI;

/**
 * Displays controls that can be used to search for people in Whirled.
 */
public class SearchControls extends SmartTable
    implements ClickListener
{
    public SearchControls ()
    {
        super("searchControls", 0, 5);

        int col = 0;
        setText(0, col++, CPeople.msgs.searchTitle(), 1, "rightLabel");

        setWidget(0, col++, _search = MsoyUI.createTextBox("", -1, 40), 1, null);
        _search.addKeyboardListener(new KeyboardListenerAdapter() {
            public void onKeyPress (Widget sender, char charCode, int modifiers) {
                DeferredCommand.add(new Command() {
                    public void execute () {
                        _go.setEnabled(getQuery().length() != 0);
                    }
                });
            }
        });
        _search.addKeyboardListener(new EnterClickAdapter(this));
        setWidget(0, col++, _go = new Button(CPeople.msgs.searchGo(), this));
        _go.setEnabled(false);

        setText(1, 1, CPeople.msgs.searchTip(), 2, "tipLabel");

        setWidget(0, col++, WidgetUtil.makeShim(20, 1));

        FlowPanel invite = new FlowPanel();
        invite.add(MsoyUI.createLabel(CPeople.msgs.searchInvite(), "nowrapLabel"));
        invite.add(Application.createLink(CPeople.msgs.searchInviteGo(), Page.PEOPLE, "invites"));
        getFlexCellFormatter().setRowSpan(0, col, 2);
        setWidget(0, col++, invite);
    }

    public void setSearch (String query)
    {
        _search.setText(query);
    }

    // from interface ClickListener
    public void onClick (Widget sender)
    {
        FlashClients.tutorialEvent("friendsSought");
        Application.go(Page.PEOPLE, Args.compose("search", "0", getQuery()));
    }

    // @Override // from Widget
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
