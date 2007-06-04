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
    public static const HELP_PAGE_SET_STYLE_COMMAND :String = "setcss";
    
    public function PageDisplayController (ctx :WorldContext, tab :PageDisplayTab) :void
    {
        _ctx = ctx;
        _tab = tab;

        setControlledPanel(tab);

        _pageLoader = new URLLoader();
        _cssLoader = new URLLoader();
    }

    public function init () :void
    {
        _pageLoader.addEventListener(Event.OPEN, pageLoadStarted);
        _pageLoader.addEventListener(Event.COMPLETE, pageLoadComplete);
        _pageLoader.addEventListener(HTTPStatusEvent.HTTP_STATUS, handleHttpStatusCode);
        _pageLoader.addEventListener(IOErrorEvent.IO_ERROR, handleIOError);
        _pageLoader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleSecurityError);

        // we don't care about so many events for the css loader
        _cssLoader.addEventListener(Event.COMPLETE, cssLoadComplete);
        _cssLoader.addEventListener(IOErrorEvent.IO_ERROR, handleCSSError);
        _cssLoader.addEventListener(SecurityErrorEvent.SECURITY_ERROR, handleCSSError);
    }

    public function shutdown () :void
    {
        _pageLoader.removeEventListener(Event.OPEN, pageLoadStarted);
        _pageLoader.removeEventListener(Event.COMPLETE, pageLoadComplete);
        _pageLoader.removeEventListener(HTTPStatusEvent.HTTP_STATUS, handleHttpStatusCode);
        _pageLoader.removeEventListener(IOErrorEvent.IO_ERROR, handleIOError);
        _pageLoader.removeEventListener(SecurityErrorEvent.SECURITY_ERROR, handleSecurityError);

        _cssLoader.removeEventListener(Event.COMPLETE, cssLoadComplete);
        _cssLoader.removeEventListener(IOErrorEvent.IO_ERROR, handleCSSError);
        _cssLoader.removeEventListener(SecurityErrorEvent.SECURITY_ERROR, handleCSSError);
    }
    
    /**
     * Handles the HELP_PAGE_DISPLAY_COMMAND action. Starts loading an HTML page from the specified
     * URL and, when ready, displays it in the PageDisplayTab.
     */
    public function help (url :String) :void
    {
        // if this is an absolute url, warn the developer (but try anyway)
        if (url.match("^http://") != null) {
            Log.getLog(this).warning("Help page url should be relative: " + url);
        }
        
        startLoading(_pageLoader, url);
    }

    /**
     * Handles the HELP_PAGE_SET_STYLE_COMMAND action. Starts loading a CSS file from the specified
     * URL and, when ready, applies it to the PageDisplayTab (i.e. the styles will be applied to
     * the currently displayed page, and all future pages, until set to a different style.)
     */
    public function setcss (url :String) :void
    {
        startLoading(_cssLoader, url);
    }
    
    /** Starts loading the specified URL. */
    protected function startLoading (loader :URLLoader, url :String) :void
    {
        // shut down any operation already in progress
        try {
            loader.close();
        } catch (e: Error) {
            // no op. i'm just catching errors caused by closing an already closed stream.
            // this wouldn't be necessary if I could just ask the loader's stream was already
            // closed. but no, that would be too easy. 
        }

        // get a new page!
        loader.load(new URLRequest(url));
    }

    protected function cssLoadComplete (event :Event) :void
    {
        _tab.setStyleSheet(String(_cssLoader.data));
    }
    
    protected function pageLoadStarted (event :Event) :void 
    {
        messageDisplay(Msgs.GENERAL.get("m.help_loading"));
    }

    protected function pageLoadComplete (event :Event) :void 
    {
        rawDisplay(String(_pageLoader.data));
    }

    protected function handleCSSError (event :Event) :void
    {
        // for errors loading the style sheet, let the developer know, but otherwise ignore them
        Log.getLog(this).warning("Error loading style sheet: " + event);
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
        // let the developer know
        Log.getLog(this).warning("Error loading page: " + event);

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
    protected var _pageLoader :URLLoader;
    protected var _cssLoader :URLLoader;
}
}
