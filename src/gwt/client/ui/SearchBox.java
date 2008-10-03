//
// $Id$

package client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FocusListenerAdapter;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import client.shell.ShellMessages;

/**
 * A text box to use for search fields. It says "<search>" in it (which is cleared out when it is
 * focused) and it handles listening for pressing enter.
 */
public class SearchBox extends HorizontalPanel
{
    public static interface Listener {
        void search (String query);
        void clearSearch ();
    }

    public SearchBox (Listener listener)
    {
        setStyleName("searchBox");
        _listener = listener;

        add(_input = new TextBox());
        _input.addKeyboardListener(new KeyboardListenerAdapter() {
            public void onKeyPress (Widget sender, char keyCode, int modifiers) {
                if (keyCode == KEY_ENTER) {
                    doSearch();
                }
            }
        });
        _input.addFocusListener(new FocusListenerAdapter() {
            public void onFocus (Widget sender) {
                if (_input.getText().equals(_cmsgs.searchDefault())) {
                    _input.removeStyleName("Faded");
                    _input.setText("");
                }
            }
        });
        _close = MsoyUI.createCloseButton(new ClickListener() {
            public void onClick (Widget sender) {
                clearSearch(true);
            }
        });
        clearSearch(false);
    }

    /**
     * Creates a click listener that can be added to a "search" button to go along with this box.
     */
    public ClickListener makeSearchListener () {
        return new ClickListener() {
            public void onClick (Widget widget) {
                doSearch();
            }
        };
    }

    /**
     * Configures the contents of the search box.
     */
    public void setText (String text)
    {
        if (text == null || text.length() == 0) {
            clearSearch(false);
        } else {
            _input.removeStyleName("Faded");
            _input.setText(text);
        }
    }

    protected void doSearch ()
    {
        String query = _input.getText().trim();
        if (query.length() == 0) {
            clearSearch(true);
        } else {
            _listener.search(query);
            if (!_close.isAttached()) {
                add(_close);
            }
        }
    }

    protected void clearSearch (boolean informListener)
    {
        _input.setText(_cmsgs.searchDefault());
        _input.addStyleName("Faded");
        _input.setFocus(false);
        if (informListener) {
            _listener.clearSearch();
        }
        if (_close.isAttached()) {
            remove(_close);
        }
    }

    protected Listener _listener;
    protected TextBox _input;
    protected Widget _close;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
