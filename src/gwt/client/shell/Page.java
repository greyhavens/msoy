//
// $Id$

package client.shell;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.ClientMode;
import com.threerings.msoy.web.gwt.Embedding;
import com.threerings.msoy.web.gwt.Invitation;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.SessionData;
import com.threerings.msoy.web.gwt.Tabs;
import com.threerings.msoy.web.gwt.WebCreds;

import client.ui.BorderedDialog;
import client.util.ArrayUtil;
import client.util.Link;
import client.util.events.FlashEvent;
import client.util.events.FlashEvents;
import client.util.events.PageCommandEvent.Listener;
import client.util.events.PageCommandEvent;

/**
 * Handles some standard services for a top-level MetaSOY page.
 */
public abstract class Page
    implements EntryPoint
{
    /** Prefix for our iframe id to avoid conflicts with other getElementById calls. */
    public static final String FRAME_ID_PREFIX = "PageFrame_";

    /**
     * Interface for removing a previously registered object.
     */
    public static interface Registration
    {
        void remove ();
    }

    /**
     * Returns the default title for the specified page.
     */
    public static String getDefaultTitle (Tabs tab)
    {
        return ""; // No title
    }

    /**
     * Gets the page corresponding to the current module, or null if the current module is not a
     * Page instance.
     */
    public static Pages getPage ()
    {
        if (CShell.frame instanceof PageFrame) {
            return ((PageFrame)CShell.frame).getPage();
        }
        return null;
    }

    /**
     * Adds a listener that will be invoked whenever a command event is received that is targeting
     * the current page. This allows callers to avoid explicit references to their page. The
     * returned registration is used to remove the listener. If there are any past events, they
     * will also be dispatched. Each event may only be acted upon once, that is to say the first
     * listener to return true from {@link PageCommandEvent.Listener#act} will cause the event
     * to not be sent to subsequent listeners.
     */
    public static Registration register (final PageCommandEvent.Listener listener)
    {
        _relay.add(listener);
        return new Registration() {
            @Override public void remove () {
                if (_listener == null) {
                    throw new IllegalStateException();
                }
                _relay.remove(_listener);
                _listener = null;
            }
            protected PageCommandEvent.Listener _listener = listener;
        };
    }

    // from interface EntryPoint
    public void onModuleLoad ()
    {
        init();

        // do our on-load stuff
        onPageLoad();

        // record command events so that sub-page listeners won't miss them (e.g. while a servlet
        // request is pending)
        FlashEvents.addListener(_relay = new Relay());

        // wire ourselves up to the top-level frame
        if (configureCallbacks(this)) {
            // if we're not running in standalone page test mode, we basically forward requests
            CShell.init(new PageFrame(getPageId()) {
                public void setTitle (String title) {
                    frameCall(Frame.Calls.SET_TITLE, title);
                }
                public void addNavLink (String label, Pages page, Args args, int pos) {
                    frameCall(Frame.Calls.ADD_NAV_LINK, label, ""+page, args.toToken(), ""+pos);
                }
                public void navigateTo (String token) {
                    frameCall(Frame.Calls.NAVIGATE_TO, token);
                }
                public void navigateReplace (String token) {
                    frameCall(Frame.Calls.NAVIGATE_REPLACE, token);
                }
                public void closeClient () {
                    frameCall(Frame.Calls.CLOSE_CLIENT);
                }
                public void closeContent () {
                    frameCall(Frame.Calls.CLOSE_CONTENT);
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
                public void logoff () {
                    frameCall(Frame.Calls.LOGOFF);
                }
                public void emailUpdated (String address, boolean validated) {
                    Session.emailUpdated(address, validated);
                    frameCall(Frame.Calls.EMAIL_UPDATED, address, String.valueOf(validated));
                }
                public String md5hex (String text) {
                    return frameCall(Frame.Calls.GET_MD5, text)[0];
                }
                public String checkFlashVersion (int width, int height) {
                    return frameCall(Frame.Calls.CHECK_FLASH_VERSION, ""+width, ""+height)[0];
                }
                public Invitation getActiveInvitation () {
                    return Invitation.unflatten(
                        ArrayUtil.toIterator(frameCall(Frame.Calls.GET_ACTIVE_INVITE)));
                }
                public VisitorInfo getVisitorInfo () {
                    return VisitorInfo.unflatten(
                        ArrayUtil.toIterator(frameCall(Frame.Calls.GET_VISITOR_INFO)));
                }
                public void reportTestAction (String test, String action) {
                    frameCall(Frame.Calls.TEST_ACTION, test, action);
                }
                public Embedding getEmbedding () {
                    return Embedding.unflatten(frameCall(Frame.Calls.GET_EMBEDDING));
                }
                public boolean isHeaderless () {
                    return Boolean.valueOf(frameCall(Frame.Calls.IS_HEADERLESS)[0]);
                }
                public void openBottomFrame (String token) {
                    frameCall(Frame.Calls.OPEN_BOTTOM_FRAME, token);
                }
                public int getThemeId () {
                    return Integer.valueOf(frameCall(Frame.Calls.GET_THEME_ID)[0]);
                }
            });

            // extract our frame id to use in frameCall
            String frameId = getFrameId();
            if (frameId != null && frameId.startsWith(FRAME_ID_PREFIX)) {
                _frameId = frameId.substring(FRAME_ID_PREFIX.length());
            } else {
                _frameId = "";
            }

            // obtain our current credentials from the frame
            CShell.creds = WebCreds.unflatten(
                ArrayUtil.toIterator(frameCall(Frame.Calls.GET_WEB_CREDS)));

            // limit the outgoing links created by the games portal app
            if (CShell.getClientMode().isFacebookGames()) {
                Link.setValidPages(new Pages[] { Pages.GAMES, Pages.FACEBOOK, Pages.WORLD });
            }

            // and get our current page token from our containing frame
            setPageToken(frameCall(Frame.Calls.GET_PAGE_TOKEN)[0]);

        } else {
            // if we're running in standalone page test mode, we do a bunch of stuff
            CShell.init(new PageFrame(getPageId()) {
                public void setTitle (String title) {
                    Window.setTitle(title == null ? _cmsgs.bareTitle() : _cmsgs.windowTitle(title));
                }
                public void addNavLink (String label, Pages page, Args args, int position) {
                    CShell.log("No nav bar in standalone mode", "label", label, "page", page,
                               "args", args, "position", position);
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
                public void closeClient () {
                    CShell.log("Would close client.");
                }
                public void closeContent () {
                    CShell.log("Would close content.");
                }
                public void dispatchEvent (FlashEvent event) {
                    FlashEvents.internalDispatchEvent(event);
                }
                public void dispatchDidLogon (SessionData data) {
                    Session.didLogon(data);
                }
                public void logoff () {
                    Session.didLogoff();
                }
                public void emailUpdated (String address, boolean validated) {
                    Session.emailUpdated(address, validated);
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
                public VisitorInfo getVisitorInfo () {
                    return Session.frameGetVisitorInfo();
                }
                public void reportTestAction (String test, String action) {
                    CShell.log("Test action", "test", test, "action", action);
                }
                public Embedding getEmbedding () {
                    return new Embedding(ClientMode.UNSPECIFIED, 0);
                }
                public boolean isHeaderless () {
                    return false; // we have no header but pretend like we do
                }
                public void openBottomFrame (String token) {
                    // no bottom frame in test mode
                }
                public int getThemeId () {
                    // no themes in test mode
                    return 0;
                }
            });

            History.addValueChangeHandler(new ValueChangeHandler<String>() {
                public void onValueChange (ValueChangeEvent<String> event)  {
                    onHistoryChanged(event.getValue());
                }
            });

            Session.addObserver(new Session.Observer() {
                public void didLogon (SessionData data) {
                    onHistoryChanged(History.getToken());
                }
                public void didLogoff () {
                    onHistoryChanged(History.getToken());
                }
            });
            Session.validate();
        }

        // load up JavaScript source files
        ScriptSources.inject(CShell.getAppId());
    }

    protected void onHistoryChanged (String token)
    {
        // this is only called when we're in single page test mode, so we assume we're staying on
        // the same page and just pass the arguments back into ourselves
        token = token.substring(token.indexOf("-")+1);
        setPageToken(token);
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
     * Clears out any existing content and sets the specified widget as the main page content.
     */
    public void setContent (Widget content)
    {
        setContent(null, content);
    }

    /**
     * Clears out any existing content and sets the specified widget as the main page content.
     */
    public void setContent (String title, Widget content)
    {
        CShell.frame.setTitle(title == null ? getDefaultTitle(getPageId().getTab()) : title);

        RootPanel contentDiv = RootPanel.get();
        if (_content != null) {
            contentDiv.remove(_content);
        }
        _content = content;
        if (_content != null) {
            if (content instanceof RequiresResize) {
                RootLayoutPanel.get().add(content);
            } else {
                contentDiv.add(content);
            }
        }

        // clear out any lingering dialog
        CShell.frame.clearDialog();

        // let the frame know the content is ready to roll
        frameCall(Frame.Calls.CONTENT_SET, getPageId().toString(), _token);
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
     * Called when our page token has been changed by the outer frame.
     */
    protected void setPageToken (String token)
    {
        _token = token;
        onHistoryChanged(Args.fromToken(token));
    }

    /**
     * Calls up to our containing frame and takes an action and possibly returns a value.
     */
    protected String[] frameCall (Frame.Calls call, String... args)
    {
        return nativeFrameCall(call.toString(), _frameId, args);
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
        return typeof($wnd.parent.frameCall) != "undefined";
    }-*/;

    protected static native String[] nativeFrameCall (
        String action, String frameId, String[] args) /*-{
        if ($wnd.frameCall) {
            return $wnd.frameCall(action, frameId, args);
        } else if ($wnd.parent.frameCall) {
            return $wnd.parent.frameCall(action, frameId, args);
        } else {
            return null;
        }
    }-*/;

    protected static native void frameTriggerEvent (String name, JavaScriptObject args) /*-{
        if ($wnd.triggerFlashEvent) {
            return $wnd.triggerFlashEvent(name, args);
        } else if ($wnd.parent.triggerFlashEvent) {
            return $wnd.parent.triggerFlashEvent(name, args);
        }
    }-*/;

    /**
     * Gets the id of our containing iframe (assigned by {@link client.frame.PageFrame}). Browser
     * support is patchy, so returns the best result of several techniques. Returns an empty string
     * if the id could not be extracted.
     */
    protected static native String getFrameId () /*-{
        try {
            // primarily for IE
            if ($wnd.frameElement && $wnd.frameElement.id) {
                return $wnd.frameElement.id;

            // non-mozilla?
            } else if (parent.id) {
                return parent.id;

            // mozilla
            } else if (parent.name) {
                return parent.name;
            }
        } catch (e) {
            var msg = "Failed to get frame id [error=" + e + "].";
            if ($wnd.console) {
                $wnd.console.log(msg);
            } else {
                //alert(msg);
            }
        }
        return "";
    }-*/;

    protected abstract class PageFrame implements Frame
    {
        public PageFrame (Pages page) {
            _page = page;
        }
        public Pages getPage () {
            return _page;
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

        protected Pages _page;
    }

    /**
     * Holds command events received from the flash client and dispatches them when listeners
     * are added. This is so that the page content widgets can act on flash events sent prior to
     * the initialization of the content. This is the norm when flash opens a new URL and sends a
     * command.
     */
    protected static class Relay
        implements PageCommandEvent.Listener
    {
        /**
         * Registers the given listener to receive future events and dispatches previous events,
         * allowing the listener to consume them.
         */
        public void add (final PageCommandEvent.Listener listener)
        {
            _listeners.add(listener);

            final PageCommandEvent event = _queue;
            if (event != null) {
                // postpone the method call so the call stack can unwind completely
                new Timer() {
                    @Override public void run () {
                        if (listener.act(event) && event == _queue) {
                            _queue = null;
                        }
                    }
                }.schedule(1);
            }
        }

        public void remove (PageCommandEvent.Listener listener)
        {
            _listeners.remove(listener);
        }

        @Override public boolean act (PageCommandEvent commandEvent)
        {
            if (commandEvent.getTarget() != getPage()) {
                return false;
            }

            for (PageCommandEvent.Listener listener : _listeners) {
                if (listener.act(commandEvent)) {
                    return true;
                }
            }

            _queue = commandEvent; // goodbye old event
            return true;
        }

        protected PageCommandEvent _queue; // queue of maximum size 1
        protected List<Listener> _listeners = Lists.newArrayList();
    }

    protected Widget _content;
    protected BorderedDialog _dialog;
    protected String _frameId = "";
    protected String _token;

    protected static Relay _relay;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
}
