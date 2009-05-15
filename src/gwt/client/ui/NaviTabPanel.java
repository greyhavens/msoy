//
// $Id$

package client.ui;

import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;

import com.threerings.msoy.web.gwt.Pages;

import client.util.Link;

/**
 * A styled tab panel that routes its tab selections through the history mechanism.
 */
public abstract class NaviTabPanel extends StyledTabPanel
{
    public NaviTabPanel (Pages page)
    {
        _page = page;
        _handreg = addBeforeSelectionHandler(_onSelect);
    }

    /**
     * Configures the currently selected tab without triggering the route through history. This is
     * what a page should call to configure the selected tab based on information from its page
     * arguments.
     */
    public void activateTab (final int tabIdx)
    {
        _handreg.removeHandler();
        DeferredCommand.add(new Command() {
            public void execute () {
                selectTab(tabIdx);
                _handreg = addBeforeSelectionHandler(_onSelect);
            }
        });
    }

    protected abstract String getTabArgs (int tabidx);

    protected BeforeSelectionHandler<Integer> _onSelect = new BeforeSelectionHandler<Integer>() {
        public void onBeforeSelection (BeforeSelectionEvent<Integer> event) {
            event.cancel();
            Link.go(_page, getTabArgs(event.getItem()));
        }
    };

    protected Pages _page;
    protected HandlerRegistration _handreg;
}
