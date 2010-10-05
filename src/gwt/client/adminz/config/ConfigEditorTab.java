//
// $Id: $


package client.adminz.config;

import java.util.List;

import com.google.common.collect.Lists;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.admin.config.gwt.ConfigField;
import com.threerings.msoy.admin.config.gwt.ConfigService.ConfigurationResult;

import client.util.ClickCallback;

/**
 *
 */
public class ConfigEditorTab extends SmartTable
{
    public interface ConfigAccessor
    {
        void submitChanges (List<ConfigField> modified,
                            AsyncCallback<ConfigurationResult> callback);

    }

    public ConfigEditorTab (ConfigAccessor parent, String key, List<ConfigField> fields)
    {
        super("configEditorTab", 5, 5);

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
                _parent.submitChanges(modified, this);
                return true;
            }
            protected boolean gotResult (ConfigurationResult result) {
                updateTable(result.records.get(_key));
                return false;
            }
        };

        cell(1, 1).alignRight().widget(submit);

        updateTable(fields);
    }

    protected void updateTable (List<ConfigField> fields)
    {
        SmartTable table = new SmartTable(5, 5);
        table.setStyleName("configEditorTable");

        int row = 0;
        for (ConfigField field : fields) {
            ConfigFieldEditor editor = ConfigFieldEditor.getEditorFor(field);
            _editors.add(editor);
            table.cell(row, 0).alignRight().widget(editor.getNameWidget());
            table.cell(row, 1).alignLeft().widget(editor.getValueWidget());
            table.cell(row, 2).alignLeft().widget(editor.getResetWidget());
            row ++;
        }

        cell(0, 0).colSpan(2).widget(table);
    }

    protected List<ConfigFieldEditor> _editors = Lists.newArrayList();

    protected ConfigAccessor _parent;
    protected String _key;
}
