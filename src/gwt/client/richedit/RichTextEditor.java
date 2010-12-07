//
// $Id$

package client.richedit;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.gwt.MessageUtil;

/**
 * HTML message editor implementation using GWT's builtin rich text area with a custom toolbar.
 */
public class RichTextEditor extends FlowPanel
    implements MessageEditor.Panel
{
    public RichTextEditor (boolean enablePanelColor)
    {
        _text = new RichTextArea();
        add(_toolbar = new RichTextToolbar(_text, enablePanelColor));
        add(_text);
        _text.setWidth("100%");
        _text.setHeight("300px");
    }

    @Override // from MessageEditor.Panel
    public Widget asWidget ()
    {
        return this;
    }

    @Override // from MessageEditor.Panel
    public String getHTML ()
    {
        return _text.getHTML();
    }

    @Override // from MessageEditor.Panel
    public void setHTML (String html)
    {
        _text.setHTML(MessageUtil.preEditMessage(html));
    }

    @Override // from MessageEditor.Panel
    public String getPanelColor ()
    {
        return _toolbar.getBackgroundColor();
    }

    @Override // from MessageEditor.Panel
    public void setPanelColor (String color)
    {
        _toolbar.setPanelColors(null, color);
    }

    @Override // from MessageEditor.Panel
    public void setFocus (boolean focus)
    {
        _text.setFocus(focus);
    }

    @Override // from MessageEditor.Panel
    public void selectAll ()
    {
        _text.getFormatter().selectAll();
    }

    @Override // from MessageEditor.Panel
    public Button getToggler ()
    {
        return null;
    }

    protected RichTextArea _text;
    protected RichTextToolbar _toolbar;
}
