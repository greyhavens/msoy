//
// $Id$

package client.swiftly;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.swiftly.data.all.SwiftlyProject;

import client.shell.DynamicMessages;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.ui.PromptPopup;
import client.util.ClickCallback;
import client.util.Link;
import client.util.MsoyCallback;

/**
 * Displays the client interface for selecting or creating a swiftly project.
 */
public class ProjectSelectionPanel extends FlexTable
{
    public ProjectSelectionPanel ()
    {
        super();
        setStyleName("projectSelectionPanel");
        setCellPadding(5);
        setCellSpacing(0);

        int row = 0;
        setHTML(row, 0, CSwiftly.msgs.swiftlyIntro());
        getFlexCellFormatter().setColSpan(row++, 0, 2);

        // add the UI for displaying this member's projects and for creating a project
        setText(row, 0, CSwiftly.msgs.membersProjects());
        getFlexCellFormatter().setStyleName(row, 0, "Header");
        setText(row, 1, CSwiftly.msgs.startProject());
        getFlexCellFormatter().setStyleName(row++, 1, "Header");
        setWidget(row, 0, _membersProjectsPanel = new VerticalPanel());
        _membersProjectsPanel.setStyleName("membersProjects");
        setWidget(row++, 1, createNewProjectUI());

        // add a display for all remixable projects
        setText(row, 0, CSwiftly.msgs.remixableProjects());
        getFlexCellFormatter().setStyleName(row, 0, "Header");
        getFlexCellFormatter().setColSpan(row++, 0, 2);
        setWidget(row, 0, _remixableProjectsPanel = new VerticalPanel());
        getFlexCellFormatter().setColSpan(row++, 0, 2);

        // populate the data from the backend
        loadRemixableProjects();
        loadMembersProjects();
    }

