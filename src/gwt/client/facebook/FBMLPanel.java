//
// $Id$

package client.facebook;

import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Panel for emulating FBML tags. Functions like a regular flow panel, but uses a <code>fb</code>
 * namespace tag instead of <code>div</code>. When added as a descendant of {@link ServerFBMLPanel},
 * {@link ServerFBMLPanel#reparse()} will cause child tags to be added to this tag to show Facebook
 * content.
 *
 * <p>NOTE: Facebook is very finicky about the correctness of fb namespace tags but does not log
 * any warnings or throw exceptions.</p>
 */
public class FBMLPanel extends FlowPanel
{
    /** The namespace of all facebook markup. */
    public static final String NAMESPACE = "fb:";

    /**
     * Tells Facebook to reparse the DOM tree and instantiate FBML elements beneath an ancestor.
     */
    public static void reparse (Widget ancestor)
    {
        String id = ancestor.getElement().getId();
        if (id == null || id.isEmpty()) {
            ancestor.getElement().setId(id = HTMLPanel.createUniqueId());
        }
        nativeReparse(id);
    }

    /**
     * Creates an <code>fb:name</code> tag for the given user with all of our usual options.
     */
    public static FBMLPanel makeName (long uid)
    {
        return new FBMLPanel("name",
            "uid", String.valueOf(uid),
            "linked", "false",
            "firstnameonly", "true",
            "capitalize", "true");
    }

    /**
     * Creates an <code>fb:profile-pic</code> tag for the given user with all of our usual options.
     */
    public static FBMLPanel makeProfilePic (long uid)
    {
        return new FBMLPanel("profile-pic",
            "uid", String.valueOf(uid),
            "linked", "false",
            "size", "square");
    }

    /**
     * Creates a new fb panel using the given tag and optional attributes. The tag will
     * automatically be prefixed by the namespace. An even number of attribute arguments must be
     * provided: name1, value1, name2, value2, ... No escaping is done on values.
     */
    public FBMLPanel (String tag, String... attrValuePairs)
    {
        setElement(DOM.createElement(NAMESPACE + tag));

        // a little hacky: we need to have <fb:x></fb:x>, not <fb:x/>, so add an empty text node
        getElement().appendChild(Document.get().createTextNode(""));

        for (int ii = 0; ii < attrValuePairs.length; ii += 2) {
            setAttribute(attrValuePairs[ii], attrValuePairs[ii+1]);
        }
    }

    /**
     * Sets an attribute for our tag.
     */
    public void setAttribute (String name, String value)
    {
        getElement().setAttribute(name, value);
    }

    /**
     * Sets the id of our tag.
     */
    public void setId (String id)
    {
        getElement().setId(id);
    }

    /**
     * Reparses this panel. See {@link #reparse(Widget)}.
     */
    public void reparse ()
    {
        reparse(this);
    }

    public static native void nativeReparse (String id) /*-{
        try {
            $wnd.FB_ParseXFBML(id);
        } catch (e) {
            if ($wnd.console) {
                $wnd.console.log("Failed to reparse XFBML [error=" + e + "]");
            }
        }
    }-*/;
}
