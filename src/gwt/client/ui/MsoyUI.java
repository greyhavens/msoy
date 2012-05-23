//
// $Id$

package client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusWidget;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.orth.data.MediaDesc;

import com.threerings.gwt.ui.AbsoluteCSSPanel;
import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.GwtAuthCodes;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.WebCreds;

import client.images.misc.MiscImages;
import client.shell.CShell;
import client.shell.LogonPanel;
import client.shell.ShellMessages;
import client.util.Link;
import client.util.NaviUtil;

/**
 * Contains useful user interface related methods.
 */
public class MsoyUI
{
    /** A button size constant for use with {@link #createButton}. */
    public static final String SHORT_THIN = "shortThin";

    /** A button size constant for use with {@link #createButton}. */
    public static final String MEDIUM_THIN = "mediumThin";

    /** A button size constant for use with {@link #createButton}. */
    public static final String LONG_THIN = "longThin";

    /** A button size constant for use with {@link #createButton}. */
    public static final String SHORT_THICK = "shortThick";

    /** A button size constant for use with {@link #createButton}. */
    public static final String MEDIUM_THICK = "mediumThick";

    /** A button size constant for use with {@link #createButton}. */
    public static final String LONG_THICK = "longThick";

    /** A regexp that matches valid email addresses. Thanks to lambert@nas.nasa.gov. This isn't the
     * ideal place for this but I don't want to create a whole separate utility class just for this
     * one regular expression. */
    public static final String EMAIL_REGEX = "^([-A-Za-z0-9_.!%+]+@" +
        "[-a-zA-Z0-9]+(\\.[-a-zA-Z0-9]+)*\\.[-a-zA-Z0-9]+)$";

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
     * Creates an inline label with the supplied text and style.
     */
    public static InlineLabel createInlineLabel (String text, String styleName)
    {
        InlineLabel label = new InlineLabel(text);
        if (styleName != null) {
            label.setStyleName(styleName);
        }
        return label;
    }

    /**
     * Creates an HTML label with the supplied text and style. <em>Warning:</em> never pass user
     * supplied text into an HTML label. Cross-site-scripting-o-rama!
     */
    public static HTML createHTML (String text, String styleName)
    {
        HTML label = new SafeHTML(text); // SafeHTML is needed to rewrite our hrefs
        if (styleName != null) {
            label.addStyleName(styleName);
        }
        return label;
    }

    /**
     * Creates a SimplePanel with the supplied style and widget
     */
    public static SimplePanel createSimplePanel (Widget widget, String styleName)
    {
        SimplePanel panel = new SimplePanel();
        if (styleName != null) {
            panel.addStyleName(styleName);
        }
        if (widget != null) {
            panel.setWidget(widget);
        }
        return panel;
    }

    /**
     * Creates a FlowPanel with the provided style
     */
    public static FlowPanel createFlowPanel (String styleName, Widget... contents)
    {
        FlowPanel panel = new FlowPanel();
        if (styleName != null) {
            panel.addStyleName(styleName);
        }
        for (Widget child : contents) {
            panel.add(child);
        }
        return panel;
    }

    /**
     * Creates a AbsolutePanel with the supplied style
     */
    public static AbsolutePanel createAbsolutePanel (String styleName)
    {
        AbsolutePanel panel = new AbsolutePanel();
        if (styleName != null) {
            panel.addStyleName(styleName);
        }
        return panel;
    }

    /**
     * Creates a AbsoluteCSSPanel with the supplied style and contents
     */
    public static AbsoluteCSSPanel createAbsoluteCSSPanel (String styleName, Widget... contents)
    {
        AbsoluteCSSPanel panel = new AbsoluteCSSPanel(styleName);
        for (Widget child : contents) {
            panel.add(child);
        }
        return panel;
    }

    /**
     * Creates a label that triggers an action using the supplied text and handler.
     */
    public static Label createActionLabel (String text, ClickHandler onClick)
    {
        return createActionLabel(text, null, onClick);
    }