    protected FlexTable createNewProjectUI ()
    {
        // a drop down to select the project type
        FlexTable table = new FlexTable();
        _projectTypes = new ListBox();
        _projectTypes.setStyleName("projectTypes");
        _projectTypes.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                updateSelectedProjectType();
            }
        });
        for (int i = 0; i < SwiftlyProject.PROJECT_TYPES.length; i++) {
            byte type = SwiftlyProject.PROJECT_TYPES[i];
            _projectTypes.addItem(_dmsgs.getString("itemType" + type),
                String.valueOf(type));
        }
        updateSelectedProjectType();

        // input fields to create a new project
        final TextBox projectName = new TextBox();
        projectName.setMaxLength(50);
        projectName.setVisibleLength(25);
        _remixable = new CheckBox();

        table.setText(0, 0, CSwiftly.msgs.projectName());
        table.setWidget(0, 1, projectName);
        table.setText(1, 0, CSwiftly.msgs.selectType());
        table.setWidget(1, 1, _projectTypes);
        table.setText(2, 0, CSwiftly.msgs.remixable());
        table.setWidget(2, 1, _remixable);

        Button create = new Button(CSwiftly.msgs.createProject());
        table.setWidget(3, 1, create);
        new ClickCallback<SwiftlyProject>(create) {
            public boolean callService () {
                String name = projectName.getText().trim();
                if (name.length() == 0) {
                    MsoyUI.error(CSwiftly.msgs.pleaseEnterProjectName());
                    return false;
                }
                CSwiftly.swiftlysvc.createProject(
                    name, _selectedProjectType, _remixable.isChecked(), this);
                return true;
            }
            public boolean gotResult (SwiftlyProject newProject) {
                Link.go(Pages.SWIFTLY, "" + newProject.projectId);
                return true;
            }
        };

        return table;
    }

    /**
     * Get the list of projects that are remixable.
     */
    protected void loadRemixableProjects ()
    {
        _remixableProjects = new ArrayList<SwiftlyProject>();
        CSwiftly.swiftlysvc.getRemixableProjects(new AsyncCallback<List<SwiftlyProject>>() {
            public void onSuccess (List<SwiftlyProject> projects) {
                _remixableProjects.addAll(projects);
                displayRemixableProjects();
            }
            public void onFailure (Throwable caught) {
                CSwiftly.log("loadRemixableProjects failed", caught);
                _remixableProjectsPanel.add(new Label(CSwiftly.serverError(caught)));
            }
        });
    }

    /**
     * Display the list of loaded remixable projects.
     */
    protected void displayRemixableProjects ()
    {
        _remixableProjectsPanel.clear();
        Iterator<SwiftlyProject> iter = _remixableProjects.iterator();
        if (!iter.hasNext()) {
            _remixableProjectsPanel.add(new Label(CSwiftly.msgs.noRemixableProjects()));
            return;
        }

        while (iter.hasNext()) {
            SwiftlyProject project = iter.next();
            Widget projectLink = Link.create(
                project.projectName, Pages.SWIFTLY, String.valueOf(project.projectId));
            _remixableProjectsPanel.add(projectLink);
        }
    }

    /**
     *  Get the list of projects this user is a collaborator on.
     */
    protected void loadMembersProjects ()
    {
        _membersProjects = new ArrayList<SwiftlyProject>();
        CSwiftly.swiftlysvc.getMembersProjects(new AsyncCallback<List<SwiftlyProject>>() {
            public void onSuccess (List<SwiftlyProject> projects) {
                _membersProjects.addAll(projects);
                displayMembersProjects();
            }
            public void onFailure (Throwable caught) {
                CSwiftly.log("loadMembersProjects failed", caught);
                _membersProjectsPanel.add(new Label(CSwiftly.serverError(caught)));
            }
        });
    }

    /**
     * Display the list of projects this user is a collaborator on
     */
    protected void displayMembersProjects ()
    {
        _membersProjectsPanel.clear();
        Iterator<SwiftlyProject> iter = _membersProjects.iterator();
        if (!iter.hasNext()) {
            _membersProjectsPanel.add(new Label(CSwiftly.msgs.noMembersProjects()));
            return;
        }

        while (iter.hasNext()) {
            SwiftlyProject project = iter.next();
            HorizontalPanel projectInfo = new HorizontalPanel();
            projectInfo.add(Link.create(
                project.projectName, Pages.SWIFTLY, String.valueOf(project.projectId)));
            /* TODO: Disabled until we figure out how delete should actually work
            if (CSwiftly.ident.memberId == project.ownerId) {
                projectInfo.add(new DeleteButton(project, ProjectSelectionPanel.this));
            }
            */
            _membersProjectsPanel.add(projectInfo);
        }
    }

    /**
     * Called after a project has been removed on the back end to update the project display.
     */
    protected void projectWasRemoved (SwiftlyProject project)
    {
        _remixableProjects.remove(project);
        displayRemixableProjects();
        _membersProjects.remove(project);
        displayMembersProjects();
    }

    protected void updateSelectedProjectType ()
    {
        int tx = _projectTypes.getSelectedIndex();
        if (tx == -1) {
            return;
        }
        _selectedProjectType = Byte.parseByte(_projectTypes.getValue(tx));
    }

    protected static class DeleteButton extends Button
        implements Command
    {
        public DeleteButton (SwiftlyProject project, ProjectSelectionPanel panel)
        {
            super(CSwiftly.msgs.deleteButton());
            _project = project;
            _panel = panel;
            addClickListener(
                new PromptPopup(CSwiftly.msgs.projectDeletePrompt(_project.projectName), this));
        }

        // from interface Command
        public void execute ()
        {
            CSwiftly.swiftlysvc.deleteProject(_project.projectId, new MsoyCallback<Void>() {
                public void onSuccess (Void result) {
                    _panel.projectWasRemoved(_project);
                }
            });
        }

        protected SwiftlyProject _project;
        protected ProjectSelectionPanel _panel;
    }

    protected VerticalPanel _membersProjectsPanel;
    protected VerticalPanel _remixableProjectsPanel;
    protected List<SwiftlyProject> _membersProjects;
    protected List<SwiftlyProject> _remixableProjects;

    protected ListBox _projectTypes;
    protected CheckBox _remixable;
    protected byte _selectedProjectType;

    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);
}
