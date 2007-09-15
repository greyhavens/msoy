//
// $Id$

package com.threerings.msoy.swiftly.client.view;

import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import com.threerings.msoy.swiftly.client.Translator;
import com.threerings.msoy.swiftly.client.controller.SwiftlyDocumentEditor;

/** A dialog window to prompt the user for a file name and file type. */
public class CreateFileDialog extends JDialog
{
    public CreateFileDialog (SwiftlyDocumentEditor editor, Translator translator,
                             Container relative)
    {
        super(new JFrame(), translator.xlate("m.dialog.create_file.title"), true);
        setLayout(new GridLayout(3, 3, 10, 10));

        // file name input
        add(new JLabel(translator.xlate("m.dialog.create_file.name")));
        _text = new JTextField();
        _text.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _cancelled = false;
                setVisible(false);
            }
        });
        _text.setEditable(true);
        add(_text);

        // file type chooser
        add(new JLabel(translator.xlate("m.dialog.create_file.type")));
        _comboBox = new JComboBox(editor.getCreateableFileTypes().toArray());
        add(_comboBox);

        // ok/cancel buttons
        JButton button = new JButton(translator.xlate("m.dialog.create_file.create"));
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _cancelled = false;
                setVisible(false);
            }
        });
        add(button);
        button = new JButton(translator.xlate("m.dialog.create_file.cancel"));
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _cancelled = true;
                setVisible(false);
            }
        });
        add(button);

        // display the dialog
        pack();
        setLocationRelativeTo(relative);
        setVisible(true);
    }

    /**
     * Return the file name inputted by the user in the dialog.
     */
    public String getName ()
    {
        return _text.getText();
    }

    /**
     * Return the mime type selected by the user in the dialog.
     */
    public String getMimeType ()
    {
        return ((SwiftlyDocumentEditor.FileTypes)_comboBox.getSelectedItem()).mimeType;
    }

    /**
     * Returns true if the user clicked the cancel button.
     */
    public boolean wasCancelled ()
    {
        return _cancelled;
    }

    /**
     * Returns true if the user entered valid data.
     */
    public boolean isValid ()
    {
        return getName().length() > 0;
    }

    private final JTextField _text;
    private final JComboBox _comboBox;

    /** Whether the user clicked cancel. defaults to true to deal with closing the dialog. */
    private boolean _cancelled = true;
}