    /**
     * Creates a label that triggers an action using the supplied text and handler. The label will
     * be styled as specified with an additional style that configures the mouse pointer and adds
     * underline to the text.
     */
    public static Label createActionLabel (String text, String style, ClickHandler onClick)
    {
        Label label = createCustomActionLabel(text, style, onClick);
        if (onClick != null) {
            label.addStyleName("actionLabel");
        }
        return label;
    }

    /**
     * Creates an action label, which when clicked will show a popup containing a label with the
     * given tip. The given style is applied to the contents of the popup for sizing or attaching
     * an image background.
     */
    public static Label createTipper (
        String link, final String tip, final String contentStyle)
    {
        return createActionLabel(link, new ClickHandler() {
            public void onClick (ClickEvent event) {
                FlowPanel content = new FlowPanel();
                content.setStyleName(contentStyle);
                content.add(new Label(tip));
                final InfoPopup popup = new InfoPopup(content);
                popup.show();
            }
        });
    }

    /**
     * Creates a label that triggers an action using the supplied text and listener. The label will
     * only be styled with the specified style.
     */
    public static Label createCustomActionLabel (String text, String style, ClickHandler handler)
    {
        Label label = createLabel(text, style);
        maybeAddClickHandler(label, handler);
        return label;
    }

    /**
     * Escapes an HTML/XML string.
     */
    public static String escapeHTML (String html)
    {
        return html.replaceAll("&", "&amp;")
                   .replaceAll("<", "&lt;")
                   .replaceAll(">", "&gt;")
                   .replaceAll("\"", "&quot;");
    }

    /**
     * Creates a safe, restricted HTML from user input. URLs specifying a *.whirled.com domain are
     * automatically turned into links. Html characters are escaped.
     */
    public static HTML createRestrictedHTML (String html)
    {
        String id = "[-0-9a-zA-Z]+";
        String idd = "(" + id + "\\.)";
        String idds = "(" + idd + "+)";
        String http = "https?://";
        String port = "(:[0-9]+)?";
        String path = "/([^ ]*)";
        html = escapeHTML(html);

        // TODO: java.net.IDN
        html = html.replaceAll("(" + http + "(" + idds + id + port + "(" + path + ")?))",
            "<a href=\"$1\">$2</a>");

        return createHTML(html, null);
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
        box.setMaxLength(maxLength > 0 ? maxLength : 255);
        if (visibleLength > 0) {
            box.setVisibleLength(visibleLength);
        }
        return box;
    }

    public static TextBox createTextBox (String text)
    {
        return createTextBox(text, -1, -1);
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
        if (width > 0) {
            area.setCharacterWidth(width);
        }
        if (height > 0) {
            area.setVisibleLines(height);
        }
        return area;
    }

    /**
     * Creates a text area with a listener and style instead of width/height
     */
    public static TextArea createTextArea (String text, String style, ChangeHandler handler)
    {
        TextArea area = new TextArea();
        if (text != null) {
            area.setText(text);
        }
        if (style != null) {
            area.addStyleName(style);
        }
        if (handler != null) {
            area.addChangeHandler(handler);
        }
        return area;
    }

    /**
     * Creates a PushButton with default(up), mouseover and mousedown states.
     */
    public static PushButton createPushButton (ImageResource defaultImage, ImageResource overImage,
        ImageResource downImage, ClickHandler onClick)
    {
        return createPushButton(new Image(defaultImage), new Image(overImage),
            new Image(downImage), onClick);
    }

    /**
     * Creates a PushButton with default(up), mouseover and mousedown states.
     */
    public static PushButton createPushButton (Image defaultImage, Image overImage,
        Image downImage, ClickHandler onClick)
    {
        PushButton button = new PushButton(defaultImage, downImage, onClick);
        button.getUpHoveringFace().setImage(overImage);
        return button;
    }

    /**
     * Creates an orange button of the specified size with the supplied text.
     */
    public static PushButton createButton (String size, String label, ClickHandler onClick)
    {
        PushButton button = new PushButton(label);
        maybeAddClickHandler(button, onClick);
        button.setStyleName(size + "OrangeButton");
        button.addStyleName("orangeButton");
        return button;
    }

