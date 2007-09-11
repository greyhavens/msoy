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

import com.threerings.util.MessageBundle;

/** A dialog window to prompt the user for a file name and file type. */
public class CreateFileDialog extends JDialog
{
    public CreateFileDialog (SwiftlyDocumentEditor editor, Container relative, MessageBundle msgs)
    {
        super(new JFrame(), msgs.get("m.dialog.create_file.title"), true);
        setLayout(new GridLayout(3, 3, 10, 10));

        // file name input
        add(new JLabel(msgs.get("m.dialog.create_file.name")));
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
        add(new JLabel(msgs.get("m.dialog.create_file.type")));
        _comboBox = new JComboBox(editor.getCreateableFileTypes().toArray());
        add(_comboBox);

        // ok/cancel buttons
        JButton button = new JButton(msgs.get("m.dialog.create_file.create"));
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                _cancelled = false;
                setVisible(false);
            }
        });
        add(button);
        button = new JButton(msgs.get("m.dialog.create_file.cancel"));
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

    public String getName ()
    {
        return _text.getText();
    }

    public String getMimeType ()
    {
        return ((SwiftlyDocumentEditor.FileTypes)_comboBox.getSelectedItem()).mimeType;
    }

    public boolean wasCancelled ()
    {
        return _cancelled;
    }

    protected JTextField _text;
    protected JComboBox _comboBox;

    /** Whether the user clicked cancel. defaults to true to deal with closing the dialog. */
    protected boolean _cancelled = true;
}
