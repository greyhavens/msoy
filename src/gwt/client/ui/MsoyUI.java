//
// $Id$

package client.ui;

import java.util.Date;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Anchor;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.client.WebMemberService;
import com.threerings.msoy.web.client.WebMemberServiceAsync;
import com.threerings.msoy.web.data.SessionData;

import client.shell.LogonPanel;
import client.shell.ShellMessages;
import client.shell.TrackingCookie;
import client.util.DateUtil;
import client.util.ServiceUtil;

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
     * Creates a label of the form "9:15am". TODO: support 24 hour time for people who go for that
     * sort of thing.
     */
    public static String formatTime (Date date)
    {
        return _tfmt.format(date).toLowerCase();
    }

    /**
     * Formats the supplied date relative to the current time: Today, Yesterday, MMM dd, and
     * finally MMM dd, YYYY.
     */
    public static String formatDate (Date date)
    {
        return formatDate(date, true);
    }

    /**
     * Formats the supplied date relative to the current time: Today, Yesterday, MMM dd, and
     * finally MMM dd, YYYY.
     *
     * @param useShorthand if false, "Today" and "Yesterday" will not be used, only the month/day
     * and month/day/year formats.
     */
    public static String formatDate (Date date, boolean useShorthand)
    {
        Date now = new Date();
        if (DateUtil.getYear(date) != DateUtil.getYear(now)) {
            return _yfmt.format(date);

        } else if (DateUtil.getMonth(date) != DateUtil.getMonth(now)) {
            return _mfmt.format(date);

        } else if (useShorthand && DateUtil.getDayOfMonth(date) == DateUtil.getDayOfMonth(now)) {
            return _cmsgs.today();

        // this will break for one hour on daylight savings time and we'll instead report the date
        // in MMM dd format or we'll call two days ago yesterday for that witching hour; we don't
        // have excellent date services in the browser, so we're just going to be OK with that
        } else if (useShorthand && DateUtil.getDayOfMonth(date) ==
                   DateUtil.getDayOfMonth(new Date(now.getTime()-24*60*60*1000))) {
            return _cmsgs.yesterday();

        } else {
            return _mfmt.format(date);
        }
    }

    /**
     * Creates a label of the form "{@link #formatDate} at {@link #formatTime}".
     */
    public static String formatDateTime (Date date)
    {
        return _cmsgs.dateTime(formatDate(date), formatTime(date));
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
    public static SimplePanel createSimplePanel (String styleName, Widget widget)
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
    public static FlowPanel createFlowPanel (String styleName)
    {
        FlowPanel panel = new FlowPanel();
        if (styleName != null) {
            panel.addStyleName(styleName);
        }
        return panel;
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
        if (listener != null) {
            label.addStyleName("actionLabel");
        }
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

    /** Escapes an HTML/XML string. */
    public static String escapeHTML (String html)
    {
        return html.replaceAll("&", "&amp;")
                   .replaceAll("<", "&lt;")
                   .replaceAll(">", "&gt;")
                   .replaceAll("\"", "&quot;");
    }

    /** Creates a safe, restricted HTML from user input. */
    public static HTML createRestrictedHTML (String html)
    {
        html = escapeHTML(html);
        html = html.replaceAll("(http://(.*?\\.)?whirled.com/([^ ]+))", "<a href=\"$1\">$3</a>");
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
    public static TextArea createTextArea (String text, String style, ChangeListener listener)
    {
        TextArea area = new TextArea();
        if (text != null) {
            area.setText(text);
        }
        if (style != null) {
            area.addStyleName(style);
        }
        if (listener != null) {
            area.addChangeListener(listener);
        }
        return area;
    }

    /**
     * Creates an orange button of the specified size with the supplied text.
     */
    public static PushButton createButton (String size, String label, ClickListener listener)
    {
        PushButton button = new PushButton(label);
        if (listener != null) {
            button.addClickListener(listener);
        }
        button.setStyleName(size + "OrangeButton");
        button.addStyleName("orangeButton");
        return button;
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
     * Creates an image button that changes appearance when you click and hover over it.
     */
    public static PushButton createImageButton (String style, ClickListener listener)
    {
        PushButton button = new PushButton();
        button.setStyleName(style);
        if (listener != null) {
            button.addClickListener(listener);
        }
        return button;
    }

    /**
     * Creates a button for closing things (a square with an x in it).
     */
    public static Widget createCloseButton (ClickListener listener)
    {
        return createImageButton("closeButton", listener);
    }

    /**
     * Creates a pair of previous and next buttons in a horizontal panel.
     */
    public static Widget createPrevNextButtons (ClickListener onPrev, ClickListener onNext)
    {
        HorizontalPanel panel = new HorizontalPanel();
        panel.setStyleName("pagedGrid"); // hijack PagedGrid styles
        Button prev = new Button(_cmsgs.prev());
        prev.setStyleName("Button");
        prev.addStyleName("PrevButton");
        prev.addClickListener(onPrev);
        panel.add(prev);
        panel.add(WidgetUtil.makeShim(5, 5));
        Button next = new Button(_cmsgs.next());
        next.setStyleName("Button");
        next.addStyleName("NextButton");
        next.addClickListener(onNext);
        panel.add(next);
        return panel;
    }

    /**
     * Creates a basic text button whose text changes depending on whether we are
     * creating or updating something.
     */
    public static Button createCrUpdateButton (boolean creating, ClickListener listener)
    {
        Button button = new Button(creating ? _cmsgs.create() : _cmsgs.update());
        button.addClickListener(listener);
        return button;
    }

    /**
     * Puts a pair buttons in a horizontal panel with a small gap betwixt them.
     */
    public static Widget createButtonPair (Widget left, Widget right)
    {
        HorizontalPanel panel = new HorizontalPanel();
        panel.addStyleName("ButtonPair");
        panel.add(left);
        panel.add(WidgetUtil.makeShim(5, 5));
        panel.add(right);
        return panel;
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
     * Creates an image that responds to clicking.
     */
    public static Image createActionImage (String path, ClickListener onClick)
    {
        return createActionImage(path, null, onClick);
    }

    /**
     * Creates an image that responds to clicking.
     */
    public static Image createActionImage (String path, String tip, ClickListener onClick)
    {
        return makeActionImage(new Image(path), tip, onClick);
    }

    /**
     * Makes an image into one that responds to clicking.
     */
    public static Image makeActionImage (Image image, String tip, ClickListener onClick)
    {
        if (onClick != null) {
            image.addStyleName("actionLabel");
            image.addClickListener(onClick);
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
     * Create a ClickListener that will track an arbitrary click on any widget against our
     * server-side tracking system.  No callback.
     * @param action String identifier for the action to be logged eg "landingPlayButtonClicked"
     * @param details Optional additional info about the action performed eg game or whirled id
     */
    public static ClickListener createTrackingListener (final String action, final String details)
    {
        return new ClickListener() {
            public void onClick (Widget sender) {
                _membersvc.trackClientAction(
                    TrackingCookie.get(), action, details != null ? details : "", null);
            }
        };
    }

    /**
     * Create a ClickListener that will track an arbitrary click on any widget during an a/b test.
     * No callback is performed on success or failure.
     * @param action String identifier for the action to be logged
     * @param testName Optional string identifier for the a/b test if associated with one
     */
    public static ClickListener createTestTrackingListener (
        final String action, final String testName)
    {
        return new ClickListener() {
            public void onClick (Widget sender) {
                _membersvc.trackTestAction(TrackingCookie.get(), action, testName, null);
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
        panel.add(new Button(_cmsgs.dismiss(), hider));
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
        content.add(new LogonPanel(true) {
            @Override public void onSuccess(SessionData result) {
                popup.hide();
                super.onSuccess(result);
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
     * near the supplied component.
     */
    public static void errorNear (String message, Widget source)
    {
        // TODO: style this differently than info feedback
        new InfoPopup(message).showNear(source);
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

    protected static final SimpleDateFormat _tfmt = new SimpleDateFormat("h:mmaa");
    protected static final SimpleDateFormat _mfmt = new SimpleDateFormat("MMM dd");
    protected static final SimpleDateFormat _yfmt = new SimpleDateFormat("MMM dd, yyyy");

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final WebMemberServiceAsync _membersvc = (WebMemberServiceAsync)
        ServiceUtil.bind(GWT.create(WebMemberService.class), WebMemberService.ENTRY_POINT);
}