    /**
     * Creates an orange button of the specified size with the supplied text.
     */
    public static PushButton createBlueButton (String size, String label, ClickHandler onClick)
    {
        if (!MEDIUM_THIN.equals(size) && !LONG_THIN.equals(size)) {
            throw new IllegalArgumentException(
                "Blue buttons only come in MEDIUM_THIN and LONG_THIN.");
        }
        PushButton button = new PushButton(label);
        maybeAddClickHandler(button, onClick);
        button.setStyleName(size + "BlueButton");
        button.addStyleName("blueButton");
        return button;
    }

    /**
     * Creates a button with tiny text.
     */
    public static Button createTinyButton (String label, ClickHandler onClick)
    {
        Button button = new Button(label);
        maybeAddClickHandler(button, onClick);
        button.addStyleName("tinyButton");
        return button;
    }

    /**
     * Creates an image button that changes appearance when you click and hover over it.
     */
    public static PushButton createImageButton (String style, ClickHandler onClick)
    {
        PushButton button = new PushButton();
        button.setStyleName(style);
        button.addStyleName("actionLabel");
        maybeAddClickHandler(button, onClick);
        return button;
    }

    /**
     * Creates a button for closing things (a square with an x in it).
     */
    public static PushButton createCloseButton (ClickHandler onClick)
    {
        return createImageButton("closeButton", onClick);
    }

    /**
     * Creates a pair of previous and next buttons in a horizontal panel.
     */
    public static Widget createPrevNextButtons (ClickHandler onPrev, ClickHandler onNext)
    {
        HorizontalPanel panel = new HorizontalPanel();
        panel.setStyleName("pagedGrid"); // hijack PagedGrid styles
        Button prev = new Button(_cmsgs.prev());
        prev.setStyleName("Button");
        prev.addStyleName("PrevButton");
        maybeAddClickHandler(prev, onPrev);
        panel.add(prev);
        panel.add(WidgetUtil.makeShim(5, 5));
        Button next = new Button(_cmsgs.next());
        next.setStyleName("Button");
        next.addStyleName("NextButton");
        maybeAddClickHandler(next, onNext);
        panel.add(next);
        return panel;
    }

    /**
     * Creates a basic text button whose text changes depending on whether we are
     * creating or updating something.
     */
    public static Button createCrUpdateButton (boolean creating, ClickHandler onClick)
    {
        Button button = new Button(creating ? _cmsgs.create() : _cmsgs.update());
        maybeAddClickHandler(button, onClick);
        return button;
    }

    /**
     * Puts a pair buttons in a horizontal panel with a small gap betwixt them.
     * Note: Our standard is that left is the cancel/back/revert, right is the ok/forward/submit.
     */
    public static Widget createButtonPair (Widget left, Widget right)
    {
        HorizontalPanel panel = new HorizontalPanel();
        panel.addStyleName("ButtonRow");
        panel.add(left);
        panel.add(WidgetUtil.makeShim(5, 5));
        panel.add(right);
        return panel;
    }

    /**
     * Creates a widget containing an array of buttons arranged in a row from left to right, with a
     * small gap betwixt each pair. Note: Our standard is that ok goes on the far right, cancel
     * just the left of that, and other buttons farther to the left.
     */
    public static Widget createButtonRow (Widget... contents)
    {
        HorizontalPanel panel = new HorizontalPanel();
        panel.addStyleName("ButtonRow");
        for (int ii = 0; ii < contents.length; ++ii) {
            if (ii > 0) {
                panel.add(WidgetUtil.makeShim(5, 5));
            }
            panel.add(contents[ii]);
        }
        return panel;
    }

    /**
     * Creates an arrow that does History.back().
     */
    public static Image createBackArrow ()
    {
        return createBackArrow(NaviUtil.onGoBack());
    }

    /**
     * Creates an arrow that invokes the specified callback.
     */
    public static Image createBackArrow (ClickHandler callback)
    {
        return createActionImage("/images/ui/back_arrow.png", callback);
    }

