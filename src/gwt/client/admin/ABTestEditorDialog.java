//
// $Id: IssueInvitesDialog.java 9643 2008-06-30 21:36:32Z nathan $

package client.admin;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.web.data.ABTest;

import client.shell.Page;
import client.shell.ShellMessages;
import client.util.BorderedDialog;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.ServiceUtil;

/**
 * Display a dialog for admins to create or edit an a/b test record
 */
public class ABTestEditorDialog extends BorderedDialog
{
    /**
     * If test is null, display the form for creating a new test.  Otherwise, display
     * the form for editing an existing test.
     * @param test If supplied, edit this test, or create a new one if null
     * @param parent If supplied, refresh the data on the parent list panel after create/update,
     * otherwise navigate to the list of tests.
     */
    public ABTestEditorDialog (ABTest test, ABTestListPanel parent)
    {
        _parent = parent;

        if (test == null) {
            _isNewTest = true;
            setHeaderTitle(CAdmin.msgs.abTestCreateTitle());
            _test = new ABTest();
        } else {
            _isNewTest = false;
            setHeaderTitle(CAdmin.msgs.abTestEditTitle());
            _test = test;
        }

        FlowPanel contents = MsoyUI.createFlowPanel("abTestEditorDialog");
        setContents(contents);

        final TextBox name = new TextBox();
        contents.add(new FormElement(CAdmin.msgs.abTestNameLabel(), name));
        name.setMaxLength(ABTest.MAX_NAME_LENGTH);
        name.setText(_test.name);
        name.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                _test.name = name.getText().trim();
            }
        });

        final TextArea description = new TextArea();
        contents.add(new FormElement(CAdmin.msgs.abTestDescriptionLabel(), description));
        description.setText(_test.description);
        description.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                _test.description = description.getText().trim();
            }
        });

        final CheckBox enabled = new CheckBox();
        contents.add(new FormElement(CAdmin.msgs.abTestEnabledLabel(), enabled));
        enabled.setChecked(_test.enabled);
        enabled.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                if (!_test.enabled && enabled.isChecked()) {
                    if (_test.started != null) {
                        MsoyUI.info(CAdmin.msgs.abTestEnabledWarning());
                    }
                    _test.started = new Date();
                    _test.enabled = true;
                } else if (_test.enabled && !enabled.isChecked()) {
                    _test.ended = new Date();
                    _test.enabled = false;
                }
            }
        });

        final TextBox numGroups = new TextBox();
        contents.add(new FormElement(CAdmin.msgs.abTestNumGroupsLabel(), numGroups));
        numGroups.setMaxLength(3);
        numGroups.setText(_test.numGroups+"");
        numGroups.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                try {
                    if (_test.started != null) {
                        numGroups.setText(_test.numGroups+"");
                        MsoyUI.error(CAdmin.msgs.abTestNumGroupsDisabled());
                    } else {
                        _test.numGroups = Integer.parseInt(numGroups.getText().trim());
                        if (_test.numGroups < 2) {
                            MsoyUI.error(CAdmin.msgs.abTestNumGroupsNaN());
                        }
                    }
                } catch (NumberFormatException e) {
                    MsoyUI.error(CAdmin.msgs.abTestNumGroupsNaN());
                }
            }
        });

        final CheckBox onlyNewVisitors = new CheckBox();
        contents.add(new FormElement(CAdmin.msgs.abTestOnlyNewVisitorsLabel(), onlyNewVisitors));
        onlyNewVisitors.setChecked(_test.onlyNewVisitors);
        onlyNewVisitors.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                _test.onlyNewVisitors = onlyNewVisitors.isChecked();
            }
        });

        final TextBox affiliate = new TextBox();
        contents.add(new FormElement(CAdmin.msgs.abTestAffiliateLabel(), affiliate));
        affiliate.setMaxLength(ABTest.MAX_AFFILIATE_LENGTH);
        affiliate.setText(_test.affiliate);
        affiliate.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                _test.affiliate = affiliate.getText().trim();
            }
        });

        final TextBox vector = new TextBox();
        contents.add(new FormElement(CAdmin.msgs.abTestVectorLabel(), vector));
        vector.setMaxLength(ABTest.MAX_VECTOR_LENGTH);
        vector.setText(_test.vector);
        vector.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                _test.vector = vector.getText().trim();
            }
        });

        final TextBox creative = new TextBox();
        contents.add(new FormElement(CAdmin.msgs.abTestCreativeLabel(), creative));
        creative.setMaxLength(ABTest.MAX_CREATIVE_LENGTH);
        creative.setText(_test.creative);
        creative.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                _test.creative = creative.getText().trim();
            }
        });

        Button submit = MsoyUI.createCrUpdateButton(_isNewTest, new ClickListener() {
            public void onClick (Widget widget) {
                commitEdit();
            }
        });

        Button cancel = new Button(_cmsgs.cancel());
        cancel.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                ABTestEditorDialog.this.hide();
            }
        });
        contents.add(MsoyUI.createButtonPair(cancel, submit));
    }

    /**
     * Called when the user clicks the "save" button to commit their edits or create a new item.
     */
    protected void commitEdit ()
    {
        // display any validation error messages
        try {
            _test.validate();
        } catch (Exception e) {
            MsoyUI.error(e.getMessage());
            return;
        }

        if (_isNewTest) {
            _adminsvc.createTest(CAdmin.ident, _test, new MsoyCallback<Void>() {
                public void onSuccess (Void result) {
                    MsoyUI.info(CAdmin.msgs.abTestCreated());
                    ABTestEditorDialog.this.hide();
                    if (_parent != null) {
                        _parent.refresh();
                    } else {
                        Link.go(Page.ADMIN, "testlist");
                    }
                }
            });

        }

        else {
            _adminsvc.updateTest(CAdmin.ident, _test, new MsoyCallback<Void>() {
                public void onSuccess (Void result) {
                    MsoyUI.info(CAdmin.msgs.abTestUpdated());
                    ABTestEditorDialog.this.hide();
                    if (_parent != null) {
                        _parent.refresh();
                    } else {
                        Link.go(Page.ADMIN, "testlist");
                    }
                }
            });
        }
    }

    /**
     * Form element plus label
     */
    protected static class FormElement extends FlowPanel
    {
        public FormElement (String labelText, Widget widget) {
            add(MsoyUI.createLabel(labelText, "Label"));
            add(MsoyUI.createSimplePanel("Element", widget));
        }
    }

    /** Are we creating a new test (true), or editing an existing one (false)? */
    protected final boolean _isNewTest;

    /** Data for the test */
    protected final ABTest _test;

    /** Parent panel needs to have data refreshed after create/update */
    protected final ABTestListPanel _parent;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
