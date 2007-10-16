//
// $Id$

package client.util;

import java.util.Date;

import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

public class DateFields extends HorizontalPanel
{
    public static Date toDate (int[] datevec)
    {
        return new Date(datevec[0] - 1900, datevec[1], datevec[2]);
    }

    public DateFields () 
    {
        setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);

        FocusListener focusListener = new FocusListener() {
            public void onFocus (Widget sender) {
                if (sender == _day && "DD".equals(_day.getText())) {
                    _day.setText("");
                } else if (sender == _year && "YYYY".equals(_day.getText())) {
                    _year.setText("");
                }
            }
            public void onLostFocus (Widget sender) {
                if (sender == _day && "".equals(_day.getText())) {
                    _day.setText("DD");
                } else if (sender == _year && "".equals(_year.getText())) {
                    _year.setText("YYYY");
                }
            }
        };

        Label divider;
        add(_month = new ListBox());
        for (int ii = 0; ii < MONTHS.length; ii++) { // TODO: localize
            _month.addItem(MONTHS[ii]);
        }
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
     * Sets the date in these fields (year, month, day).
     */
    public void setDate (int[] date)
    {
        _year.setText("" + date[0]);
        _month.setSelectedIndex(date[1]);
        _day.setText("" + date[2]);
    }

    /**
     * Get the date that has been put into the fields of this DateFields widget as (year, month,
     * day). If one or more of the fields hasn't been filled in properly, this method returns null.
     */
    public int[] getDate () 
    {
        try {
            int day = _day.getValue().intValue();
            int year = _year.getValue().intValue();
            if (day > 0 && day <= 31 && year >= 0) {
                return new int[] { year, _month.getSelectedIndex(), day };
            }
        } catch (NumberFormatException nfe) {
            // let us return null in this case
        }
        return null;
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

    protected ListBox _month;
    protected NumberTextBox _day;
    protected NumberTextBox _year;

    protected static final String[] MONTHS = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };
}
