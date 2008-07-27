//
// $Id$

package client.msgs;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.RichTextArea;

import client.ui.RichTextToolbar;

/**
 * A {@link RichTextArea} with toolbar for editing forum messages.
 */
public class MessageEditor extends FlowPanel
{
    public MessageEditor ()
    {
        _text = new RichTextArea();
        add(new RichTextToolbar(_text, false));
        add(_text);
        _text.setWidth("100%");
        _text.setHeight("300px");
    }

    public RichTextArea getTextArea()
    {
        return _text;
    }

    public String getHTML ()
    {
        return _text.getHTML();
    }

    public void setHTML (String html)
    {
        _text.setHTML(html);
    }

    public void setFocus (boolean focus)
    {
        _text.setFocus(focus);
    }

    protected RichTextArea _text;
}
