package client.util;

import com.google.gwt.user.client.ui.Label;

/**
 * A small helper class to provide us with an inline label, since the default
 * implementation of Label creates a DIV, and there's no way to make a SPAN.
 */
public class InlineLabel
    extends Label
{
    public InlineLabel ()
    {
        super();
        setStyleName("inline");
    }

    public InlineLabel (String text, boolean wordWrap)
    {
        super(text, wordWrap);
        setStyleName("inline");
    }

    public InlineLabel (String text)
    {
        super(text);
        setStyleName("inline");
    }
}
