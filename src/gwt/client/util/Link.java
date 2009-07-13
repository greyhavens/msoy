//
// $Id$

package client.util;

import java.util.HashSet;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.impl.HyperlinkImpl;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;

import client.ui.MsoyUI;

/**
 * A utility class with link-related methods.
 */
public class Link
{
    /**
     * Sets the pages that we can legitimately link to. By default, any page can be linked. After
     * this method is called, only pages in the set will be linked. Requests to link to other pages
     * will return a label or plain text widget.
     */
    public static void setValidPages (Pages[] pages)
    {
        _validPages = new HashSet<Pages>();
        for (Pages page : pages) {
            _validPages.add(page);
        }
    }

    /**
     * Returns link that displays the details of a given group.
     */
    public static Widget groupView (String label, int groupId)
    {
        return create(label, Pages.GROUPS, "d", groupId);
    }

    /**
     * Returns link that displays the details of a given member.
     */
    public static Widget memberView (String label, int memberId)
    {
        return create(label, Pages.PEOPLE, memberId);
    }

    /**
     * Returns link that displays the details of a given member.
     */
    public static Widget memberView (MemberName name)
    {
        return create(name.toString(), Pages.PEOPLE, name.getMemberId());
    }

    /**
     * Returns link that displays a shop listing.
     */
    public static Widget shopListingView (String itemName, byte itemType, int catalogId)
    {
        return create(itemName, Pages.SHOP, "l", itemType, catalogId);
    }

    /**
     * Returns link that displays the transaction history of a given member.
     */
    public static Widget transactionsView (String label, int memberId)
    {
        return create(label, Pages.ME, "transactions", "1", memberId);
    }

    /**
     * Returns link that navigates to the specified application page with the specified arguments.
     * A page should use this method to pass itself arguments.
     */
    public static Widget create (String label, Pages page, Object... args)
    {
        return create(label, null, page, args);
    }

    /**
     * Returns link that navigates to the specified application page with the specified arguments.
     * A page should use this method to pass itself arguments.
     */
    public static Widget create (String label, String styleName, Pages page, Object... args)
    {
        Widget link;
        if (isValid(page)) {
            link = new ReroutedHyperlink(label, false, createToken(page, args));
        } else {
            link = MsoyUI.createLabel(label, styleName);
        }
        if (styleName != null) {
            link.addStyleName(styleName);
        }
        link.addStyleName("inline");
        return link;
    }

    /**
     * Creates a link with a text label that will open a url in the top-most frame.
     */
    public static Widget createTop (String style, String url)
    {
        return new TargetedLink("", style, url, "_top");
    }

    /**
     * Returns a non-inline link that navigates to the specified application page with the
     * specified arguments. A page should use this method to pass itself arguments.
     */
    public static Widget createBlock (String label, String styleName, Pages page, Object... args)
    {
        Widget link;
        if (isValid(page)) {
            link = new ReroutedHyperlink(label, false, createToken(page, args));
        } else {
            link = MsoyUI.createLabel(label, styleName);
        }
        if (styleName != null) {
            link.addStyleName(styleName);
        }
        return link;
    }

    /**
     * Returns link that navigates to the specified application page with the specified arguments.
     * A page should use this method to pass itself arguments.
     */
    public static Widget createImage (String path, String tip, Pages page, Object... args)
    {
        return createHyperlink("<img border=0 src=\"" + path + "\">", tip, page, args);
    }

    /**
     * Returns link that navigates to the specified application page with the specified arguments.
     * A page should use this method to pass itself arguments.
     */
    public static Widget createImage (AbstractImagePrototype image, String tip,
                                      Pages page, Object... args)
    {
        return createHyperlink(image.getHTML(), tip, page, args);
    }

    /**
     * Returns HTML that links to the specified page with the specified arguments. Don't use this
     * if you can avoid it. Hyperlink does special stuff to make the history mechanism work in some
     * browsers and this breaks that.
     */
    public static String createHtml (String label, Pages page, Object... args)
    {
        HTML escaper = new HTML();
        escaper.setText(label);
        String html = escaper.getHTML();
        if (isValid(page)) {
            html = "<a href=\"/#" + createToken(page, args) + "\">" + html + "</a>";
        }
        return html;
    }

    /**
     * Returns a string that can be appended to '#' to link to the specified page with the
     * specified arguments.
     */
    public static String createToken (Pages page, Object... args)
    {
        if (page == null) {
            page = Pages.LANDING;
        }
        return page.makeToken(args);
    }

