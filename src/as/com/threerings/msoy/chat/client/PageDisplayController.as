//
// $Id$

package com.threerings.msoy.chat.client {

import flash.events.Event;
import flash.events.HTTPStatusEvent;
import flash.events.IOErrorEvent;
import flash.events.SecurityErrorEvent;
import flash.events.TextEvent;
import flash.net.URLLoader;
import flash.net.URLRequest;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.util.Controller;

/**
 * Controller for the page display tab, handles requests to display a page, messages, etc.
 */
public class PageDisplayController extends Controller
{
    public static const HELP_PAGE_DISPLAY_COMMAND :String = "help";
    
    public function PageDisplayController (ctx :WorldContext, tab :PageDisplayTab) :void
    {
        _ctx = ctx;
        _tab = tab;

        setControlledPanel(tab);

        _loader = new URLLoader();
    }

    public function init () :void
    {
        _loader.addEventListener(Event.OPEN, loadStarted);
        _loader.addEventListener(Event.COMPLETE, loadComplete);
        _loader.addEventListener(HTTPStatusEvent.HTTP_STATUS, handleHttpStatusCode);
        _loader.addEventListener(IOErrorEvent.IO_ERROR, handleIOError);
        _loader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleSecurityError);

    }

    public function shutdown () :void
    {
        _loader.removeEventListener(Event.OPEN, loadStarted);
        _loader.removeEventListener(Event.COMPLETE, loadComplete);
        _loader.removeEventListener(HTTPStatusEvent.HTTP_STATUS, handleHttpStatusCode);
        _loader.removeEventListener(IOErrorEvent.IO_ERROR, handleIOError);
        _loader.removeEventListener(SecurityErrorEvent.SECURITY_ERROR, handleSecurityError);

    }
    
    /** Handles the HELP_PAGE_DISPLAY_COMMAND action. */
    public function help (url :String) :void
    {
        // if this is an absolute url, warn the developer (but try anyway)
        if (url.match("^http://") != null) {
            Log.getLog(this).warning("Help page url should be relative: " + url);
        }
        
        startLoading(url);
    }
    
    /** Starts loading the specified URL. */
    protected function startLoading (url :String) :void
    {
        // shut down any operation already in progress
        try {
            _loader.close();
        } catch (e: Error) {
            // no op. i'm just catching errors caused by closing an already closed stream.
            // this wouldn't be necessary if I could just ask the loader's stream was already
            // closed. but no, that would be too easy. 
        }

        // get a new page!
        _loader.load(new URLRequest(url));
    }
    
    protected function loadStarted (event :Event) :void 
    {
        messageDisplay(Msgs.GENERAL.get("m.help_loading"));
    }

    protected function loadComplete (event :Event) :void 
    {
        rawDisplay(String(_loader.data));
    }

    protected function handleHttpStatusCode (event :HTTPStatusEvent) :void
    {
        // this isn't all that useful, since it doesn't behave the same way on all browsers.
        // but we'll try our best.
        
        if (event.status >= 400) {
            messageDisplay(
                Msgs.GENERAL.get("m.help_httpstatus_error", _tab.tabName, event.status));
        }
    }

    protected function handleIOError (event :IOErrorEvent) :void
    {
        messageDisplay(Msgs.GENERAL.get("m.help_io_error", _tab.tabName));
    }        
    
    protected function handleSecurityError (event :SecurityErrorEvent) :void
    {
        messageDisplay(Msgs.GENERAL.get("m.help_security_error", _tab.tabName));
    }        
    
    protected function messageDisplay (message :String) :void
    {
        rawDisplay("<b>" + message + "</b><br><br>" + Msgs.GENERAL.get("m.help_footnote"));
    }

    protected function rawDisplay (source :String) :void
    {
        _tab.displayHTML(source);
    }

    protected var _ctx :WorldContext;
    protected var _tab :PageDisplayTab;
    protected var _loader :URLLoader;
}
}
