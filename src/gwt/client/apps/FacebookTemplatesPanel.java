//
// $Id$

package client.apps;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.apps.gwt.AppInfo;
import com.threerings.msoy.apps.gwt.AppService;
import com.threerings.msoy.apps.gwt.AppServiceAsync;

import com.threerings.msoy.facebook.gwt.FacebookTemplate;

import client.edutil.EditorTable;
import client.edutil.EditorUtil.ConfigException;
import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.InfoCallback;

/**
 * Interface for adding, removing and editing facebook templates.
 */
public class FacebookTemplatesPanel extends FlowPanel
{
    public FacebookTemplatesPanel (AppInfo info)
    {
        addStyleName("facebookTemplates");
        _appId = info.appId;

        // view templates
        add(MsoyUI.createHTML(_msgs.fbTemplTitle(info.name), "Title"));
        add(_display = new TemplatesList());

        // add new template
        final EditorTable editor = new EditorTable();
        add(editor);
        editor.addWidget(MsoyUI.createHTML(_msgs.fbTemplAddTitle(), "Title"), 2);
        final TextBox code = MsoyUI.createTextBox("", 15, 8);
        editor.addRow(_msgs.fbTemplCodeLabel(), code,
            new Command() {
            @Override public void execute () {
                if (code.getText().trim().equals("")) {
                    throw new ConfigException(_msgs.fbTemplCodeRequiredErr());
                }
            }
        });

        final TextBox variant = MsoyUI.createTextBox("", 6, 4);
        editor.addRow(_msgs.fbTemplVariantLabel(), variant, null);

        final TextBox bundleId = MsoyUI.createTextBox("", 20, 12);
        editor.addRow(_msgs.fbTemplBundleIdLabel(), bundleId, new Command() {
            @Override public void execute () {
                try {
                    long val = Long.parseLong(bundleId.getText().trim());
                    if (val < 0) {
                        throw new NumberFormatException();   
                    }
                } catch (NumberFormatException nfe) {
                    throw new ConfigException(_msgs.fbTemplBundleIdFormatErr());
                }
            }
        });

        editor.addRow("", new Button (_msgs.fbTemplAddNewBtn(), new ClickHandler() {
            @Override public void onClick (ClickEvent event) {
                if (!editor.bindChanges()) {
                    return;
                }
                FacebookTemplate newTmpl = new FacebookTemplate(code.getText().trim(),
                    variant.getText().trim(), Long.parseLong(bundleId.getText().trim()));
                if (_templates.indexOf(newTmpl) != -1) {
                    MsoyUI.error(_msgs.fbTemplErrDuplicate(newTmpl.code, newTmpl.variant));
                    return;
                }
                _added.add(newTmpl);
                _templates.add(newTmpl);
                _display.update();
            }
        }), null);

        Button save = editor.addSaveRow();
        new ClickCallback<Void>(save) {
            @Override public boolean callService () {
                _appsvc.updateTemplates(_appId, _added, _removed, this);
                return true;
            }
            @Override public boolean gotResult (Void result) {
                setTemplates(_templates);
                MsoyUI.info(_msgs.fbTemplSaved());
                return true;
            }
        };

        refresh();
    }

    public void refresh ()
    {
        _appsvc.loadTemplates(_appId,
            new InfoCallback<List<FacebookTemplate>> () {
                @Override public void onSuccess (List<FacebookTemplate> result) {
                    setTemplates(result);
                }
            });
    }

    public void setTemplates (List<FacebookTemplate> result)
    {
        _original = result;
        _templates = new ArrayList<FacebookTemplate>();
        _templates.addAll(result);
        _removed = new HashSet<FacebookTemplate>();
        _added = new HashSet<FacebookTemplate>();
        _display.update();
    }

    protected class TemplatesList extends SmartTable
    {
        public TemplatesList ()
        {
            super("templatesList", 5, 0);
            setWidget(0, 0, MsoyUI.createNowLoading());
        }

        public void update ()
        {
            while (getRowCount() > 0) {
                removeRow(getRowCount() - 1);
            }

            if (_templates.size() == 0) {
                setText(0, 0, _msgs.fbTemplEmpty());
                return;
            }

            final int CODE = 0, VARIANT = 1, BUNDLE_ID = 2, DELETE_BTN = 3;

            int row = 0;
            setText(row, CODE, _msgs.fbTemplCodeHdr(), 1, "Header", "Code");
            setText(row, VARIANT, _msgs.fbTemplVariantHdr(), 1, "Header");
            setText(row, BUNDLE_ID, _msgs.fbTemplBundleIdHdr(), 1, "Header");
            getRowFormatter().setStyleName(row++, "Row");

            for (FacebookTemplate template : _templates) {
                setText(row, CODE, template.code, 1, "Code");
                setText(row, VARIANT, template.variant);
                setText(row, BUNDLE_ID, String.valueOf(template.bundleId));

                // delete button
                final FacebookTemplate ftemplate = template;
                setWidget(row, DELETE_BTN, MsoyUI.createCloseButton(new ClickHandler() {
                    @Override public void onClick (ClickEvent event) {
                        int idx = _templates.indexOf(ftemplate);
                        removeRow(idx + 1); // "1" for header row
                        _templates.remove(idx);
                        if (_original.indexOf(ftemplate) != -1) {
                            _removed.add(ftemplate);
                        }
                        update();
                    }
                }));

                getRowFormatter().setStyleName(row, "Row");
                if (row % 2 == 1) {
                    getRowFormatter().addStyleName(row, "AltRow");
                }
                row++;
            }
        }
    }

    protected int _appId;
    protected List<FacebookTemplate> _original, _templates;
    protected Set<FacebookTemplate> _added, _removed;
    protected TemplatesList _display;

    protected static final AppsMessages _msgs = GWT.create(AppsMessages.class);
    protected static final AppServiceAsync _appsvc = GWT.create(AppService.class);
}
