//
// $Id$

package client.ui;

import com.google.gwt.user.client.ui.FlowPanel;

import client.util.PageCallback;

/**
 * A panel that displays some data after loading.
 */
public abstract class DataPanel<T> extends FlowPanel
{
    protected DataPanel (String styleName)
    {
        setStyleName(styleName);
        add(MsoyUI.createNowLoading());
    }

    protected abstract void init (T data);

    protected void addNoDataMessage (String message)
    {
        add(MsoyUI.createLabel(message, "infoLabel"));
    }

    protected PageCallback<T> createCallback ()
    {
        return new PageCallback<T>(this) {
            public void onSuccess (T data) {
                clear();
                init(data);
            }
        };
    }
}
