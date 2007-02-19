//
// $Id$

package client.swiftly;

import java.util.ArrayList;
import java.util.Iterator;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
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
        super();
        setWidth("100%");
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
                    _projectsContainer.add(new InlineLabel(CSwiftly.msgs.noProjects()));
                } else {
                    _projectsContainer.add(new Label(CSwiftly.msgs.selectProject()));
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

        FlexTable createProject = new FlexTable();
        createProject.setStyleName("createProject");
        // TODO templates drop down
        final TextBox projectText = new TextBox();
        projectText.setMaxLength(50);
        projectText.setVisibleLength(25);
        ClickListener doCreate = new ClickListener() {
            public void onClick (Widget sender) {
                createProject(projectText.getText());
            }
        };
        projectText.addKeyboardListener(new EnterClickAdapter(doCreate));
        createProject.setWidget(0, 0, projectText);
        createProject.setWidget(0, 1, new Button(CSwiftly.msgs.createProject(), doCreate));
        table.setWidget(1, 0, createProject);
    }

    protected FlowPanel _projectsContainer;

    protected void createProject (final String projectName)
    {
        SwiftlyProject project = new SwiftlyProject();
        project.projectName = projectName;
        CSwiftly.swiftlysvc.createProject(CSwiftly.creds, project, new AsyncCallback() {
            public void onSuccess (Object result) {
                // _groupListContainer.setModel(new SimpleDataModel((List)result));
                CSwiftly.log("Project created");
                // TODO: print project created and refresh project list
            }
            public void onFailure (Throwable caught) {
                CGroup.log("createProject(" + project + ") failed", caught);
                // TODO: addError(CSwiftly.serverError(caught));
            }
        });
    }
}
