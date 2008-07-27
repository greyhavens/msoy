//
// $Id$

package client.ui;

import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.Widget;

/**
 * Displays default text in a text box or area and clears that text out when the box is focused.
 */
public class DefaultTextListener
    implements FocusListener
{
    public static void configure (TextBoxBase target, String defaultText)
    {
        DefaultTextListener listener = new DefaultTextListener(target, defaultText);
        target.addFocusListener(listener);
    }

    // from interface FocusListener
    public void onFocus (Widget widget)
    {
        if (_target.getText().equals(_defaultText)) {
            _target.setText("");
        }
    }

    // from interface FocusListener
    public void onLostFocus (Widget sender)
    {
        if (_target.getText().trim().equals("")) {
            _target.setText(_defaultText);
        }
    }

    protected DefaultTextListener (TextBoxBase target, String defaultText)
    {
        _target = target;
        _defaultText = defaultText;
        _target.setText(defaultText);
    }

    protected TextBoxBase _target;
    protected String _defaultText;
}
