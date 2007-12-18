//
// $Id$

package client.msgs;

import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.VerticalPanel;

import client.util.RichTextToolbar;

/**
 * A {@link RichTextArea} with toolbar for editing forum messages.
 */
public class MessageEditor extends VerticalPanel
{
    public MessageEditor ()
    {
        _text = new RichTextArea();
        RichTextToolbar toolbar = new RichTextToolbar(_text, false);
        add(toolbar);
        add(_text);
        _text.setWidth("680px");
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
