//
// $Id: $


package client.adminz.config;

import com.threerings.msoy.admin.config.gwt.ConfigField;
import com.threerings.msoy.admin.config.gwt.ConfigField.FieldType;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 *
 */
public abstract class ConfigFieldEditor extends SimplePanel
{
    public static ConfigFieldEditor getEditorFor (ConfigField field)
    {
        return new StringFieldEditor(field);
    }

    protected static class StringFieldEditor extends ConfigFieldEditor
        implements ChangeHandler
    {
        public StringFieldEditor (ConfigField field)
        {
            super(field);
        }

        @Override protected Widget buildWidget (ConfigField field)
        {
            _box = new TextBox();
            _box.setText(field.valStr);
            _box.addChangeHandler(this);
            return _box;
        }

        @Override
        public ConfigField getModifiedField ()
        {
            Object newValue = textToValue(_box.getText().trim(), _field.type);
            String newValStr = (newValue != null) ? newValue.toString() : null;
            if (newValStr == null || newValStr.equals(_field.valStr)) {
                return null;
            }
            return new ConfigField(_field.name, _field.type, newValStr);
        }

        public void onChange (ChangeEvent changeEvent)
        {
            if (getModifiedField() != null) {
                addStyleName("borderedWidget");
            } else {
                removeStyleName("borderedWidget");
            }
        }
    }

    public ConfigFieldEditor (ConfigField field)
    {
        _field = field;

        setWidget(buildWidget(field));
    }

    public abstract ConfigField getModifiedField ();

    protected abstract Widget buildWidget (ConfigField field);

    protected static Object textToValue (String text, FieldType type)
    {
        switch(type) {
        case INTEGER:
            return new Integer(text);
        case SHORT:
            return new Short(text);
        case BYTE:
            return new Byte(text);
        case LONG:
            return new Long(text);
        case FLOAT:
            return new Float(text);
        case DOUBLE:
            return new Double(text);
        case BOOLEAN:
            return new Boolean(text);
        case STRING:
            return text;
        }
        return null;
    }

    protected ConfigField _field;
    protected TextBox _box;
}
