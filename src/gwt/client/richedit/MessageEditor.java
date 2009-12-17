//
// $Id$

package client.richedit;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * Interface and static factory methods for editing html messages.
 */
public class MessageEditor
{
    /**
     * Interface for the embedded editor widget.
     */
    public interface Panel
    {
        /**
         * Casts the editor to a widget so it can be placed in the DOM hierarchy.
         */
        Widget asWidget ();

        /**
         * Retrieves the HTML for the user's message.
         */
        String getHTML ();

        /**
         * Sets the HTML for the user's message.
         */
        void setHTML (String html);

        /**
         * Gets the panel color set by this editor, or null if none is set.
         */
        String getPanelColor ();

        /**
         * Sets the panel color of this editor.
         */
        void setPanelColor (String color);

        /**
         * Sets or clears focus for the editor.
         */
        void setFocus (boolean focus);

        /**
         * Selects all of the text in the editor.
         */
        void selectAll ();

        /**
         * Gets a button to toggle the major editor mode, whatever that may be.
         */
        Button getToggler ();
    }

    /**
     * Creates the default message editor.
     */
    public static Panel createDefault ()
    {
        return createDefault(false);
    }

    /**
     * Creates the default message editor.
     */
    public static Panel createDefault (final boolean enablePanelColor)
    {
        final WrapperEditor wrapper = new WrapperEditor(new RichTextEditor(enablePanelColor));
        wrapper.getToggler().setText(_msgs.experiment());
        wrapper.getToggler().addClickHandler(new ClickHandler() {
            @Override public void onClick (ClickEvent event) {
                if (wrapper.getEditor() instanceof RichTextEditor) {
                    wrapper.setEditor(new TinyMceEditor(enablePanelColor));
                    wrapper.getToggler().setText(_msgs.antiExperiment());
                } else if (wrapper.getEditor() instanceof TinyMceEditor) {
                    wrapper.setEditor(new RichTextEditor(enablePanelColor));
                    wrapper.getToggler().setText(_msgs.experiment());
                }
            }
        });
        return wrapper;
    }

    protected static class WrapperEditor extends SimplePanel
        implements Panel
    {
        public WrapperEditor (Panel wrapped)
        {
            if (wrapped == null) {
                throw new NullPointerException();
            }
            _experiment = new Button();
            _editor = wrapped;
            setWidget(_editor.asWidget());
        }

        public Panel getEditor ()
        {
            return _editor;
        }

        public void setEditor (Panel newEditor)
        {
            newEditor.setHTML(_editor.getHTML());
            newEditor.setPanelColor(_editor.getPanelColor());
            _editor = newEditor;
            setWidget(newEditor.asWidget());
        }

        @Override // from MessageEditor.Panel
        public Widget asWidget ()
        {
            return this;
        }

        @Override // from MessageEditor.Panel
        public String getHTML ()
        {
            return _editor.getHTML();
        }

        @Override // from MessageEditor.Panel
        public void selectAll ()
        {
            _editor.selectAll();
        }

        @Override // from MessageEditor.Panel
        public void setFocus (boolean focus)
        {
            _editor.setFocus(focus);
        }

        @Override // from MessageEditor.Panel
        public void setHTML (String html)
        {
            _editor.setHTML(html);
        }

        @Override // from MessageEditor.Panel
        public String getPanelColor ()
        {
            return _editor.getPanelColor();
        }

        @Override // from MessageEditor.Panel
        public void setPanelColor (String color)
        {
            _editor.setPanelColor(color);
        }

        @Override // from MessageEditor.Panel
        public Button getToggler ()
        {
            return _experiment;
        }

        protected Panel _editor;
        protected Button _experiment;
    }

    protected static final RichEditMessages _msgs = GWT.create(RichEditMessages.class);
}
