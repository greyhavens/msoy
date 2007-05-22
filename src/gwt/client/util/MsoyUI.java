//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

/**
 * Contains useful user interface related methods.
 */
public class MsoyUI
{
    /**
     * Creates a label with the supplied text and style.
     */
    public static Label createLabel (String text, String styleName)
    {
        Label label = new Label(text);
        label.setStyleName(styleName);
        return label;
    }

    /**
     * Creates a label that triggers an action using the supplied text and listener.
     */
    public static Label createActionLabel (String text, ClickListener listener)
    {
        return createActionLabel(text, null, listener);
    }

    /**
     * Creates a label that triggers an action using the supplied text and listener. The label will
     * be styled as specified with an additional style that configures the mouse pointer and adds
     * underline to the text.
     */
    public static Label createActionLabel (String text, String style, ClickListener listener)
    {
        Label label = createCustomActionLabel(text, style, listener);
        label.addStyleName("actionLabel");
        return label;
    }

    /**
     * Creates a label that triggers an action using the supplied text and listener. The label will
     * only be styled with the specified style.
     */
    public static Label createCustomActionLabel (String text, String style, ClickListener listener)
    {
        Label label = new Label(text);
        if (style != null) {
            label.setStyleName(style);
        }
        label.addClickListener(listener);
        return label;
    }

    /**
     * Displays informational feedback to the user in a non-offensive way.
     */
    public static void info (String message)
    {
        new InfoPopup(message).show();
    }

    /**
     * Displays informational feedback to the user next to the supplied widget in a non-offensive
     * way.
     */
    public static void infoNear (String message, Widget source)
    {
        new InfoPopup(message).showNear(source);
    }

    /**
     * Displays error feedback to the user in a non-offensive way.
     */
    public static void error (String message)
    {
        // TODO: style this differently than info feedback
        new InfoPopup(message).show();
    }

//     /**
//      * Creates a pair of submit and cancel buttons in a horizontal row.
//      */
//     public static RowPanel createSubmitCancel (
//         PopupPanel popup, ClickListener onSubmit)
//     {
//         RowPanel buttons = new RowPanel();
//         buttons.add(new Button(CShell.cmsgs.submit(), onSubmit));
//         buttons.add(new Button(CShell.cmsgs.cancel(), new ClickListener() {
//             public void onClick (Widget sender) {
//                 box.hide();
//             }
//         }));
//         return buttons;
//     }
}
