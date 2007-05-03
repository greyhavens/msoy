//
// $Id$

package client.util;

import java.util.Date;

import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class DateFields extends HorizontalPanel
{
    public DateFields () 
    {
        setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);

        FocusListener focusListener = new FocusListener() {
            public void onFocus (Widget sender) {
                if (sender == _month && "MM".equals(_month.getText())) {
                    _month.setText("");
                } else if (sender == _day && "DD".equals(_day.getText())) {
                    _day.setText("");
                } else if (sender == _year && "YYYY".equals(_day.getText())) {
                    _year.setText("");
                }
            }
            public void onLostFocus (Widget sender) {
                if (sender == _month && "".equals(_month.getText())) {
                    _month.setText("MM");
                } else if (sender == _day && "".equals(_day.getText())) {
                    _day.setText("DD");
                } else if (sender == _year && "".equals(_year.getText())) {
                    _year.setText("YYYY");
                }
            }
        };

        Label divider;
        add(_month = new NumberTextBox(false, 2));
        _month.setText("MM");
        _month.addFocusListener(focusListener);
        add(divider = new Label(" / "));
        divider.setWidth(15 + "px");
        divider.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        add(_day = new NumberTextBox(false, 2));
        _day.setText("DD");
        _day.addFocusListener(focusListener);
        add(divider = new Label(" / "));
        divider.setWidth(15 + "px");
        divider.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        add(_year = new NumberTextBox(false, 4));
        _year.setText("YYYY");
        _year.addFocusListener(focusListener);
    }

    /**
     * Get the date that has been put into the fields of this DateFields widget.  If one or more
     * of the fields hasn't been filled in properly, this method returns null.
     */
    public Date getDate () 
    {
        Date date = null;
        try {
            int month = _month.getValue().intValue();
            int day = _day.getValue().intValue();
            int year = _year.getValue().intValue() - 1900;
            if (month > 0 && month <= 12 && day > 0 && day <= 31 && year >= 0) {
                date = new Date(year, month-1, day);
            }
        } catch (NumberFormatException nfe) {
            // let us return null in this case
        }
        return date;
    }

    /**
     * Adds the given keyboard listener to all of the internal fields.  
     */
    public void addKeyboardListenerToFields (KeyboardListener listener) 
    {
        _month.addKeyboardListener(listener);
        _day.addKeyboardListener(listener);
        _year.addKeyboardListener(listener);
    }

    NumberTextBox _month;
    NumberTextBox _day;
    NumberTextBox _year;
}
