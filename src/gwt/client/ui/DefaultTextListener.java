//
// $Id$

package client.ui;

import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.user.client.ui.TextBoxBase;

/**
 * Displays default text in a text box or area and clears that text out when the box is focused.
 */
public class DefaultTextListener
    implements FocusHandler, BlurHandler
{
    public static void configure (TextBoxBase target, String defaultText)
    {
        DefaultTextListener listener = new DefaultTextListener(target, defaultText);
        target.addFocusHandler(listener);
        target.addBlurHandler(listener);
    }

    // from interface FocusHandler
    public void onFocus (FocusEvent event)
    {
        if (_target.getText().equals(_defaultText)) {
            _target.setText("");
        }
    }

    // from interface BlurHandler
    public void onBlur (BlurEvent event)
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
