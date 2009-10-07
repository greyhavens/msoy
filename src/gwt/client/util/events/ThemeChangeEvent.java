//
// $Id: $

package client.util.events;

import com.google.gwt.core.client.JavaScriptObject;

import client.util.JavaScriptUtil;

public class ThemeChangeEvent extends FlashEvent
{
    /** The name of this event type: defined in WorldClient.as */
    public static final String NAME = "themeChange";

    public interface Listener extends FlashEventListener
    {
        void themeChanged (ThemeChangeEvent event);
    }

    public ThemeChangeEvent ()
    {
    }

    public ThemeChangeEvent (int groupId, String url)
    {
        _groupId = groupId;
        _url = url;
    }

    @Override // from FlashEvent
    public String getEventName ()
    {
        return NAME;
    }

    @Override // from FlashEvent
    public void fromJSObject (JavaScriptObject args)
    {
        _groupId = JavaScriptUtil.getIntElement(args, 0);
        _url = JavaScriptUtil.getStringElement(args, 1);
    }

    @Override // from FlashEvent
    public void toJSObject (JavaScriptObject args)
    {
        JavaScriptUtil.setIntElement(args, 0, _groupId);
        JavaScriptUtil.setStringElement(args, 1, _url);
    }

    @Override // from FlashEvent
    public void notifyListener (FlashEventListener listener)
    {
        if (listener instanceof Listener) {
            ((Listener) listener).themeChanged(this);
        }
    }

    public int getGroupId ()
    {
        return _groupId;
    }

    public String getUrl ()
    {
        return _url;
    }

    protected int _groupId;
    protected String _url;
}
