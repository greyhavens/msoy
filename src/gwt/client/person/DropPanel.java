//
// $Id$

package client.person;

import client.shell.CShell;

import com.allen_sauer.gwt.dnd.client.DragContext;
import com.allen_sauer.gwt.dnd.client.PickupDragController;
import com.allen_sauer.gwt.dnd.client.drop.FlowPanelDropController;
import com.allen_sauer.gwt.dnd.client.util.LocationWidgetComparator;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 *
 * @author mjensen
 */
public abstract class DropPanel<T> extends FlowPanel
{
    public DropPanel (PickupDragController dragController, DropModel<T> model)
    {
        addStyleName("dropPanel");

        _dragController = dragController;
        _dropController = new FlowPanelDropController(this) {
            @Override protected void insert(Widget widget, int beforeIndex) {
                CShell.log("insert: "+widget+" at "+beforeIndex);
                super.insert(widget, beforeIndex);
                try {
                    @SuppressWarnings("unchecked")
                    PayloadWidget<T> payloadWidget = (PayloadWidget<T>) widget;
                    _model.insert(payloadWidget.getPayload(), beforeIndex);
                } catch (ClassCastException ccex) {
                    // the dropped widget wasn't of type PayloadWidget<T>
                }
            }
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

    public boolean canDrop (Widget widget)
    {
        try {
            @SuppressWarnings({"unused", "unchecked"})
            PayloadWidget<T> test = (PayloadWidget<T>) widget;
            return true;
        } catch (ClassCastException ccex) {
            // the dropped widget wasn't of type PayloadWidget<T>
        }
        return false;
    }

    public PayloadWidget<T> createPayloadWidget (T payload)
    {
        PayloadWidget<T> payloadWidget = new PayloadWidget<T>(createWidget(payload), payload);
        _dragController.makeDraggable(payloadWidget);
        return payloadWidget;
    }

    protected abstract Widget createWidget (T element);

    protected Widget createPositioner (DragContext context)
    {
        try {
            @SuppressWarnings("unchecked")
            PayloadWidget<T> payload = (PayloadWidget<T>) context.draggable;
            payload = createPayloadWidget(payload.getPayload());
            payload.setPositioner(true);
            return payload;
        } catch (ClassCastException ccex) {
            // lame version of instanceof
        }
        return context.draggable;
    }

    protected DropModel<T> _model;
    protected FlowPanelDropController _dropController;
    protected PickupDragController _dragController;
}
