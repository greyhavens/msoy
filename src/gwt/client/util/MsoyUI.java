//
// $Id$

package client.util;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import client.shell.CShell;

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
        if (styleName != null) {
            label.setStyleName(styleName);
        }
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
        Label label = createLabel(text, style);
        if (listener != null) {
            label.addClickListener(listener);
        }
        return label;
    }

    /**
     * Creates a text box with all of the configuration that you're bound to want to do.
     */
    public static TextBox createTextBox (String text, int maxLength, int visibleLength)
    {
        TextBox box = new TextBox();
        if (text != null) {
            box.setText(text);
        }
        if (maxLength > 0) {
            box.setMaxLength(maxLength);
        }
        if (visibleLength > 0) {
            box.setVisibleLength(visibleLength);
        }
        return box;
    }

    /**
     * Creates a text area with all of the configuration that you're bound to want to do.
     */
    public static TextArea createTextArea (String text, int width, int height)
    {
        TextArea area = new TextArea();
        if (text != null) {
            area.setText(text);
        }
        area.setCharacterWidth(width);
        area.setVisibleLines(height);
        return area;
    }

    /**
     * Creates a button with tiny text.
     */
    public static Button createTinyButton (String label, ClickListener listener)
    {
        Button button = new Button(label, listener);
        button.addStyleName("tinyButton");
        return button;
    }

    /**
     * Creates a button with big text.
     */
    public static Button createBigButton (String label, ClickListener listener)
    {
        Button button = new Button(label, listener);
        button.addStyleName("bigButton");
        return button;
    }

    /**
     * Creates an arrow that does History.back().
     */
    public static Image createBackArrow ()
    {
        return createBackArrow(new ClickListener() {
            public void onClick (Widget sender) {
                History.back();
            }
        });
    }

    /**
     * Creates an arrow that invokes the specified callback.
     */
    public static Image createBackArrow (ClickListener callback)
    {
        return createActionImage("/images/ui/back_arrow.png", callback);
    }

    /**
     * Creates an image that responds to clicking.
     */
    public static Image createActionImage (String path, ClickListener onClick)
    {
        Image image = new Image(path);
        image.addStyleName("actionLabel");
        image.addClickListener(onClick);
        return image;
    }

    /**
     * Creates an image that will render inline with text (rather than forcing a break).
     */
    public static Image createInlineImage (String path)
    {
        Image image = new Image(path);
        image.setStyleName("inline");
        return image;
    }

    /**
     * Creates a box with a starry header.
     */
    public static Widget createBox (String styleName, String title, Widget contents)
    {
        VerticalPanel box = new VerticalPanel();
        makeBox(box, styleName, title);
        box.add(contents);
        return box;
    }

    /**
     * Adds a box header to and styles the supplied vertical panel.
     */
    public static void makeBox (VerticalPanel box, String styleName, String title)
    {
        box.addStyleName(styleName + "Box");
        HorizontalPanel header = new HorizontalPanel();
        header.setStyleName("Header");
        header.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        header.add(MsoyUI.createLabel("", "HeaderLeft"));
        Label tlabel = createLabel(title, "HeaderCenter");
        header.add(tlabel);
        header.setCellWidth(tlabel, "100%");
        header.add(createLabel("", "HeaderRight"));
        box.add(header);
    }

    /**
     * Displays informational feedback to the user in a non-offensive way.
     */
    public static void info (String message)
    {
        infoAction(message, null, null);
    }

    /**
     * Displays informational feedback along with an action button which will dismiss the info
     * display and take an action.
     */
    public static void infoAction (String message, String actionLabel, ClickListener action)
    {
        HorizontalPanel panel = new HorizontalPanel();
        final InfoPopup popup = new InfoPopup(panel);
        ClickListener hider = new ClickListener() {
            public void onClick (Widget sender) {
                popup.hide();
            }
        };
        panel.add(new Label(message));
        panel.add(WidgetUtil.makeShim(20, 10));
        if (actionLabel != null) {
            Button button = new Button(actionLabel, action);
            button.addClickListener(hider);
            panel.add(button);
            panel.add(WidgetUtil.makeShim(5, 10));
        }
        panel.add(new Button(CShell.cmsgs.dismiss(), hider));
        popup.show();
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
