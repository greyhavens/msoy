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
    public PayloadWidget (Widget realWidget, T payload)
    {
        addStyleName("payload");
        add(realWidget);
        _payload = payload;
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
     * The data object that this widget is lugging around.
     */
    protected T _payload;

    /**
     * Indicates that this widget is just a placeholder and should not change the underlying model
     * when added to a DropPanel.
     */
    protected boolean _positioner;
}
