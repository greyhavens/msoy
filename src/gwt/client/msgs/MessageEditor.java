//
// $Id$

package client.msgs;

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
        final WrapperEditor wrapper = new WrapperEditor(new RichTextEditor());
        wrapper.getToggler().setText(_msgs.experiment());
        wrapper.getToggler().addClickHandler(new ClickHandler() {
            @Override public void onClick (ClickEvent event) {
                if (wrapper.getEditor() instanceof RichTextEditor) {
                    wrapper.setEditor(new TinyMceEditor());
                    wrapper.getToggler().setText(_msgs.antiExperiment());
                } else if (wrapper.getEditor() instanceof TinyMceEditor) {
                    wrapper.setEditor(new RichTextEditor());
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
            _editor = newEditor;
            setWidget(newEditor.asWidget());
        }

        @Override
        public Widget asWidget ()
        {
            return this;
        }

        @Override
        public String getHTML ()
        {
            return _editor.getHTML();
        }

        @Override
        public void selectAll ()
        {
            _editor.selectAll();
        }

        @Override
        public void setFocus (boolean focus)
        {
            _editor.setFocus(focus);
        }

        @Override
        public void setHTML (String html)
        {
            _editor.setHTML(html);
        }

        public Button getToggler ()
        {
            return _experiment;
        }

        protected Panel _editor;
        protected Button _experiment;
    }

    protected static final MsgsMessages _msgs = GWT.create(MsgsMessages.class);
}