    /**
     * Creates a click handler that navigates to the supplied page when activated.
     */
    public static ClickHandler createHandler (final Pages page, final Object... args)
    {
        return new ClickHandler() {
            public void onClick (ClickEvent event) {
                go(page, args);
            }
        };
    }

    /**
     * Move to the page in question.
     */
    public static void go (Pages page, Object... args)
    {
        if (!isValid(page)) {
            return;
        }

        CShell.frame.navigateTo(createToken(page, args));
    }

    /**
     * Replace the current page with the one specified.
     */
    public static void replace (Pages page, Object... args)
    {
        if (!isValid(page)) {
            return;
        }

        CShell.frame.navigateReplace(createToken(page, args));
    }

    /**
     * Called when Flash wants us to display a page.
     */
    public static void goFromFlash (String page, String args)
    {
    	try {
            go(Enum.valueOf(Pages.class, page.toUpperCase()), Args.fromToken(args));
    	} catch (Exception e) {
            CShell.log("Unable to display page from Flash", "page", page, "args", args, e);
    	}
    }

    /**
     * A helper function for both {@link #getImageLink}s.
     */
    protected static Widget createHyperlink (String html, String tip, Pages page, Object... args)
    {
        if (!isValid(page)) {
            return MsoyUI.createHTML(html, null);
        }

        Widget link = new ReroutedHyperlink(html, true, createToken(page, args));
        if (tip != null) {
            link.setTitle(tip);
        }
        link.addStyleName("inline");
        return link;
    }

    /**
     * Check if the page can be linked to. See {@link #setValidPages}.
     */
    protected static boolean isValid (Pages page)
    {
        return _validPages == null || _validPages.contains(page);
    }

    /**
     * A modified Hyperlink that routes its click through the application (so that it can be sent
     * to the top-level frame for processing) and which sets its path to the path of the top-level
     * frame, so that opening the link in a new tab or window will properly recreate the entire
     * framed environment rather than just opening the internal frame.
     *
     * We'd just extend Hyperlink and make a few small modifications, but we can't get at the
     * anchor element or the list of click listeners because they're both private. So instead we
     * have to reimplement everything. Yay!
     */
    protected static class ReroutedHyperlink extends Widget
        implements HasClickHandlers
    {
        public ReroutedHyperlink (String text, boolean asHTML, String targetHistoryToken) {
            _targetHistoryToken = targetHistoryToken;

            setElement(DOM.createDiv());
            sinkEvents(Event.ONCLICK);
            setStyleName("gwt-Hyperlink");

            Element anchorElem = DOM.createAnchor();
            DOM.appendChild(getElement(), anchorElem);
            DOM.setElementProperty(anchorElem, "href", "/#" + _targetHistoryToken);

            if (asHTML) {
                DOM.setInnerHTML(anchorElem, text);
            } else {
                DOM.setInnerText(anchorElem, text);
            }
        }

        // from interface HasClickHandlers
        public HandlerRegistration addClickHandler (ClickHandler handler) {
            return addDomHandler(handler, ClickEvent.getType());
        }

        @Override // from Widget
        public void onBrowserEvent (Event event) {
            super.onBrowserEvent(event);
            if (DOM.eventGetType(event) == Event.ONCLICK && impl.handleAsClick(event)) {
                CShell.frame.navigateTo(_targetHistoryToken);
                DOM.eventPreventDefault(event);
            }
        }

        protected String _targetHistoryToken;

        protected static HyperlinkImpl impl = GWT.create(HyperlinkImpl.class);
    }

    /**
     * A simple old skool link.
     */
    protected static class TargetedLink extends Widget
    {
        /**
         * Creates a new link with the given text and the given href and target attributes.
         */
        public TargetedLink (String text, String style, String href, String target)
        {
            setElement(DOM.createDiv());
            setStyleName("gwt-Hyperlink");
            if (style != null) {
                addStyleName(style);
            }
            Element anchorElem = DOM.createAnchor();
            getElement().appendChild(anchorElem);
            anchorElem.setInnerText(text);
            anchorElem.setAttribute("href", href);
            anchorElem.setAttribute("target", target);
        }
    }

    /**
     * The set of pages that we can link to. Requests for links to any other pages return plain
     * text widgets. This is a hack to keep whirled on Facebook from linking out to other parts of
     * the site that will just confuse or look buggy to Facebook users.
     */
    protected static HashSet<Pages> _validPages;
}
