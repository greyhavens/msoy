//
// $Id$

package client.person;

import java.util.HashMap;
import java.util.Map;

import com.allen_sauer.gwt.dnd.client.DragContext;
import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.allen_sauer.gwt.dnd.client.drop.FlowPanelDropController;
import com.allen_sauer.gwt.dnd.client.util.LocationWidgetComparator;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * A panel that allows PayloadWidgets to get dropped and dragged around in order to add, remove,
 * and rearrange the contents of a model.
 *
 * @author mjensen
 */
public abstract class DropPanel<T> extends FlowPanel
{
    public DropPanel (PickupDragController dragController, DropModel<T> model)
    {
        addStyleName("dropPanel");
        _contentToWidget = new HashMap<T, Widget>();

        _dragController = dragController;
        _dropController = new FlowPanelDropController(this) {
            @Override protected Widget newPositioner(DragContext context) {
                return DropPanel.this.createPositioner(context);
            }
            @Override protected LocationWidgetComparator getLocationWidgetComparator() {
              return LocationWidgetComparator.RIGHT_HALF_COMPARATOR;
            }
        };
        _dragController.registerDropController(_dropController);

        // add existing content
        _model = model;
        for (T element : _model.getContents()) {
            add(createPayloadWidget(element));
        }
    }

    public PayloadWidget<T> createPayloadWidget (T payload)
    {
        PayloadWidget<T> payloadWidget = new PayloadWidget<T>(this, createWidget(payload), payload);
        _dragController.makeDraggable(payloadWidget);
        return payloadWidget;
    }

    @Override
    public void add(Widget widget)
    {
        super.add(widget);
        checkForDuplicates(widget);
    }

    @Override
    public void insert(Widget widget, int beforeIndex)
    {
        T payload = null;
        PayloadWidget<T> payloadWidget = getPayloadWidget(widget);
        if (payloadWidget != null && !payloadWidget.isPositioner()) {
            payload = payloadWidget.getPayload();
            if (payloadWidget.getSource() != DropPanel.this) {
                // This payload has been dropped from elsewhere. Make sure to use our
                // custom widget when adding and displaying it on this panel.
                widget = createPayloadWidget(payload);
            }
        }
        super.insert(widget, beforeIndex);
        checkForDuplicates(widget);
        // the model needs to get updated after the widget is inserted
        if (payload != null) {
            _model.insert(payload, beforeIndex);
        }
    }

    @Override
    public boolean remove(Widget widget)
    {
        PayloadWidget<T> payloadWidget = getPayloadWidget(widget);
        if (payloadWidget != null && !payloadWidget.isPositioner()) {
            T payload = payloadWidget.getPayload();
            _model.remove(payload);
            _contentToWidget.remove(payload);
        }
        return super.remove(widget);
    }

    /**
     * This checks with the model to see if it allows duplicate entries. If not, this makes sure
     * that more than one Widget representing the same item is added to this panel.
     */
    protected void checkForDuplicates (Widget widget)
    {
        if (!_model.allowsDuplicates()) {
            // check whether there is already a widget for this content on the panel
            PayloadWidget<T> payloadWidget = getPayloadWidget(widget);
            if (payloadWidget != null) {
                T payload = payloadWidget.getPayload();
                if (!payloadWidget.isPositioner()) {
                    Widget existing = _contentToWidget.get(payload);
                    if (existing != null && existing != payloadWidget) {
                        remove(existing);
                    }
                    _contentToWidget.put(payload, payloadWidget);
                }
            }
        }
    }

    /**
     * Creates the positioner widget used to indicate where the draggable widget will get dropped.
     */
    protected Widget createPositioner (DragContext context)
    {
        PayloadWidget<T> payload = getPayloadWidget(context.draggable);
        if (payload != null) {
            payload = createPayloadWidget(payload.getPayload());
            payload.setPositioner(true);
            return payload;
        }
        return context.draggable;
    }

    /**
     * Attempts to get the given widget cast as a PayloadWidget<T>. Returns null if the given
     * widget is not a PayloadWidget<T>.
     */
    protected PayloadWidget<T> getPayloadWidget (Widget widget)
    {
        try {
            @SuppressWarnings("unchecked")
            PayloadWidget<T> payload = (PayloadWidget<T>) widget;
            return payload;
        } catch (ClassCastException ccex) {
            // lame version of instanceof
        }
        return null;
    }

    /**
     * Creates the widget used to display the given content. This will get added to a PayloadWidget
     * to be dragged around.
     */
    protected abstract Widget createWidget (T content);

    protected DropModel<T> _model;
    protected FlowPanelDropController _dropController;
    protected PickupDragController _dragController;
    protected Map<T, Widget> _contentToWidget;
}
