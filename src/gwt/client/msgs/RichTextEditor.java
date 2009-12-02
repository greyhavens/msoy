//
// $Id$

package client.msgs;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.gwt.MessageUtil;

import client.item.RichTextToolbar;

/**
 * HTML message editor implementation using GWT's builtin rich text area with a custom toolbar.
 */
public class RichTextEditor extends FlowPanel
    implements MessageEditor.Panel
{
    public RichTextEditor ()
    {
        _text = new RichTextArea();
        add(new RichTextToolbar(_text, false));
        add(_text);
        _text.setWidth("100%");
        _text.setHeight("300px");
    }

    @Override // from MessageEditor
    public Widget asWidget ()
    {
        return this;
    }

    @Override // from MessageEditor
    public String getHTML ()
    {
        return _text.getHTML();
    }

    @Override // from MessageEditor
    public void setHTML (String html)
    {
        _text.setHTML(MessageUtil.preEditMessage(html));
    }

    @Override // from MessageEditor
    public void setFocus (boolean focus)
    {
        _text.setFocus(focus);
    }

    @Override // from MessageEditor
    public void selectAll ()
    {
        _text.getBasicFormatter().selectAll();
    }

    @Override // from MessageEditor
    public Button getToggler ()
    {
        return null;
    }

    protected RichTextArea _text;
}
