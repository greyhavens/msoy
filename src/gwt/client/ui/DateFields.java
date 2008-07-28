//
// $Id$

package client.ui;

import java.util.Date;

import client.util.DateUtil;

import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SourcesFocusEvents;
import com.google.gwt.user.client.ui.Widget;

public class DateFields extends HorizontalPanel
    implements SourcesFocusEvents
{
    public DateFields ()
    {
        setStyleName("dateFields");
        setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);

        Label divider;
        add(_month = new ListBox());
        for (int ii = 0; ii < MONTHS.length; ii++) { // TODO: localize
            _month.addItem(MONTHS[ii]);
        }
        _month.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                populateDay(_month.getSelectedIndex());
            }
        });
        add(divider = new Label(" / "));
        divider.setWidth(15 + "px");
        divider.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);

        add(_day = new ListBox());
        populateDay(0);
        add(divider = new Label(" / "));
        divider.setWidth(15 + "px");
        divider.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);

        add(_year = new ListBox());
        int start = DateUtil.getYear(new Date())+1900-MIN_AGE;
        for (int ii = 0; ii < 100-MIN_AGE; ii++) {
            _year.addItem(""+(start-ii));
        }
    }

    /**
     * Sets the date in these fields (year, month, day).
     */
    public void setDate (int[] date)
    {
        for (int ii = 0; ii < _year.getItemCount(); ii++) {
            if (Integer.parseInt(_year.getItemText(ii)) == date[0]) {
                _year.setSelectedIndex(ii);
                break;
            }
        }
        _month.setSelectedIndex(date[1]);
        _day.setSelectedIndex(date[2]-1);
    }

    /**
     * Get the date that has been put into the fields of this DateFields widget as (year, month,
     * day). If one or more of the fields hasn't been filled in properly, this method returns null.
     */
    public int[] getDate ()
    {
        int year = Integer.parseInt(_year.getItemText(_year.getSelectedIndex()));
        return new int[] { year, _month.getSelectedIndex(), _day.getSelectedIndex() + 1 };
    }

    /**
     * Focuses the first of our three list boxes.
     */
    public void setFocus (boolean focus)
    {
        _month.setFocus(focus);
    }

    // from interface SourcesFocusEvents
    public void addFocusListener (FocusListener listener)
    {
        _month.addFocusListener(listener);
        _day.addFocusListener(listener);
        _year.addFocusListener(listener);
    }

    // from interface SourcesFocusEvents
    public void removeFocusListener (FocusListener listener)
    {
        _month.removeFocusListener(listener);
        _day.removeFocusListener(listener);
        _year.removeFocusListener(listener);
    }

    protected void populateDay (int month)
    {
        int selidx = _day.getSelectedIndex();
        _day.clear();
        for (int ii = 0; ii < DAYS[month]; ii++) {
            _day.addItem(""+(ii+1));
        }
        _day.setSelectedIndex(Math.max(0, Math.min(selidx, DAYS[month]-1)));
    }

    protected ListBox _month, _day, _year;

    protected static final String[] MONTHS = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };
    protected static final int[] DAYS = { 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
    protected static final int MIN_AGE = 13;
}
