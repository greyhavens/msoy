//
// $Id$

package client.person;

import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * This Widget is dragged and dropped on the DropPanel. It carries payload data that is used to
 * update the model.
 *
 * @author mjensen
 */
public class PayloadWidget<T> extends FocusPanel
{
    public PayloadWidget (Widget source, Widget realWidget, T payload)
    {
        addStyleName("payload");
        add(realWidget);
        _payload = payload;
        _source = source;
    }

    public T getPayload ()
    {
        return _payload;
    }

    public boolean isPositioner ()
    {
        return _positioner;
    }

    public void setPositioner (boolean positioner)
    {
        this._positioner = positioner;
        if (_positioner) {
            addStyleName("Positioner");
        } else {
            removeStyleName("Positioner");
        }
    }

    /**
     * Gets the panel from which this widget was dragged.
     */
    public Widget getSource ()
    {
        return _source;
    }

    public void setSource (Widget source)
    {
        _source = source;
    }

    /**
     * The data object that this widget is lugging around.
     */
    protected T _payload;

    /**
     * Indicates that this widget is just a placeholder and should not change the underlying model
     * when added to a DropPanel.
     */
    protected boolean _positioner;

    /**
     * This is a reference to the parent widget where this widget originated.
     */
    protected Widget _source;
}
