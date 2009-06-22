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
        _display.setText(0, 1, _msgs.fbTemplVariantHdr());
        _display.setText(0, 2, _msgs.fbTemplBundleIdHdr());
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
                for (TemplateWidgets widgets : _templates) {
                    widgets.setTemplate(widgets.createTemplate());
                }
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
        _display.setWidget(row, 0, widgets.code);
        _display.setWidget(row, 1, widgets.variant);
        _display.setWidget(row, 2, widgets.bundleId);
        _display.setWidget(row, 3, MsoyUI.createCloseButton(new ClickHandler() {
            @Override public void onClick (ClickEvent event) {
                _display.removeRow(row);
                _templates.remove(widgets);
                if (widgets.isSaved()) {
                    _removed.add(widgets.template);
                }
            }
        }));
        _templates.add(widgets);
    }

    protected boolean doSave (AsyncCallback<Void> callback)
    {
        Set<FacebookTemplate> unique = new HashSet<FacebookTemplate>();
        Set<FacebookTemplate> changed = new HashSet<FacebookTemplate>();
        for (TemplateWidgets widgets : _templates) {
            FacebookTemplate template = widgets.createTemplate();
            if (template.code.length() == 0) {
                MsoyUI.error(_msgs.fbTemplErrCodeRequired());
                widgets.code.setFocus(true);
                return false;
            }
            if (widgets.hasChanged()) {
                changed.add(template);
            }
            if (!unique.add(template)) {
                MsoyUI.error(_msgs.fbTemplErrDuplicate(template.code, template.variant));
                return false;
            }
        }

        if (changed.size() == 0 && _removed.size() == 0) {
            MsoyUI.info(_msgs.fbTemplNoChanges());
            return false;
        }

        _adminsvc.updateFacebookTemplates(changed, _removed, callback);
        return true;
    }

    protected static class TemplateWidgets
    {
        protected FacebookTemplate template;
        protected TextBox code = MsoyUI.createTextBox("", 15, 8);;
        protected TextBox variant = MsoyUI.createTextBox("", 6, 4);
        protected TextBox bundleId = MsoyUI.createTextBox("", 20, 12);

        public TemplateWidgets (FacebookTemplate template)
        {
            setTemplate(template);
        }

        public void setTemplate (FacebookTemplate template)
        {
            this.template = template;
            code.setText(template.code);
            variant.setText(template.variant);
            bundleId.setText(String.valueOf(template.bundleId));
            code.setReadOnly(isSaved());
            variant.setReadOnly(isSaved());
        }

        public boolean isSaved ()
        {
            return template.code.length() > 0;
        }

        public void setFocus ()
        {
            code.setFocus(true);
        }

        public boolean hasChanged ()
        {
            return !createTemplate().equals(template);
        }

        public FacebookTemplate createTemplate ()
        {
            FacebookTemplate template = new FacebookTemplate();
            template.code = code.getText();
            template.variant = variant.getText();
            template.bundleId = Long.parseLong(bundleId.getText());
            return template;
        }
    }

    protected SmartTable _display;
    protected List<TemplateWidgets> _templates = new ArrayList<TemplateWidgets>();
    protected Set<FacebookTemplate> _removed = new HashSet<FacebookTemplate>();
}
