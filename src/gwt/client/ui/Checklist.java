//
// $Id$

package client.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.SmartTable;

/**
 * Table that displays a list of items with a checkbox to their left.  The first row of the table
 * contains a "Select All" checkbox which will select or deselect all items in the table.
 *
 * To use the checklist, you must implement the {@link #createWidgetFor(Object)} method, which
 * creates the widget to display an item next to the check.  To retrieve all currently selected
 * data items, call {@link #getSelectedSet()}.
 *
 * @author Kyle Sampson <kyle@threerings.net>
 *
 * @param <T> Type of the data associated with each item in the list.  Note that equals / hashCode
 */
public abstract class Checklist<T> extends SmartTable
{
    /**
     * Constructs a checklist for the given items.
     */
    public Checklist (Collection<T> items)
    {
        super("checklist", 0, 0);
        _items = items;

        // Select all checkbox
        final CheckBox selectAll = new CheckBox();
        selectAll.setChecked(true);
        selectAll.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                selectAll(selectAll.isChecked());
            }
        });
        setWidget(0, 0, selectAll, 1, "checklist-header");
        setText(0, 1, "Select All", 1, "checklist-header");
        setRowEventListener(getRowFormatter().getElement(0), new EventListener() {
            public void onBrowserEvent (Event event) {
                if (DOM.eventGetType(event) == Event.ONCLICK) {
                    selectAll.setChecked(!selectAll.isChecked());
                    selectAll(selectAll.isChecked());
                }
            }
        });

        // Add a row for each items given.
        int row = 1;
        for (final T item : items) {
            final CheckBox check = new CheckBox();
            check.setChecked(true);
            check.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    setItemSelected(item, check.isChecked());
                }
            });
            _checks.add(check);
            _selectedItems.add(item);
            setWidget(row, 0, check, 1, "checklist-check");
            setWidget(row, 1, createWidgetFor(item), 1, "checklist-widget");
            final Element rowElement = getRowFormatter().getElement(row);
            final String originalStyleClass = rowElement.getClassName();
            rowElement.setClassName(originalStyleClass + " checklist-notSelected");
            setRowEventListener(rowElement, new EventListener() {
                public void onBrowserEvent (Event event) {
                    if (DOM.eventGetType(event) == Event.ONCLICK) {
                        check.setChecked(!check.isChecked());
                        setItemSelected(item, check.isChecked());
                    } else if (DOM.eventGetType(event) == Event.ONMOUSEOVER) {
                        rowElement.setClassName(originalStyleClass + " checklist-selected");
                    } else if (DOM.eventGetType(event) == Event.ONMOUSEOUT) {
                        rowElement.setClassName(originalStyleClass + " checklist-notSelected");
                    }
                }
            });
            row++;
        }
    }

    /**
     * Returns the set of data items that have been selected.
     */
    public Set<T> getSelectedSet ()
    {
        return _selectedItems;
    }

    protected void selectAll (boolean selected)
    {
        for (CheckBox check : _checks) {
            check.setChecked(selected);
        }
        if (selected) {
            _selectedItems.addAll(_items);
        } else {
            _selectedItems.clear();
        }
    }

    protected void setItemSelected (T contact, boolean selected)
    {
        if (selected) {
            _selectedItems.add(contact);
        } else {
            _selectedItems.remove(contact);
        }
    }

    protected void setRowEventListener (final Element rowElement, final EventListener listener)
    {
        DOM.sinkEvents(rowElement, Event.ONCLICK | Event.MOUSEEVENTS);
        DOM.setEventListener(rowElement, new EventListener() {
            public void onBrowserEvent (Event event) {
                if (event.getTarget().getId().equals(rowElement.getId())) {
                    listener.onBrowserEvent(event);
                }
            }
        });
    }

    protected abstract Widget createWidgetFor (T item);

    protected final Collection<T> _items;
    protected final List<CheckBox> _checks = new ArrayList<CheckBox>();
    protected final Set<T> _selectedItems = new HashSet<T>();
}
