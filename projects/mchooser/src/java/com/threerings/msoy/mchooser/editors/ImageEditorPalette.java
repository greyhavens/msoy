//
// $Id$

package com.threerings.msoy.mchooser.editors;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.samskivert.swing.VGroupLayout;

/**
 * Displays editing controls for the {@link ImageEditor}.
 */
public class ImageEditorPalette extends JPanel
{
    public ImageEditorPalette (ImageEditor editor)
    {
        super(new VGroupLayout(VGroupLayout.NONE, VGroupLayout.STRETCH, 5, VGroupLayout.TOP));
        _editor = editor;

        add(new JLabel("Edit Image"));
        addTool(new MoveTool());
        // activate the first tool (move)
        _tgroup.getElements().nextElement().setSelected(true);

        add(new JButton(_undo));
    }

    protected void addTool (final ImageEditorTool tool)
    {
        tool.init(_editor);
        JRadioButton tbutton = new JRadioButton(tool.getName());
        tbutton.getModel().addChangeListener(new ChangeListener() {
            public void stateChanged (ChangeEvent event) {
                if (((ButtonModel)event.getSource()).isSelected() && _selection != tool) {
                    selectTool(tool);
                }
            }
        });
        _tgroup.add(tbutton);
        add(tbutton);
    }

    protected void selectTool (ImageEditorTool tool)
    {
        if (_selection != null) {
            _selection.deactivate();
        }
        _selection = tool;
        if (_selection != null) {
            _selection.activate();
        }
    }

    protected Action _undo = new AbstractAction("Undo") {
        public void actionPerformed (ActionEvent event) {
            _editor.undo();
        }
    };

    protected ImageEditor _editor;
    protected ImageEditorTool _selection;
    protected ButtonGroup _tgroup = new ButtonGroup();
}
