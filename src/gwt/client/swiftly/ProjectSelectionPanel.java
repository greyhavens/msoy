//
// $Id$

package client.swiftly;

import java.util.List;
import java.util.Iterator;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.Hyperlink;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.msoy.web.data.SwiftlyProjectType;

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

        _errorContainer = new HorizontalPanel();
        add(_errorContainer);

        FlexTable table = new FlexTable();
        add(table);

        _projectsContainer = new VerticalPanel();
        _projectsContainer.setStyleName("projectsContainer");
        ScrollPanel chooser = new ScrollPanel(_projectsContainer);
        // TODO this needs to have dimensions set to actually have any use but these should be
        // relative etc.
        chooser.setSize("300px", "150px");
        _projectHeader = new HorizontalPanel();
        table.setWidget(0, 0, _projectHeader);
        table.setWidget(1, 0, chooser);

        // get the list of projects for this user
        CSwiftly.swiftlysvc.getProjects(CSwiftly.creds, new AsyncCallback() {
            public void onSuccess (Object result) {
                Iterator iter = ((List)result).iterator();
                if (!iter.hasNext()) {
                    _projectHeader.add(new Label(CSwiftly.msgs.noProjects()));
                } else {
                    _projectHeader.add(new Label(CSwiftly.msgs.selectProject()));
                    while (iter.hasNext()) {
                        final SwiftlyProject project = (SwiftlyProject)iter.next();
                        Hyperlink projectLink = new Hyperlink(
                            project.projectName, String.valueOf(project.projectId));
                        DOM.setStyleAttribute(projectLink.getElement(), "display", "inline");
                        _projectsContainer.add(projectLink);
                    }
                }
            }
            public void onFailure (Throwable caught) {
                CSwiftly.log("getProjects failed", caught);
                addError(CSwiftly.serverError(caught));
            }
        });

        _typesContainer = new ListBox();
        _typesContainer.setStyleName("typesContainer");
        _typesContainer.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                int tx = _typesContainer.getSelectedIndex();
                if (tx == -1) {
                    return;
                }
                _selectedType = Integer.parseInt(_typesContainer.getValue(tx));
            }
        });

        // get the list of project types for this user
        CSwiftly.swiftlysvc.getProjectTypes(CSwiftly.creds, new AsyncCallback() {
            public void onSuccess (Object result) {
                Iterator iter = ((List)result).iterator();
                if (!iter.hasNext()) {
                    _projectsContainer.add(new InlineLabel(CSwiftly.msgs.noTypes()));
                } else {
                    while (iter.hasNext()) {
                        final SwiftlyProjectType pType = (SwiftlyProjectType)iter.next();
                        _typesContainer.addItem(pType.typeName, String.valueOf(pType.typeId));
                    }
                    _typesContainer.setSelectedIndex(0);
                    _selectedType = Integer.parseInt(_typesContainer.getValue(0));
                }
            }
            public void onFailure (Throwable caught) {
                CSwiftly.log("getProjectTypes failed", caught);
                addError(CSwiftly.serverError(caught));
            }
        });

        final TextBox projectText = new TextBox();
        projectText.setMaxLength(50);
        projectText.setVisibleLength(25);
        ClickListener doCreate = new ClickListener() {
            public void onClick (Widget sender) {
                createProject(projectText.getText());
            }
        };
        projectText.addKeyboardListener(new EnterClickAdapter(doCreate));
        table.setWidget(2, 0, projectText);
        table.setWidget(2, 1, new Button(CSwiftly.msgs.createProject(), doCreate));
        table.setWidget(3, 0, _typesContainer);
        table.setWidget(3, 1, new InlineLabel(CSwiftly.msgs.selectType()));
    }

    protected VerticalPanel _projectsContainer;
    protected ListBox _typesContainer;
    protected HorizontalPanel _projectHeader;
    protected HorizontalPanel _errorContainer;
    protected int _selectedType;

    protected void createProject (final String projectName)
    {
        CSwiftly.swiftlysvc.createProject(
                CSwiftly.creds, projectName, _selectedType, new AsyncCallback() {
            public void onSuccess (Object result) {
                CSwiftly.log("Project created: " + projectName);
                // TODO: or we could just refresh the list of projects
                SwiftlyProject newProject = (SwiftlyProject)result;
                History.newItem("" + newProject.projectId);
            }
            public void onFailure (Throwable caught) {
                CSwiftly.log("createProject(" + projectName + ") failed", caught);
                addError(CSwiftly.serverError(caught));
            }
        });
    }

    protected void addError (String error)
    {
        _errorContainer.add(new Label(error));
    }

    protected void clearErrors ()
    {
        _errorContainer.clear();
    }

}
