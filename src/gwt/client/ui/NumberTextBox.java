//
// $Id$

package client.ui;

import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
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

        addKeyUpHandler(new KeyUpHandler() {
            public void onKeyUp (KeyUpEvent event) {
                if (event.isShiftKeyDown() ||
                    event.getNativeKeyCode() > '9' || event.getNativeKeyCode() < '0') {
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
        });

        if (maxLength != 0) {
            setMaxLength(maxLength);
        }
        if (visibleLength != 0) {
            setVisibleLength(visibleLength);
        }
    }

    /**
     * Sets the numeric contents of this text box. Passing null will clear the box.
     */
    public void setNumber (Number value)
    {
        setText(value == null ? "" : value.toString());
    }

    /**
     * Get the numberic value of this box. Returns 0 if the box is empty.
     */
    public Number getNumber ()
    {
        String valstr = getText().length() == 0 ? "0" : getText();
        return _allowFloatingPoint ? (Number)(new Double(valstr)) : (Number)(new Integer(valstr));
    }

    protected boolean _allowFloatingPoint;
}
