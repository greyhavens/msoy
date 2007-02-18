//
// $Id$

package client.swiftly;

import java.util.ArrayList;
import java.util.Iterator;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.Hyperlink;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.data.SwiftlyProject;

/**
 * Displays the client interface for selecting or creating a swiftly project.
 */
public class ProjectSelectionPanel extends VerticalPanel
{
    public ProjectSelectionPanel ()
    {
        setStyleName("projectSelectionPanel");

        FlexTable table = new FlexTable();
        DOM.setStyleAttribute(table.getElement(), "width", "100%");
        add(table);

        _projectsContainer = new FlowPanel();
        _projectsContainer.setStyleName("projectsContainer");
        table.setWidget(0, 0, _projectsContainer);

        CSwiftly.swiftlysvc.getProjects(CSwiftly.creds, new AsyncCallback() {
            public void onSuccess (Object result) {
                Iterator iter = ((ArrayList)result).iterator();
                if (!iter.hasNext()) {
                    // TODO _projectsContainer.add(new InlineLabel(CSwiftly.msgs.listNoProjects()));
                } else {
                    _projectsContainer.add(new Label("Please select one of your projects:"));
                    while (iter.hasNext()) {
                        final SwiftlyProject project = (SwiftlyProject)iter.next();
                        Hyperlink projectLink = new Hyperlink(
                            project.projectName, String.valueOf(project.projectId));
                        DOM.setStyleAttribute(projectLink.getElement(), "display", "inline");
                        _projectsContainer.add(projectLink);
                        if (iter.hasNext()) {
                            _projectsContainer.add(new InlineLabel(", "));
                        }
                    }
                }
            }
            public void onFailure (Throwable caught) {
                CSwiftly.log("getProjects failed", caught);
                // TODO addError(CSwiftly.serverError(caught));
            }
        });

        add(new Label("Create a project from a template:"));
        // TODO project creation drop down for templates etc.
    }

    protected FlowPanel _projectsContainer;
}
