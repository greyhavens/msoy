//
// $Id$

package client.ui;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.impl.PopupImpl;
import com.google.gwt.user.client.ui.impl.PopupImplMozilla;

/**
 * Mozilla implementation of {@link PopupImpl} that does the right thing with Flash and other
 * non-well-behaved layers.
 */
public class PopupImplMozillaHack extends PopupImplMozilla
{
    @Override // from PopupImpl
    public void onShow (Element popup)
    {
        super.onShow(popup);
        createIFrame(popup);
    }

    @Override // from PopupImpl
    public void onHide (Element popup)
    {
        super.onHide(popup);
        clearIFrame(popup);
    }

    protected native void createIFrame (Element popup) /*-{
        var frame = $doc.createElement('iframe');
        frame.scrolling = 'no';
        frame.frameBorder = 0;
        frame.style.position = 'absolute';
        frame.style.display = 'block';

        // stick these way up above everything else just for safety
        popup.style.zIndex = 2000;
        frame.style.zIndex = 1999;

        popup.__frame = frame;
        frame.style.left = popup.offsetLeft + "px";
        frame.style.top = popup.offsetTop + "px";
        frame.style.width = popup.offsetWidth + "px";
        frame.style.height = popup.offsetHeight + "px";
        popup.parentNode.insertBefore(frame, popup);
    }-*/;

    protected native void clearIFrame (Element popup) /*-{
        var frame = popup.__frame;
        if (frame) {
            $doc.body.removeChild(frame);
            popup.__frame = null;
        }
    }-*/;
}
