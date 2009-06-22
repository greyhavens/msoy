//
// $Id$

package client.adminz;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.facebook.gwt.FacebookTemplate;

import client.ui.MsoyUI;
import client.util.ClickCallback;

/**
 * Interface for adding, removing and editing facebook templates.
 */
public class FacebookTemplatesPanel extends AdminDataPanel<List<FacebookTemplate>>
{
    public FacebookTemplatesPanel ()
    {
        super("facebookTemplates");
        _adminsvc.loadFacebookTemplates(createCallback());
    }

    @Override
    protected void init (List<FacebookTemplate> data)
    {
        add(_display = new SmartTable());
        _display.setText(0, 0, _msgs.fbTemplCodeHdr());
        _display.setText(0, 1, _msgs.fbTemplBundleIdHdr());
        for (FacebookTemplate template : data) {
            addRow(template);
        }
        HorizontalPanel buttons = new HorizontalPanel();
        buttons.add(new Button(_msgs.fbTemplAddNew(), new ClickHandler() {
            @Override public void onClick (ClickEvent event) {
                addRow(new FacebookTemplate());
                _templates.get(_templates.size() - 1).setFocus();
            }
        }));
        Button save = new Button(_msgs.fbTemplSave());
        buttons.add(save);
        new ClickCallback<Void>(save) {
            @Override protected boolean callService () {
                return doSave(this);
            }

            @Override protected boolean gotResult (Void result) {
                _removed.clear();
                MsoyUI.info(_msgs.fbTemplSaved());
                return true;
            }
        };
        add(buttons);
    }

    protected void addRow (FacebookTemplate template)
    {
        final TemplateWidgets widgets = new TemplateWidgets(template);
        final int row = _display.getRowCount();
        _display.setWidget(row, 0, widgets._code);
        _display.setWidget(row, 1, widgets._bundleId);
        _display.setWidget(row, 2, MsoyUI.createCloseButton(new ClickHandler() {
            @Override public void onClick (ClickEvent event) {
                _display.removeRow(row);
                _templates.remove(widgets);
                if (widgets.isSaved()) {
                    _removed.add(widgets.getTemplate().code);
                }
            }
        }));
        _templates.add(widgets);
    }

    protected boolean doSave (AsyncCallback<Void> callback)
    {
        Set<String> codes = new HashSet<String>();
        List<TemplateWidgets> changed = new ArrayList<TemplateWidgets>();
        for (TemplateWidgets widgets : _templates) {
            String code = widgets.getCode().getText();
            if (code.length() == 0) {
                MsoyUI.error(_msgs.fbTemplErrCodeRequired());
                widgets._code.setFocus(true);
                return false;
            }
            if (!codes.add(code)) {
                MsoyUI.error(_msgs.fbTemplErrDuplicateCode(code));
                return false;
            }
            if (widgets.hasChanged()) {
                changed.add(widgets);
            }
        }

        if (changed.size() == 0 && _removed.size() == 0) {
            MsoyUI.info(_msgs.fbTemplNoChanges());
            return false;
        }

        List<FacebookTemplate> toSave = new ArrayList<FacebookTemplate>();
        for (TemplateWidgets widgets : changed) {
            widgets.saveChanges();
            toSave.add(widgets.getTemplate());
        }

        _adminsvc.updateFacebookTemplates(toSave, _removed, callback);
        return true;
    }

    protected static class TemplateWidgets
    {
        public TemplateWidgets (FacebookTemplate template)
        {
            _template = template;
            _code = MsoyUI.createTextBox(template.code, 15, 8);
            _code.setReadOnly(isSaved());
            _bundleId = MsoyUI.createTextBox(String.valueOf(template.bundleId), 20, 12);
        }

        public boolean isSaved ()
        {
            return _template.code.length() > 0;
        }

        public void setFocus ()
        {
            _code.setFocus(true);
        }

        public TextBox getCode ()
        {
            return _code;
        }

        public FacebookTemplate getTemplate ()
        {
            return _template;
        }

        public boolean hasChanged ()
        {
            return !_template.code.equals(_code.getText()) ||
                _template.bundleId != Long.parseLong(_bundleId.getText());
        }

        public void saveChanges ()
        {
            _template.code = _code.getText();
            _template.bundleId = Long.parseLong(_bundleId.getText());
            _code.setReadOnly(true);
        }

        protected FacebookTemplate _template;
        protected TextBox _code;
        protected TextBox _bundleId;
    }

    protected SmartTable _display;
    protected List<TemplateWidgets> _templates = new ArrayList<TemplateWidgets>();
    protected Set<String> _removed = new HashSet<String>();
}
