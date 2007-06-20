//
// $Id$

package client.util.events;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;

import com.threerings.msoy.item.data.all.ItemIdent;

import client.util.FlashClients;

public class FurniChangedEvent extends FlashEvent
{
    /** The name of this event type: defined in RoomController.as. */
    public static final String NAME = "furniChanged";

    // @Override // FlashEvent
    public String getEventName ()
    {
        return NAME;
    }

    // @Override // FlashEvent
    public void readFlashArgs (JavaScriptObject args) 
    {
        JavaScriptObject jsTypes[] = new JavaScriptObject[] { 
            FlashClients.getJavaScriptElement(args, 0), 
            FlashClients.getJavaScriptElement(args, 1) };
        List lists[] = new List[] { _added = new ArrayList(), _removed = new ArrayList() };
        for (int type = 0; type < jsTypes.length; type++) {
            for (int ii = 0; ii < FlashClients.getLength(jsTypes[type]); ii++) {
                JavaScriptObject furni = FlashClients.getJavaScriptElement(jsTypes[type], ii);
                ItemIdent item = new ItemIdent(FlashClients.getByteElement(furni, 0),
                    FlashClients.getIntElement(furni, 1));
                lists[type].add(item);
            }
        }
    }

    // @Override // FlashEvent
    public void notifyListener (FlashEventListener listener)
    {
        if (listener instanceof FurniChangeListener) {
            ((FurniChangeListener) listener).furniChanged(this);
        }
    }

    public List getAddedFurni ()
    {
        return _added;
    }

    public List getRemovedFurni ()
    {
        return _removed;
    }

    protected List _added;
    protected List _removed;
}
