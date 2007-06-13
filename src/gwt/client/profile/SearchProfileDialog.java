//
// $Id$

package client.profile;

import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;

import client.util.BorderedDialog;
import client.shell.Application;

/**
 * Displays an interface for searching profiles
 */
public class SearchProfileDialog extends BorderedDialog
{
    public SearchProfileDialog ()
    {
        _header.add(createTitleLabel(CProfile.msgs.searchTitle(), null));
        ClickListener goButtonListener = new ClickListener() {
            public void onClick (Widget sender) {
                String args = "search_";
                if (_radioName.isChecked()) {
                    args += "name_0_";
                } else if (_radioDisplayName.isChecked()) {
                    args += "display_0_";
                } else {
                    args += "email_0_";
                }
                args += URL.encodeComponent(_search.getText().trim());
                History.newItem(Application.createLinkToken("profile", args));
                hide();
            }
        };
        _footer.add(_go = new Button(CProfile.msgs.searchGo(), goButtonListener));
        _go.setEnabled(false);

        FlexTable contents = (FlexTable)_contents;
        int row = 0;
        contents.getFlexCellFormatter().setColSpan(row, 0, 4);
        contents.getFlexCellFormatter().setStyleName(row, 0, "Intro");
        contents.setText(row++, 0, CProfile.msgs.searchIntro());

        contents.getFlexCellFormatter().setStyleName(row, 0, "rightLabel");
        contents.setText(row, 0, CProfile.msgs.searchType());
        contents.setWidget(row, 1, _radioName = new RadioButton("searchType", 
            CProfile.msgs.searchRadioName()));
        contents.setWidget(row, 2, _radioDisplayName = new RadioButton("searchType",
            CProfile.msgs.searchRadioDisplayName()));
        contents.setWidget(row++, 3, _radioEmail = new RadioButton("searchType",
            CProfile.msgs.searchRadioEmail()));
        _radioName.setChecked(true);

        contents.getFlexCellFormatter().setColSpan(row, 1, 3);
        contents.setWidget(row++, 1, _search = new TextBox());
        _search.addKeyboardListener(new KeyboardListenerAdapter() {
            public void onKeyPress (Widget sender, char charCode, int modifiers) {
                DeferredCommand.add(new Command() {
                    public void execute () {
                        _go.setEnabled(_search.getText().trim().length() != 0);
                    }
                });
            }
        });
        _search.addKeyboardListener(new EnterClickAdapter(goButtonListener));
        _search.setVisibleLength(40);
    }

    // @Override // from BorderedDialog
    protected Widget createContents ()
    {
        FlexTable contents = new FlexTable();
        contents.setCellSpacing(10);
        contents.setStyleName("formDialog");
        return contents;
    }

    protected Button _go;
    protected RadioButton _radioName, _radioDisplayName, _radioEmail;
    protected TextBox _search;
}
