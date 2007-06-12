//
// $Id$

package client.profile;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

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
        _footer.add(_go = new Button(CProfile.msgs.searchGo(), new ClickListener() {
            public void onClick (Widget sender) {
                // TODO
                History.newItem(Application.createLinkToken("profile", "search_testing"));
                hide();
            }
        }));
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
}
