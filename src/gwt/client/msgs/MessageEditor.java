//
// $Id$

package client.msgs;

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
    }

    /**
     * Creates the default message editor.
     */
    public static Panel createDefault ()
    {
        return new RichTextEditor();
        //return new TinyMCEEditor();
    }
}
