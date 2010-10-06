//
// $Id: $

package client.adminz.config;

import com.threerings.msoy.admin.config.gwt.ConfigField;
import com.threerings.msoy.admin.config.gwt.ConfigField.FieldType;

import com.google.gwt.dom.client.Style;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import client.shell.CShell;
import client.ui.MsoyUI;

/**
 *
 */
public abstract class ConfigFieldEditor
{
    public static ConfigFieldEditor getEditorFor (ConfigField field, Command onChange)
    {
        if (field.type == FieldType.BOOLEAN) {
            return new CheckboxFieldEditor(field, onChange);
        }
        return new StringFieldEditor(field, onChange);
    }

    protected static class StringFieldEditor extends ConfigFieldEditor
    {
        public StringFieldEditor (ConfigField field, Command onChange)
        {
            super(field, onChange);
        }

        @Override
        protected Widget buildWidget (ConfigField field)
        {
            _box = new TextBox();
            _box.setStyleName("configStringEditor");
            _box.setVisibleLength(40);
            resetField();

            _box.addChangeHandler(new ChangeHandler() {
                @Override public void onChange (ChangeEvent changeEvent) {
                    // if the string fails conversion, just reset to the old value
                    if (_field.toValue(_box.getText().trim()) == null) {
                        _box.setText(_field.valStr);
                    }
                    updateModificationState();
                }
            });
            return _box;
        }

        @Override
        public ConfigField getModifiedField ()
        {
            Object newValue = _field.toValue(_box.getText().trim());
            if (newValue == null) {
                return null;
            }
            String newValStr = newValue.toString();
            if ((newValStr.isEmpty() && _field.valStr == null) ||newValStr.equals(_field.valStr)) {
                return null;
            }
            return new ConfigField(_field.name, _field.type, newValStr);
        }

        @Override
        protected void resetField ()
        {
            _box.setText(_field.valStr);
        }

        protected TextBox _box;
    }

    protected static class CheckboxFieldEditor extends ConfigFieldEditor
    {
        public CheckboxFieldEditor (ConfigField field, Command onChange)
        {
            super(field, onChange);
        }

        @Override
        protected Widget buildWidget (ConfigField field)
        {
            _box = new CheckBox();
            _box.setStyleName("configCheckBoxEditor");
            resetField();

            _box.addValueChangeHandler(new ValueChangeHandler() {
                @Override public void onValueChange (ValueChangeEvent changeEvent) {
                    updateModificationState();
                }
            });
            return _box;
        }

        @Override
        public ConfigField getModifiedField ()
        {
            String newValStr = Boolean.toString(_box.isChecked());
            if (newValStr.equals(_field.valStr)) {
                return null;
            }
            return new ConfigField(_field.name, _field.type, newValStr);
        }

        @Override
        protected void resetField ()
        {
            _box.setValue(new Boolean(_field.valStr));
        }

        protected CheckBox _box;
    }

    public ConfigFieldEditor (ConfigField field, Command onChange)
    {
        _field = field;
        _onChange = onChange;

        _value = buildWidget(field);
        _name = MsoyUI.createLabel(field.name, "fieldName");
        _reset = MsoyUI.createCloseButton(new ClickHandler() {
            public void onClick (ClickEvent event) {
                resetField();
                updateModificationState();
            }
        });
        _reset.setVisible(false);
    }

    protected void updateModificationState ()
    {
        Style style = _value.getElement().getStyle();
        if (getModifiedField() != null) {
            style.setBackgroundColor("red");
            _reset.setVisible(true);

        } else {
            style.clearBackgroundColor();
            _reset.setVisible(false);
        }
        _onChange.execute();
    }

    public Widget getNameWidget ()
    {
        return _name;
    }

    public Widget getValueWidget ()
    {
        return _value;
    }

    public Widget getResetWidget ()
    {
        return _reset;
    }

    public abstract ConfigField getModifiedField ();

    protected abstract Widget buildWidget (ConfigField field);
    protected abstract void resetField ();

    protected ConfigField _field;
    protected Command _onChange;

    protected Widget _name, _value, _reset;
}