    /**
     * Creates an image with the supplied path and style.
     */
    public static Image createImage (String path, String styleName)
    {
        Image image = new Image(path);
        if (styleName != null) {
            image.addStyleName(styleName);
        }
        return image;
    }

    /**
     * Creates an image with the supplied {@link ImageResource} and style.
     */
    public static Image createImage (ImageResource resource, String styleName)
    {
        Image image = new Image(resource);
        if (styleName != null) {
            image.addStyleName(styleName);
        }
        return image;
    }

    /**
     * Creates an image that responds to clicking.
     */
    public static Image createActionImage (String path, ClickHandler onClick)
    {
        return createActionImage(path, null, onClick);
    }

    /**
     * Creates an image that responds to clicking.
     */
    public static Image createActionImage (String path, String tip, ClickHandler onClick)
    {
        return makeActionImage(new Image(path), tip, onClick);
    }

    /**
     * Creates an image that responds to clicking.
     */
    public static Image createActionImage (ImageResource resource, ClickHandler onClick)
    {
        return createActionImage(resource, null, onClick);
    }

    /**
     * Creates an image that responds to clicking.
     */
    public static Image createActionImage (ImageResource resource, String tip, ClickHandler onClick)
    {
        return makeActionImage(new Image(resource), tip, onClick);
    }

