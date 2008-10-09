//
// $Id$

package client.ui;

import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * A text box that will only accept numbers.
 */
public class NumberTextBox extends TextBox
{
    public NumberTextBox (boolean allowFloatingPoint)
    {
        this(allowFloatingPoint, 0, 0);
    }

    /**
     * Constructor to set both the visible and maximum lengths to the given value.
     */
    public NumberTextBox (boolean allowFloatingPoint, int length)
    {
        this(allowFloatingPoint, length, length);
    }

    /**
     * @param allowFloatingPoint If true, a single decimal point is part of the allowed character
     * set.  Otherwise, only [0-9]* is accepted.
     */
    public NumberTextBox (final boolean allowFloatingPoint, int maxLength, int visibleLength)
    {
        _allowFloatingPoint = allowFloatingPoint;

        addKeyboardListener(new KeyboardListener() {
            public void onKeyUp (Widget sender, char keyCode, int modifiers) {
                if ((modifiers & KeyboardListener.MODIFIER_SHIFT) != 0 || keyCode > '9' ||
                    keyCode < '0') {
                    String text = getText();
                    boolean foundDecimal = !allowFloatingPoint;
                    for (int ii = 0; ii < text.length(); ii++) {
                        if (text.charAt(ii) > '9' || text.charAt(ii) < '0') {
                            if (text.charAt(ii) == '.' && !foundDecimal) {
                                foundDecimal = true;
                            } else {
                                text = text.substring(0, ii) + text.substring(ii+1);
                                ii--;
                            }
                        }
                    }
                    setText(text);
                }
            }
            public void onKeyPress (Widget sender, char keyCode, int modifiers) { }
            public void onKeyDown (Widget sender, char keyCode, int modifiers) { }
        });

        if (maxLength != 0) {
            setMaxLength(maxLength);
        }
        if (visibleLength != 0) {
            setVisibleLength(visibleLength);
        }
    }

    /**
     * Get the number value for the contents of this box. Returns 0 if the box is empty.
     */
    public Number getValue ()
    {
        String valstr = getText().length() == 0 ? "0" : getText();
        return _allowFloatingPoint ? (Number)(new Double(valstr)) : (Number)(new Integer(valstr));
    }

    protected boolean _allowFloatingPoint;
}
