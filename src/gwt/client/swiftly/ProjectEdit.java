//
// $Id$

package client.swiftly;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.gwt.ui.InlineLabel;

import client.util.BorderedDialog;

/**
 * Display a dialog to edit a project.
 */
public class ProjectEdit extends BorderedDialog
{
    /**
     * A callback interface for classes that want to know when a project was committed.
     */
    public static interface ProjectEditListener {
        public void projectSubmitted(SwiftlyProject project);
    }
        
    public ProjectEdit (SwiftlyProject project, ProjectEditListener listener)
    {
        super();

        _project = project;
        _listener = listener;

        VerticalPanel contents = (VerticalPanel)_contents;
        _header.add(createTitleLabel(_project.projectName, "ProjectName"));

        TextBox projectName = new TextBox();
        projectName.setText(_project.projectName);
        projectName.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                _project.projectName = ((TextBox)sender).getText().trim();
            }
        });
        contents.add(new InlineLabel(CSwiftly.msgs.projectName()));
        contents.add(projectName);
        _remixable = new CheckBox(CSwiftly.msgs.remixable());
        _remixable.setChecked(_project.remixable);
        _remixable.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                _project.remixable = _remixable.isChecked();
            }
        });
        contents.add(_remixable);

        // Submit button
        _footer.add(new Button(CSwiftly.msgs.submit(), new ClickListener() {
            public void onClick (Widget sender) {
                commitEdit();
            }
        }));

        // Cancel button
        _footer.add(new Button(CSwiftly.msgs.cancel(),new ClickListener() {
            public void onClick (Widget sender) {
                closeDialog();
            }
        }));
    }

    // from BorderedDialog.  This is called in the super constructor, so no UI components
    // that depend on members that are set in this object's constructor can be used here.
    public Widget createContents ()
    {
        VerticalPanel contents = new VerticalPanel();
        contents.setStyleName("projectEdit");
        return contents;
    }

    protected void commitEdit()
    {
        // save the project record
        CSwiftly.swiftlysvc.updateProject(CSwiftly.creds, _project, new AsyncCallback() {
            public void onSuccess (Object result) {
                closeDialog();
            }
            public void onFailure (Throwable cause) {
                CSwiftly.serverError(cause);
            }
        });
    }

    protected void closeDialog()
    {
        hide();
        if (_listener != null) {
            _listener.projectSubmitted(_project);
        }
    }

    protected SwiftlyProject _project;
    protected ProjectEditListener _listener;
    protected CheckBox _remixable;
}