    /**
     * Makes an image into one that responds to clicking.
     */
    public static Image makeActionImage (Image image, String tip, ClickHandler onClick)
    {
        if (onClick != null) {
            image.addStyleName("actionLabel");
            image.addClickHandler(onClick);
        }
        if (tip != null) {
            image.setTitle(tip);
        }
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
     * Creates a label that displays the specified role.
     */
    public static Widget createRoleLabel (WebCreds.Role role)
    {
        // subscriber is special
        if (role == WebCreds.Role.SUBSCRIBER) {
            InlineLabel label = new InlineLabel(_cmsgs.roleSubscriber());
            label.addClickHandler(Link.createHandler(Pages.BILLING, "subscribe"));
            label.addStyleName("actionLabel");
            return createFlowPanel("roleLabel", label);
        }

        String roleName;
        switch (role) {
        case ADMIN:
        case MAINTAINER:
            roleName = _cmsgs.roleAdmin();
            break;
        case SUPPORT:
            roleName = _cmsgs.roleSupport();
            break;
        case PERMAGUEST:
            roleName = _cmsgs.roleGuest();
            break;
        default:
            roleName = _cmsgs.roleUser();
            break;
        }
        return createLabel(roleName, "roleLabel");
    }

    /**
     * Creates an icon for the specified role, or null if we don't have one.
     */
    public static Widget createRoleIcon (WebCreds.Role role)
    {
        if (role == null) {
            return null;
        }

        Label icon = null;
        switch (role) {
        case SUBSCRIBER:
            // Club Whirled is gone, but the icons beside player names remain
            icon = new Label();
            icon.addStyleName("clubWhirledIcon");
            break;
        }
        if (icon != null) {
            // Apply common styles
            icon.addStyleName("roleIcon");
            icon.addStyleName("actionLabel");
        }
        return icon;
    }

    /**
     * Wraps the supplied ClickHandler into a new ClickHandler that will track an arbitrary click
     * on any widget during an a/b test and then take the desired action.
     *
     * @param test identifier for the A/B test.
     * @param action identifier for the action to be logged.
     * @param target the click handler to call once we've sent off our tracking action.
     */
    public static ClickHandler makeTestTrackingHandler (
        final String test, final String action, final ClickHandler target)
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                CShell.frame.reportTestAction(test, action);
                if (target != null) {
                    target.onClick(event);
                }
            }
        };
    }

    /**
     * Create a link to an external page
     */
    public static Anchor createExternalAnchor (String url, String title)
    {
        Anchor anchor = new Anchor(url, title, "_blank");
        anchor.addStyleName("external");
        return anchor;
    }

    /**
     * Return an invisible area of a given size with a given click event, used for imagemaps.
     */
    public static Image createInvisiLink (ClickHandler onClick, int width, int height)
    {
        Image image = MsoyUI.createActionImage("/images/whirled/blank.png", onClick);
        image.setWidth(width + "px");
        image.setHeight(height + "px");
        return image;
    }

    /**
     * Displays informational feedback to the user in a non-offensive way.
     */
    public static void info (String message)
    {
        infoAction(message, null, null);
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
     * Displays informational feedback along with an action button which will dismiss the info
     * display and take an action.
     */
    public static void infoAction (String message, String actionLabel, ClickHandler action)
    {
        HorizontalPanel panel = new HorizontalPanel();
        final InfoPopup popup = new InfoPopup(panel);
        ClickHandler hider = new ClickHandler() {
            public void onClick (ClickEvent event) {
                popup.hide();
            }
        };
        panel.add(new Label(message));
        panel.add(WidgetUtil.makeShim(20, 10));
        if (actionLabel != null) {
            Button button = new Button(actionLabel);
            maybeAddClickHandler(button, action);
            button.addClickHandler(hider);
            panel.add(button);
            panel.add(WidgetUtil.makeShim(5, 10));
        }
        popup.show();
    }

    /**
     * Displays a dialogin explaining that the user's session has expired and that they need to
     * login anew.
     */
    public static void showSessionExpired (String message)
    {
        final BorderedPopup popup = new BorderedPopup();
        popup.addStyleName("sessionExpired");
        FlowPanel content = new FlowPanel();
        content.add(MsoyUI.createLabel(message, "Message"));
        content.add(new LogonPanel(LogonPanel.Mode.VERT) {
            @Override protected void didLogon () {
                super.didLogon();
                popup.hide();
            }
        });
        popup.setWidget(content);
        popup.show();
    }

    /**
     * Displays error feedback to the user in a non-offensive way.
     */
    public static void error (String message)
    {
        // TODO: style this differently than info feedback
        new InfoPopup(message).show();
    }

    /**
     * Displays error feedback to the user in a non-offensive way. The error feedback is displayed
     * near the supplied component and if the component supports focus, it is focused.
     */
    public static void errorNear (String message, Widget source)
    {
        if (source instanceof FocusWidget) {
            ((FocusWidget)source).setFocus(true);
        }
        // TODO: style this differently than info feedback
        new InfoPopup(message).showNear(source);
    }

    /**
     * Reports a failure that occurred during a service call by showing a popup with a translated
     * version of the exception text. Also shows a special popup for expired session errors.
     */
    public static void reportServiceFailure (Throwable caught)
    {
        if (GwtAuthCodes.SESSION_EXPIRED.equals(caught.getMessage())) {
            showSessionExpired(CShell.serverError(caught));
        } else {
            error(CShell.serverError(caught));
        }
    }

    /**
     * Truncate a paragraph to the maximum number of full sentences, or to the max number of
     * characters followed by "..."
     */
    public static String truncateParagraph (String text, int maxLen)
    {
        if (text.length() <= maxLen) {
            return text;
        }
        for (int ii = maxLen-1; ii >= 0; ii--) {
            char c = text.charAt(ii);
            if (c == '.' || c == '!') {
                return text.substring(0, ii+1);
            }
        }
        return text.substring(0, maxLen-3) + "...";
    }

    /**
     * Assign a focus listener that will select all the text in the box when focus is gained.
     */
    public static void selectAllOnFocus (TextBoxBase textBox)
    {
        // We need some click logic too because when focus is gained due to a click the selection
        // flashes, disappearing when the cursor appears
        TextBoxSelector listener = new TextBoxSelector();
        textBox.addFocusHandler(listener);
        textBox.addBlurHandler(listener);
        textBox.addClickHandler(listener);
    }

    /**
     * Adds the supplied click listener to the supplied target iff the listener is non-null.
     */
    public static void maybeAddClickHandler (HasClickHandlers target, ClickHandler onClick)
    {
        if (onClick != null) {
            target.addClickHandler(onClick);
        }
    }

    /**
     * Computes the number of rows to display for a paged grid.
     *
     * @param used the number of vertical pixels used by other interface elements on this page.
     * @param perRow the height of each grid row.
     * @param minimum the minimum number of rows.
     */
    public static int computeRows (int used, int perRow, int minimum)
    {
        return Math.max(minimum, (Window.getClientHeight() - used) / perRow);
    }

    /**
     * Creates a button that allows a piece of Whirled content to be shared on Digg, Facebook,
     * MySpace and potentially other handy places.
     *
     * @param page the page being shared.
     * @param args the args to the page being shared.
     * @param what a pre-translationed singular subject like "game" or "avatar", for plugging into
     * "Share this [what]".
     * @param title the title for the share blurb.
     * @param descrip the description for the share blurb.
     * @param image the image to be included in the share blurb.
     */
    public static Widget makeShareButton (Pages page, Args args, String what,
                                          String title, String descrip, MediaDesc image)
    {
        final ShareDialog.Info info = new ShareDialog.Info();
        info.page = page;
        info.args = args;
        info.what = what;
        info.title = title;
        info.descrip = descrip;
        info.image = image;
        return makeShareButton(new ClickHandler() {
            public void onClick (ClickEvent event) {
                new ShareDialog(info).show();
            }
        });
    }

    /**
     * Creates a button that allows a piece of Whirled content to be shared on Digg, Facebook,
     * MySpace and potentially other handy places. The caller is responsible for providing a click
     * handler that creates the share info and calls <code>new ShareDialog(info).show()</code> to
     * display the share dialog.
     */
    public static Widget makeShareButton (final ClickHandler onShare)
    {
        FlowPanel bits = createFlowPanel("shareBox");
        bits.add(createLabel(_cmsgs.share(), null));
        Image share = new Image(_mimgs.share());
        share.addClickHandler(onShare);
        share.addStyleName("actionLabel");
        bits.add(share);
        return bits;
    }

    /**
     * Returns true if the logged on member is registered (not a permaguest), false if they are
     * not. In the false case, a popup will be displayed telling the user that they need to
     * register with a link to the page that allows them to do so.
     */
    public static boolean requireRegistered ()
    {
        if (CShell.isRegistered()) {
            return true;
        }
        infoAction(_cmsgs.requiresRegistered(), _cmsgs.goRegister(), NaviUtil.onMustRegister());
        return false;
    }

    /**
     * Returns true if the logged on member has a validated email address, false if they do not. In
     * the false case, a popup will be displayed telling the user that they need to validate their
     * email address with a link to the page that allows them to do so.
     */
    public static boolean requireValidated ()
    {
        if (CShell.isValidated()) {
            return true;
        } else if (!requireRegistered()) {
            return false;
        } else {
            infoAction(_cmsgs.requiresValidated(), _cmsgs.goValidate(),
                       Link.createHandler(Pages.ACCOUNT, "edit"));
            return false;
        }
    }

    /**
     * Returns a widget that says "Loading..." and displays a large (168x116) swirly image.
     */
    public static Widget createNowLoading ()
    {
        FlowPanel panel = createFlowPanel("nowLoadingWidget");
        panel.add(new Image("/images/ui/loading_globe_notext.gif"));
        panel.add(new Label(_cmsgs.nowLoading()));
        return panel;
    }

    protected static class TextBoxSelector
        implements FocusHandler, ClickHandler, BlurHandler
    {
        public void onFocus (FocusEvent event) {
            _focus = true;
            _click = false;
            TextBoxBase text = ((TextBoxBase)event.getSource());
            text.setSelectionRange(0, text.getText().length());
        }
        public void onBlur (BlurEvent event) {
            _focus = false;
            _click = false;
        }
        public void onClick (ClickEvent event) {
            if (!_click) {
                _click = true;
                TextBoxBase text = ((TextBoxBase)event.getSource());
                text.setSelectionRange(0, text.getText().length());
            }
        }
        protected boolean _focus;
        protected boolean _click;
    }

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final MiscImages _mimgs = GWT.create(MiscImages.class);
}
