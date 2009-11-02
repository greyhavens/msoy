//
// $Id$

package client.apps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
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
        editor.addRow(_msgs.fbTemplCodeLabel(), _code = MsoyUI.createTextBox("", 15, 8),
            new Command() {
            @Override public void execute () {
                if (_code.getText().trim().equals("")) {
                    throw new ConfigException(_msgs.fbTemplCodeRequiredErr());
                }
            }
        });

        editor.addRow(
            _msgs.fbTemplVariantLabel(), _variant = MsoyUI.createTextBox("", 6, 4), null);

        editor.addRow(_msgs.fbTemplBundleIdLabel(), _bundleId = MsoyUI.createTextBox("", 20, 12),
            new Command() {
            @Override public void execute () {
                try {
                    long val = Long.parseLong(_bundleId.getText().trim());
                    if (val < 0) {
                        throw new NumberFormatException();   
                    }
                } catch (NumberFormatException nfe) {
                    throw new ConfigException(_msgs.fbTemplBundleIdFormatErr());
                }
            }
        });

        editor.addRow(
            _msgs.fbTemplCaptionLabel(), _caption = MsoyUI.createTextBox("", 250, 60), null);

        editor.addRow(_msgs.fbTemplDescriptionLabel(),
            _description = MsoyUI.createTextBox("", 250, 60), null);

        editor.addRow(
            _msgs.fbTemplPromptLabel(), _prompt = MsoyUI.createTextBox("", 60, 30), null);

        editor.addRow(
            _msgs.fbTemplLinkLabel(), _link = MsoyUI.createTextBox("", 40, 20), null);

        editor.addRow("", new Button (_msgs.fbTemplAddNewBtn(), new ClickHandler() {
            @Override public void onClick (ClickEvent event) {
                if (!editor.bindChanges()) {
                    return;
                }
                FacebookTemplate newTmpl = new FacebookTemplate(_code.getText().trim(),
                    _variant.getText().trim(), Long.parseLong(_bundleId.getText().trim()));
                if (_templates.indexOf(newTmpl) != -1) {
                    FacebookTemplate.Key key = newTmpl.key;
                    MsoyUI.error(_msgs.fbTemplErrDuplicate(key.code, key.variant));
                    return;
                }
                newTmpl.caption = _caption.getText().trim();
                newTmpl.description = _description.getText().trim();
                newTmpl.prompt = _prompt.getText().trim();
                newTmpl.linkText = _link.getText().trim();
                _added.add(newTmpl);
                _templates.add(newTmpl);
                _display.update();
            }
        }), null);

        Button save = editor.addSaveRow();
        new ClickCallback<Void>(save) {
            @Override public boolean callService () {
                _appsvc.updateTemplates(_appId, _added, _removed, _abled, this);
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

    public void setFields (FacebookTemplate templ)
    {
        _code.setText(templ.key.code);
        _variant.setText(templ.key.variant);
        _bundleId.setText(""+templ.bundleId);
        _caption.setText(templ.caption);
        _description.setText(templ.description);
        _prompt.setText(templ.prompt);
        _link.setText(templ.linkText);
    }

    public void setTemplates (List<FacebookTemplate> result)
    {
        _original = result;
        _templates = new ArrayList<FacebookTemplate>();
        _templates.addAll(result);
        _removed = new HashSet<FacebookTemplate.Key>();
        _added = new HashSet<FacebookTemplate>();
        _abled = new HashMap<FacebookTemplate.Key, Boolean>();
        _display.update();
    }

    protected static Label setAbleButtonLabel (Label label, FacebookTemplate template)
    {
        label.setText(template.enabled ? _msgs.fbTemplDisableBtn() : _msgs.fbTemplEnableBtn());
        return label;
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

            final int CODE = 0, VARIANT = 1, BUNDLE_ID = 2, CAPTION = 3, DESCRIP = 4,
                 PROMPT = 5, LINK_TEXT = 6, COPY_BTN = 7, ENABLING_BTN = 8, DELETE_BTN = 9;

            int row = 0;
            setText(row, CODE, _msgs.fbTemplCodeHdr(), 1, "Header", "Code");
            setText(row, VARIANT, _msgs.fbTemplVariantHdr(), 1, "Header");
            setText(row, BUNDLE_ID, _msgs.fbTemplBundleIdHdr(), 1, "Header");
            setText(row, CAPTION, _msgs.fbTemplCaptionHdr(), 1, "Header", "Caption");
            setText(row, DESCRIP, _msgs.fbTemplDescripHdr(), 1, "Header", "Description");
            setText(row, PROMPT, _msgs.fbTemplPromptHdr(), 1, "Header", "Prompt");
            setText(row, LINK_TEXT, _msgs.fbTemplLinkTextHdr(), 1, "Header", "Link");
            getRowFormatter().setStyleName(row++, "Row");

            for (FacebookTemplate template : _templates) {
                setText(row, CODE, template.key.code, 1, "Code");
                setText(row, VARIANT, template.key.variant);
                setText(row, BUNDLE_ID, String.valueOf(template.bundleId));
                setText(row, CAPTION, template.caption);
                setText(row, DESCRIP, template.description);
                setText(row, PROMPT, template.prompt);
                setText(row, LINK_TEXT, template.linkText);

                final FacebookTemplate ftemplate = template;

                // copy button
                setWidget(row, COPY_BTN, MsoyUI.createActionLabel(_msgs.fbTemplCopyBtn(),
                    new ClickHandler() {
                    @Override public void onClick (ClickEvent event) {
                        setFields(ftemplate);
                    }
                }));

                // enabling button
                setWidget(row, ENABLING_BTN, setAbleButtonLabel(MsoyUI.createActionLabel("",
                    new ClickHandler() {
                        @Override public void onClick (ClickEvent event) {
                            ftemplate.enabled = !ftemplate.enabled;
                            setAbleButtonLabel(((Label)event.getSource()), ftemplate);
                            _abled.put(ftemplate.key, Boolean.valueOf(ftemplate.enabled));
                        }
                    }), template));

                // delete button
                setWidget(row, DELETE_BTN, MsoyUI.createCloseButton(new ClickHandler() {
                    @Override public void onClick (ClickEvent event) {
                        int idx = _templates.indexOf(ftemplate);
                        removeRow(idx + 1); // "1" for header row
                        _templates.remove(idx);
                        _added.remove(ftemplate); // just in case it was added then removed
                        _abled.remove(ftemplate.key);
                        if (_original.indexOf(ftemplate) != -1) {
                            _removed.add(ftemplate.key);
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
    protected Set<FacebookTemplate> _added;
    protected Set<FacebookTemplate.Key> _removed;
    protected Map<FacebookTemplate.Key, Boolean> _abled; // en- or dis-
    protected TemplatesList _display;
    protected TextBox _code, _variant, _bundleId, _caption, _description, _prompt, _link;

    protected static final AppsMessages _msgs = GWT.create(AppsMessages.class);
    protected static final AppServiceAsync _appsvc = GWT.create(AppService.class);
}
