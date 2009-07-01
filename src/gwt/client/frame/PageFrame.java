package client.frame;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Frame;

import com.threerings.msoy.web.gwt.Pages;

import com.threerings.msoy.data.all.DeploymentConfig;

import client.util.events.FlashEvent;

/**
 * IFrame that embeds another msoy gwt module determined by a {@link Pages} instance.
 */
public class PageFrame extends Frame
{
    /**
     * Creates a new frame embeddeing the given page and having the given id.
     */
    public PageFrame (Pages page, String id)
    {
        super("/gwt/" + DeploymentConfig.version + "/" + page.getPath() + "/");
        setStyleName("pageIFrame");
        getElement().setAttribute("name", id);
    }

    /**
     * Fowards an event from flash to the embedded page.
     */
    public void forwardEvent (FlashEvent event)
    {
        JavaScriptObject args = JavaScriptObject.createArray();
        event.toJSObject(args);
        forwardEvent(getElement(), event.getEventName(), args);
    }

    /**
     * Sets the token of the embedded page. Normally called when the user navigates between
     * addresses within a page, e.g. from #games to #games-d_n.
     */
    public void setToken (String token)
    {
        setToken(getElement(), token);
    }

    /**
     * Forwards a Flash event to the page frame.
     */
    protected static native void forwardEvent (
        Element frame, String name, JavaScriptObject args) /*-{
        try {
            if (frame.contentWindow && frame.contentWindow.triggerEvent) {
                frame.contentWindow.triggerEvent(name, args);
            }
        } catch (e) {
            if ($wnd.console) {
                $wnd.console.log("Failed to forward event [name=" + name + ", error=" + e + "].");
            }
        }
    }-*/;

    /**
     * Passes a page's current token down into our page frame.
     */
    protected static native void setToken (Element frame, String token) /*-{
        try {
            if (frame.contentWindow.setPageToken) {
                frame.contentWindow.setPageToken(token);
            }
        } catch (e) {
            if ($wnd.console) {
                $wnd.console.log("Failed to set page token [token=" + token + ", error=" + e + "].");
            }
        }
    }-*/;
}
