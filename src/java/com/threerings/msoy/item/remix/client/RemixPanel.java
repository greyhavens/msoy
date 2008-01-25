//
// $Id$

package com.threerings.msoy.item.remix.client;

import java.awt.BorderLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.VGroupLayout;

import com.samskivert.util.ObjectUtil;
import com.samskivert.util.ResultListener;

import com.whirled.DataPack;

import com.whirled.remix.data.EditableDataPack;

public class RemixPanel extends JPanel
{
    public RemixPanel (String url)
    {
        super(new BorderLayout());

        _cancel = new AbstractAction("Cancel") {
            public void actionPerformed (ActionEvent e) {
                // TODO
            }
        };
        _remix = new AbstractAction("Remix") {
            public void actionPerformed (ActionEvent e) {
                // TODO
            }
        };
        _remix.setEnabled(false);

        JPanel butPan = GroupLayout.makeButtonBox(GroupLayout.LEFT);
        butPan.add(new JButton(_cancel));
        butPan.add(new JButton(_remix));
        add(butPan, BorderLayout.SOUTH);

        startPackLoading(url);
    }

    protected void startPackLoading (String url)
    {
        ResultListener<EditableDataPack> rl = new ResultListener<EditableDataPack>() {
            public void requestCompleted (EditableDataPack pack) {
                packAvailable();
            }

            public void requestFailed (Exception cause) {
                // TODO
            }
        };
        _pack = new EditableDataPack(url, rl);
    }

    /**
     * Called once our pack is ready to go.
     */
    protected void packAvailable ()
    {
        JPanel panel = GroupLayout.makeVBox();

        addFields(panel, _pack.getDataFields(), true);
        addFields(panel, _pack.getFileFields(), false);
    }

    protected void addFields (JPanel panel, List<String> fields, final boolean areData)
    {
        if (fields.isEmpty()) {
            return;
        }

        panel.add(new JLabel(areData ? "Data fields" : "Files"));

        for (String name : fields) {
            JPanel hbox = GroupLayout.makeHBox();
            hbox.add(new JLabel(name));
            JButton but = new JButton("Change");
            final String fname = name;
            but.addActionListener(new ActionListener() {
                public void actionPerformed (ActionEvent e) {
                    changeEntry(fname, areData);
                }
            });
        }
    }

    protected void changeEntry (String name, boolean isData)
    {
        if (isData) {
            changeEntry(_pack.getDataEntry(name));
        } else {
            changeEntry(_pack.getFileEntry(name));
        }
    }

    protected void changeEntry (DataPack.FileEntry entry)
    {
        // TODO
    }

    protected void changeEntry (final DataPack.DataEntry entry)
    {
        JPanel pan = setupEdit(entry);

        final DataPack.DataType type = (DataPack.DataType) entry.getType();
        final JTextField field = new JTextField(type.formatValue(entry.value));
        pan.add(field);

        JButton button = new JButton("OK");
        button.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent e) {
                Object newValue = type.parseValue(field.getText().trim());
                if (!ObjectUtil.equals(newValue, entry.value)) {
                    entry.value = newValue;
                    updatePreview();
                }
            }
        });
        pan.add(button);

        JDialog dialog = new JDialog();
        dialog.add(pan, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    protected JPanel setupEdit (DataPack.AbstractEntry entry)
    {
        JDialog dialog = new JDialog();

        JPanel pan = GroupLayout.makeVBox();
        pan.add(new JLabel("Name: " + entry.name));
        pan.add(new JLabel("Description: " + entry.info));
        DataPack.AbstractType type = entry.getType();
        pan.add(new JLabel("Type: " + type.toString() + " (" + type.getDescription() + ")"));

        return pan;
    }

    protected void updatePreview ()
    {
        // TODO
    }

    /** The datapack we're editing. */
    protected EditableDataPack _pack;

    protected Action _cancel;

    protected Action _remix;
}
