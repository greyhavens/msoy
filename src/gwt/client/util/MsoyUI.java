//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;

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

//     /**
//      * Creates a pair of submit and cancel buttons in a horizontal row.
//      */
//     public static RowPanel createSubmitCancel (
//         WebContext ctx, PopupPanel popup, ClickListener onSubmit)
//     {
//         RowPanel buttons = new RowPanel();
//         buttons.add(new Button(ctx.cmsgs.submit(), onSubmit));
//         buttons.add(new Button(ctx.cmsgs.cancel(), new ClickListener() {
//             public void onClick (Widget sender) {
//                 box.hide();
//             }
//         }));
//         return buttons;
//     }
}
