//
// $Id$

package client.item;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.MemberName;

import client.shell.MsoyEntryPoint;

/**
 * Displays a creator's name with "by Foozle" where Foozle is a link to the creator's profile page.
 */
public class CreatorLabel extends Widget
{
    public CreatorLabel ()
    {
        Element parent = DOM.createDiv();
        setElement(parent);
        Element by = DOM.createSpan();
        DOM.setInnerText(by, CItem.imsgs.creatorBy() + " ");
        DOM.appendChild(parent, by);
        _text = DOM.createSpan();
        DOM.setAttribute(_text, "className", "actionLabel");
        DOM.appendChild(parent, _text);
        sinkEvents(Event.ONCLICK | Event.MOUSEEVENTS);
    }

    public void setMember (MemberName name)
    {
        _name = name;
        String ppage = MsoyEntryPoint.memberViewPath(_name.getMemberId());
        DOM.setInnerHTML(_text, "<a href=\"" + ppage + "\">" + _name.toString() + "</a>");
    }

    protected MemberName _name;
    protected Element _text;
}
