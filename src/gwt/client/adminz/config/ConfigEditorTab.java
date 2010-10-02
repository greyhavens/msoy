//
// $Id: $


package client.adminz.config;

import java.util.List;

import com.google.common.collect.Lists;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.admin.config.gwt.ConfigField;
import com.threerings.msoy.admin.config.gwt.ConfigService.ConfigurationResult;

import client.util.ClickCallback;
import com.google.gwt.user.client.ui.Button;

/**
 *
 */
public class ConfigEditorTab extends SmartTable
{
    public interface ConfigAccessor
    {
        void submitChanges (String tabKey, List<ConfigField> modified);
    }

    public ConfigEditorTab (ConfigAccessor parent, String key, List<ConfigField> fields)
    {
        super(0, 0);

        _parent = parent;
        _key = key;

        Button submit = new Button("Submit Changes");

        // wire up saving the code on click
        new ClickCallback<ConfigurationResult>(submit) {
            protected boolean callService () {
                List<ConfigField> modified = Lists.newArrayList();
                for (ConfigFieldEditor editor : _editors) {
                    ConfigField field = editor.getModifiedField();
                    if (field != null) {
                        modified.add(field);
                    }
                }
                _parent.submitChanges(_key, modified);
                return true;
            }
            protected boolean gotResult (ConfigurationResult result) {

                return false;
            }
        };

        cell(0, 1).alignRight().widget(submit);

        updateTable(fields);
    }

    protected void updateTable (List<ConfigField> fields)
    {
        SmartTable table = new SmartTable(5, 5);

        int row = 0;
        for (ConfigField field : fields) {
            ConfigFieldEditor editor = ConfigFieldEditor.getEditorFor(field);
            _editors.add(editor);
            cell(row, 0).alignRight().text(field.name);
            cell(row, 1).alignLeft().widget(editor);
            row ++;
        }

        cell(0, 0).colSpan(2).widget(table);
    }

    protected List<ConfigFieldEditor> _editors = Lists.newArrayList();

    protected ConfigAccessor _parent;
    protected String _key;
}
