//
// $Id$

package client.ui;

import java.util.Date;

import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasAllFocusHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

import com.threerings.gwt.util.DateUtil;

public class DateFields extends HorizontalPanel
    implements HasAllFocusHandlers
{
    public DateFields ()
    {
        this(-100, -MIN_AGE);
    }

    /**
     * Create new date fields showing all years between those given. The years are relative to the
     * current year.
     */
    public DateFields (int year0, int yearN)
    {
        setStyleName("dateFields");
        setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);

        Label divider;
        add(_month = new ListBox());
        for (int ii = 0; ii < MONTHS.length; ii++) { // TODO: localize
            _month.addItem(MONTHS[ii]);
        }
        _month.addChangeHandler(new ChangeHandler() {
            public void onChange (ChangeEvent event) {
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
        int start = DateUtil.getYear(new Date())+1900+yearN;
        for (int ii = 0; ii < yearN-year0; ii++) {
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

    // from interface HasAllFocusHandlers
    public HandlerRegistration addFocusHandler (FocusHandler handler)
    {
        return new AggregatedHandlerRegistration (new HandlerRegistration[] {
            _month.addFocusHandler(handler),
            _day.addFocusHandler(handler),
            _year.addFocusHandler(handler)
        });
    }

    // from interface HasAllFocusHandlers
    public HandlerRegistration addBlurHandler (BlurHandler handler)
    {
        return new AggregatedHandlerRegistration (new HandlerRegistration[] {
            _month.addBlurHandler(handler),
            _day.addBlurHandler(handler),
            _year.addBlurHandler(handler)
        });
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

    protected static class AggregatedHandlerRegistration
        implements HandlerRegistration
    {
        public AggregatedHandlerRegistration (HandlerRegistration[] others)
        {
            _others = others;
        }

        public void removeHandler ()
        {
            for (HandlerRegistration other : _others) {
                other.removeHandler();
            }
        }

        HandlerRegistration[] _others;
    }

    protected ListBox _month, _day, _year;

    protected static final String[] MONTHS = {
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    };
    protected static final int[] DAYS = { 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
    protected static final int MIN_AGE = 13;
}
