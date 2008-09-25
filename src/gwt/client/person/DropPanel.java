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
 *
 * @author mjensen
 */
public abstract class DropPanel<T> extends FlowPanel
{
    public DropPanel (PickupDragController dragController, DropModel<T> model)
    {
        addStyleName("dropPanel");
        contentToWidget = new HashMap<T, Widget>();

        _dragController = dragController;
        _dropController = new FlowPanelDropController(this) {
            @Override protected void insert(Widget widget, int beforeIndex) {
                super.insert(widget, beforeIndex);
                try {
                    @SuppressWarnings("unchecked")
                    PayloadWidget<T> payloadWidget = (PayloadWidget<T>) widget;
                    if (!payloadWidget.isPositioner()) {
                        _model.insert(payloadWidget.getPayload(), beforeIndex);
                    }
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

    public PayloadWidget<T> createPayloadWidget (T payload)
    {
        PayloadWidget<T> payloadWidget = new PayloadWidget<T>(createWidget(payload), payload);
        _dragController.makeDraggable(payloadWidget);
        return payloadWidget;
    }

    @Override
    public void add(Widget w)
    {
        super.add(w);
        checkForDuplicates(w);
    }

    @Override
    public void insert(Widget w, int beforeIndex)
    {
        super.insert(w, beforeIndex);
        checkForDuplicates(w);
    }

    protected void checkForDuplicates (Widget widget) {
        // check whether there is already a widget for this content on the panel
        try {
            @SuppressWarnings("unchecked")
            PayloadWidget<T> payloadWidget = (PayloadWidget<T>) widget;
            T payload = payloadWidget.getPayload();
            if (!payloadWidget.isPositioner()) {
                Widget existing = contentToWidget.get(payload);
                if (existing != null && existing != payloadWidget) {
                    remove(existing);
                }
                contentToWidget.put(payload, payloadWidget);
            }
        } catch (ClassCastException lame) {
            // lame version of instanceof
        }
    }

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

    protected abstract Widget createWidget (T element);

    protected DropModel<T> _model;
    protected FlowPanelDropController _dropController;
    protected PickupDragController _dragController;
    protected Map<T, Widget> contentToWidget;
}
