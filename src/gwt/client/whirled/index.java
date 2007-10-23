//
// $Id$

package client.whirled;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.client.DeploymentConfig;

import client.msgs.MsgsEntryPoint;
import client.shell.Args;
import client.shell.Page;
import client.util.MsoyUI;

public class index extends MsgsEntryPoint
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new index();
            }
        };
    }

    // @Override // from Page
    public void onHistoryChanged (Args args)
    {
        // if we're not logged in, always display whirledwide; if we are logged in and we're just
        // hitting the start/default page, display my whirled instead.
        String action = (CWhirled.creds == null) ? "whirledwide" : args.get(0, "mywhirled");

        PopulationDisplay popDisplay = new PopulationDisplay() {
            public void displayPopulation (int population) {
                // This is a hack to get the population into the usual tabs spot...
                VerticalPanel container = new VerticalPanel();
                container.setVerticalAlignment(VerticalPanel.ALIGN_BOTTOM);
                container.add(WidgetUtil.makeShim(5, 3));
                Label popLabel = new Label(CWhirled.msgs.populationDisplay("" + population));
                popLabel.setStyleName("PopulationDisplay");
                container.add(popLabel);
                setPageTabs(container);
            }
        };
        if ("whirledwide".equals(action)) {
            setPageTitle(CWhirled.msgs.titleWhirledwide());
            setContent(new Whirledwide(popDisplay), true, false);
        } else if ("mywhirled".equals(action)) {
            setPageTitle(CWhirled.msgs.titleMyWhirled());
            setContent(new MyWhirled(popDisplay));
        }
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return WHIRLED;
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CWhirled.msgs = (WhirledMessages)GWT.create(WhirledMessages.class);
    }
}
