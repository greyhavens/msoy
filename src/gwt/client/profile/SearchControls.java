//
// $Id$

package client.profile;

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
        setText(0, col++, CProfile.msgs.searchTitle(), 1, "rightLabel");
        for (int ii = 0; ii < _types.length; ii++) {
            setWidget(0, col++, _types[ii] = new RadioButton("searchType", TYPE_NAMES[ii]));
        }
        _types[0].setChecked(true);

        FlowPanel invite = new FlowPanel();
        invite.add(MsoyUI.createLabel(CProfile.msgs.searchInvite(), "nowrapLabel"));
        invite.add(Application.createLink(CProfile.msgs.searchInviteGo(), Page.ACCOUNT, "invites"));
        col += 1; // skip over the search button column
        getFlexCellFormatter().setRowSpan(0, col, 2);
        setWidget(0, col++, WidgetUtil.makeShim(20, 1));
        getFlexCellFormatter().setRowSpan(0, col, 2);
        setWidget(0, col++, invite);

        setWidget(1, 1, _search = MsoyUI.createTextBox("", -1, 40), _types.length, null);
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
        setWidget(1, 2, _go = new Button(CProfile.msgs.searchGo(), this));
        _go.setEnabled(false);
    }

    public void setSearch (String type, String query)
    {
        for (int ii = 0; ii < TYPES.length; ii++) {
            if (TYPES[ii].equals(type)) {
                _types[ii].setChecked(true);
            }
        }
        _search.setText(query);
    }

    // from interface ClickListener
    public void onClick (Widget sender)
    {
        String[] args =  { "search", null, "0", getQuery() };
        for (int ii = 0; ii < _types.length; ii++) {
            if (_types[ii].isChecked()) {
                args[1] = TYPES[ii];
                break;
            }
        }
        FlashClients.tutorialEvent("friendsSought");
        Application.go(Page.PROFILE, Args.compose(args));
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

    protected RadioButton[] _types = new RadioButton[TYPES.length];
    protected TextBox _search;
    protected Button _go;

    protected static final String[] TYPES = { "name", "display", "email" };
    protected static final String[] TYPE_NAMES = {
        CProfile.msgs.searchName(), CProfile.msgs.searchDisplayName(), CProfile.msgs.searchEmail()
    };
}
