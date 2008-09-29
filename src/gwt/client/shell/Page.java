//
// $Id$

package client.shell;

import java.util.List;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.web.data.Invitation;
import com.threerings.msoy.web.data.SessionData;
import com.threerings.msoy.web.data.WebCreds;

import client.ui.BorderedDialog;
import client.util.ArrayUtil;
import client.util.Link;
import client.util.events.FlashEvent;
import client.util.events.FlashEvents;

/**
 * Handles some standard services for a top-level MetaSOY page.
 */
public abstract class Page
    implements EntryPoint
{
    /**
     * Returns the default title for the specified page.
     */
    public static String getDefaultTitle (Frame.Tabs tab)
    {
        try {
            return _dmsgs.xlate(tab.toString().toLowerCase()+ "Title");
        } catch (Exception e) {
            return null;
        }
    }

    // from interface EntryPoint
    public void onModuleLoad ()
    {
        init();

        // do our on-load stuff
        onPageLoad();

        // wire ourselves up to the top-level frame
        if (configureCallbacks(this)) {
            // if we're running in standalone page test mode, we do a bunch of stuff
            CShell.init(new PageFrame() {
                public void setTitle (String title) {
                    frameCall(Frame.Calls.SET_TITLE, new String[] { title });
                }
                public void addNavLink (String label, Pages page, String args) {
                    frameCall(Frame.Calls.ADD_NAV_LINK, new String[] { label, ""+page, args });
                }
                public void navigateTo (String token) {
                    frameCall(Frame.Calls.NAVIGATE_TO, new String[] { token });
                }
                public void navigateReplace (String token) {
                    frameCall(Frame.Calls.NAVIGATE_REPLACE, new String[] { token });
                }
                public void closeClient () {
                    frameCall(Frame.Calls.CLOSE_CLIENT, null);
                }
                public void closeContent () {
                    frameCall(Frame.Calls.CLOSE_CONTENT, null);
                }
                public void dispatchEvent (FlashEvent event) {
                    JavaScriptObject args = JavaScriptObject.createArray();
                    event.toJSObject(args);
                    frameTriggerEvent(event.getEventName(), args);
                }
                public void dispatchDidLogon (SessionData data) {
                    List<String> fdata = data.flatten();
                    frameCall(Frame.Calls.DID_LOGON, fdata.toArray(new String[fdata.size()]));
                }
                public String md5hex (String text) {
                    return frameCall(Frame.Calls.GET_MD5, new String[] { text })[0];
                }
                public String checkFlashVersion (int width, int height) {
                    return frameCall(Frame.Calls.CHECK_FLASH_VERSION,
                                     new String[] { ""+width, ""+height })[0];
                }
                public Invitation getActiveInvitation () {
                    return Invitation.unflatten(
                        ArrayUtil.toIterator(frameCall(Frame.Calls.GET_ACTIVE_INVITE, null)));
                }
            });

            // obtain our current credentials from the frame
            CShell.creds = WebCreds.unflatten(
                ArrayUtil.toIterator(frameCall(Frame.Calls.GET_WEB_CREDS, null)));
            CShell.visitor = VisitorInfo.unflatten(
                ArrayUtil.toIterator(frameCall(Frame.Calls.GET_VISITOR_INFO, null)));

            // and get our current page token from our containing frame
            setPageToken(frameCall(Frame.Calls.GET_PAGE_TOKEN, null)[0]);

        } else {
            // if we're running in standalone page test mode, we do a bunch of stuff
            CShell.init(new PageFrame() {
                public void setTitle (String title) {
                    Window.setTitle(title == null ? _cmsgs.bareTitle() : _cmsgs.windowTitle(title));
                }
                public void addNavLink (String label, Pages page, String args) {
                    CShell.log("No nav bar in standalone mode [label=" + label + ", page=" + page +
                               ", args=" + args + "].");
                }
                public void navigateTo (String token) {
                    if (!token.equals(History.getToken())) {
                        History.newItem(token);
                    }
                }
                public void navigateReplace (String token) {
                    History.back();
                    History.newItem(token);
                }
                public void dispatchEvent (FlashEvent event) {
                    FlashEvents.internalDispatchEvent(event);
                }
                public void dispatchDidLogon (SessionData data) {
                    Session.didLogon(data);
                }
                public String md5hex (String text) {
                    CShell.log("Pants! No md5 in standalone mode.");
                    return text;
                }
                public String checkFlashVersion (int width, int height) {
                    return null; // sure man, no problem!
                }
                public Invitation getActiveInvitation () {
                    return null; // we're testing, no one invited us
                }
            });

            final HistoryListener listener = new HistoryListener() {
                public void onHistoryChanged (String token) {
                    // this is only called when we're in single page test mode, so we assume we're
                    // staying on the same page and just pass the arguments back into ourselves
                    token = token.substring(token.indexOf("-")+1);
                    setPageToken(token);
                }
            };
            History.addHistoryListener(listener);

            Session.addObserver(new Session.Observer() {
                public void didLogon (SessionData data) {
                    listener.onHistoryChanged(History.getToken());
                }
                public void didLogoff () {
                    listener.onHistoryChanged(History.getToken());
                }
            });
            Session.validate();
        }
    }

    /**
     * Called when the page is first resolved to initialize its bits.
     */
    public void init ()
    {
        // initialize our services and translations
        initContext();
    }

    /**
     * Called when the user has navigated to this page. A call will immediately follow to {@link
     * #onHistoryChanged} with the arguments passed to this page or the empty string if no
     * arguments were supplied.
     */
    public void onPageLoad ()
    {
    }

    /**
     * Called when the user navigates to this page for the first time, and when they follow {@link
     * Link#create} links within tihs page.
     */
    public abstract void onHistoryChanged (Args args);

    /**
     * Called when the user navigates away from this page to another page. Gives the page a chance
     * to shut anything down before its UI is removed from the DOM.
     */
    public void onPageUnload ()
    {
    }

    /**
     * Returns the identifier of this page (used for navigation).
     */
    public abstract Pages getPageId ();

    /**
     * Called during initialization to give our entry point and derived classes a chance to
     * initialize their respective context classes.
     */
    protected void initContext ()
    {
    }

    /**
     * Returns the content widget last configured with {@link #setContent}.
     */
    protected Widget getContent ()
    {
        return _content;
    }

    /**
     * Clears out any existing content, creates a new Flash object from the definition, and sets it
     * as the new main page content. Returns the newly-created content as a widget.
     */
    protected HTML setFlashContent (String title, String definition)
    {
        // Please note: the following is a work-around for an IE7 bug. If we create a Flash object
        // node *before* attaching it to the DOM tree, IE will silently fail to register the Flash
        // object's callback functions for access from JavaScript. To make this work, create an
        // empty node first, add it to the DOM tree, and then initialize it with the Flash object
        // definition.  Also see: WidgetUtil.embedFlashObject()
        HTML control = new HTML();
        setContent(title, control);
        control.setHTML(definition);
        return control;
    }

    /**
     * Clears out any existing content and sets the specified widget as the main page content.
     */
    protected void setContent (Widget content)
    {
        setContent(null, content);
    }

    /**
     * Clears out any existing content and sets the specified widget as the main page content.
     */
    protected void setContent (String title, Widget content)
    {
        CShell.frame.setTitle(title == null ? getDefaultTitle(getPageId().getTab()) : title);

        RootPanel contentDiv = RootPanel.get();
        if (_content != null) {
            contentDiv.remove(_content);
        }
        _content = content;
        if (_content != null) {
            contentDiv.add(_content);
        }

        // clear out any lingering dialog
        CShell.frame.clearDialog();
    }

    /**
     * Called when our page token has been changed by the outer frame.
     */
    protected void setPageToken (String token)
    {
        Args args = new Args();
        args.setToken(token);
        onHistoryChanged(args);
    }

    /**
     * Calls up to our containing frame and takes an action and possibly returns a value.
     */
    protected String[] frameCall (Frame.Calls call, String[] args)
    {
        return nativeFrameCall(call.toString(), args);
    }

    /**
     * Called when Flash or our parent frame wants us to dispatch an event.
     */
    protected void triggerEvent (String eventName, JavaScriptObject args)
    {
        FlashEvent event = FlashEvents.createEvent(eventName, args);
        if (event != null) {
            FlashEvents.internalDispatchEvent(event);
        }
    }

    /**
     * Wires ourselves up to our enclosing frame.
     *
     * @return true if we're running as a subframe, false if we're running in standalone test mode.
     */
    protected static native boolean configureCallbacks (Page page) /*-{
        $wnd.setPageToken = function (token) {
            page.@client.shell.Page::setPageToken(Ljava/lang/String;)(token)
        };
        $wnd.triggerEvent = function (eventName, args) {
            page.@client.shell.Page::triggerEvent(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(eventName, args);
        };
        $wnd.helloWhirled = function () {
             return true;
        };
        $wnd.displayPage = function (page, args) {
            @client.util.Link::goFromFlash(Ljava/lang/String;Ljava/lang/String;)(page, args);
        };
        return $wnd != $wnd.top;
    }-*/;

    protected static native String[] nativeFrameCall (String action, String[] args) /*-{
        return $wnd.top.frameCall(action, args);
    }-*/;

    protected static native void frameTriggerEvent (String name, JavaScriptObject args) /*-{
        $wnd.top.triggerFlashEvent(name, args);
    }-*/;

    protected abstract class PageFrame implements Frame
    {
        public void navigateTo (String token) {
            if (!token.equals(History.getToken())) {
                History.newItem(token);
            }
        }

        public void navigateReplace (String token) {
            History.back();
            History.newItem(token);
        }

        public void closeClient () {
            CShell.log("Would close client.");
        }
        public void closeContent () {
            CShell.log("Would close content.");
        }

        public void showDialog (String title, Widget dialog) {
            clearDialog();
            _dialog = new BorderedDialog(false, false, false) {
                protected void onClosed (boolean autoClosed) {
                    _dialog = null;
                }
            };
            _dialog.setHeaderTitle(title);
            _dialog.setContents(dialog);
            _dialog.show();
        }
        public void clearDialog () {
            if (_dialog != null) {
                _dialog.hide();
            }
        }
    }

    protected Widget _content;
    protected BorderedDialog _dialog;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
}
