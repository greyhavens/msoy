//
// $Id$

package client.swiftly;

import java.util.List;
import java.util.Iterator;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.msoy.item.data.all.Item;

import client.shell.Application;
import client.util.ClickCallback;
import client.util.MsoyUI;

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
        setWidget(row, 0, _membersProjects = new VerticalPanel());
        _membersProjects.setStyleName("membersProjects");
        setWidget(row++, 1, createCreateUI());

        // add a display for all remixable projects
        setText(row, 0, CSwiftly.msgs.remixableProjects());
        getFlexCellFormatter().setStyleName(row, 0, "Header");
        getFlexCellFormatter().setColSpan(row++, 0, 2);
        setWidget(row, 0, _remixableProjects = new VerticalPanel());
        getFlexCellFormatter().setColSpan(row++, 0, 2);

        // populate the data from the backend
        loadRemixableProjects();
        loadMembersProjects();
    }

    protected FlexTable createCreateUI ()
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
            _projectTypes.addItem(CSwiftly.dmsgs.getString("itemType" + type),
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
        new ClickCallback(create) {
            public boolean callService () {
                String name = projectName.getText().trim();
                if (name.length() == 0) {
                    MsoyUI.error(CSwiftly.msgs.pleaseEnterProjectName());
                    return false;
                }
                CSwiftly.swiftlysvc.createProject(CSwiftly.ident, name, _selectedProjectType,
                                                  _remixable.isChecked(), this);
                return true;
            }
            public boolean gotResult (Object result) {
                SwiftlyProject newProject = (SwiftlyProject)result;
                History.newItem(Application.createLinkToken("swiftly", "" + newProject.projectId));
                return true;
            }
        };

        return table;
    }

    // get the list of projects that are remixable
    protected void loadRemixableProjects ()
    {
        CSwiftly.swiftlysvc.getRemixableProjects(CSwiftly.ident, new AsyncCallback() {
            public void onSuccess (Object result) {
                Iterator iter = ((List)result).iterator();
                if (!iter.hasNext()) {
                    _remixableProjects.add(new Label(CSwiftly.msgs.noRemixableProjects()));
                } else {
                    while (iter.hasNext()) {
                        final SwiftlyProject project = (SwiftlyProject)iter.next();
                        Hyperlink projectLink = Application.createLink(
                            project.projectName, "swiftly", String.valueOf(project.projectId));
                        DOM.setStyleAttribute(projectLink.getElement(), "display", "inline");
                        _remixableProjects.add(projectLink);
                    }
                }
            }
            public void onFailure (Throwable caught) {
                CSwiftly.log("getMembersProjects failed", caught);
                _remixableProjects.add(new Label(CSwiftly.serverError(caught)));
            }
        });
    }

    // get the list of projects this user is a collaborator on
    protected void loadMembersProjects ()
    {
        CSwiftly.swiftlysvc.getMembersProjects(CSwiftly.ident, new AsyncCallback() {
            public void onSuccess (Object result) {
                Iterator iter = ((List)result).iterator();
                if (!iter.hasNext()) {
                    _membersProjects.add(new Label(CSwiftly.msgs.noMembersProjects()));
                } else {
                    while (iter.hasNext()) {
                        final SwiftlyProject project = (SwiftlyProject)iter.next();
                        Hyperlink projectLink = Application.createLink(
                            project.projectName, "swiftly", String.valueOf(project.projectId));
                        DOM.setStyleAttribute(projectLink.getElement(), "display", "inline");
                        _membersProjects.add(projectLink);
                    }
                }
            }
            public void onFailure (Throwable caught) {
                CSwiftly.log("getMembersProjects failed", caught);
                _membersProjects.add(new Label(CSwiftly.serverError(caught)));
            }
        });
    }

    protected void updateSelectedProjectType ()
    {
        int tx = _projectTypes.getSelectedIndex();
        if (tx == -1) {
            return;
        }
        _selectedProjectType = Byte.parseByte(_projectTypes.getValue(tx));
    }

    protected VerticalPanel _membersProjects;
    protected VerticalPanel _remixableProjects;

    protected ListBox _projectTypes;
    protected CheckBox _remixable;
    protected byte _selectedProjectType;
}
