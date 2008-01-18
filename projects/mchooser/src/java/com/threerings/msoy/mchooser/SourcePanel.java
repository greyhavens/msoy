//
// $Id$

package com.threerings.msoy.mchooser;

import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.google.common.collect.Maps;
import com.samskivert.swing.VGroupLayout;

import static com.threerings.msoy.mchooser.MediaChooser.log;

/**
 * Displays a list of media sources.
 */
public class SourcePanel extends JPanel
{
    public SourcePanel (MediaSource.ResultReceiver receiver)
    {
        super(new VGroupLayout(VGroupLayout.NONE, VGroupLayout.STRETCH, 5, VGroupLayout.TOP));
        _receiver = receiver;

        JLabel header = new JLabel("Source:");
        header.setHorizontalAlignment(JLabel.CENTER);
        add(header);

        JLabel tip = new JLabel("Choose a media source...");
        tip.setHorizontalAlignment(JLabel.CENTER);
        _selcomp = tip;
    }

    public void activate (MediaChooser chooser)
    {
        _chooser = chooser;
        _chooser.setSidebar(this);
        _chooser.setMain(_selcomp);
        if (_sgroup.getButtonCount() > 0) {
            _sgroup.getElements().nextElement().setSelected(true);
        }
    }

    public void addSource (final MediaSource source)
    {
        JRadioButton choice = new JRadioButton(source.getName());
        choice.getModel().addChangeListener(new ChangeListener() {
            public void stateChanged (ChangeEvent event) {
                if (((ButtonModel)event.getSource()).isSelected() && _selection != source) {
                    _selection = source;
                    _chooser.setMain(_selcomp = source.createChooser(_receiver));
                }
            }
        });
        _sgroup.add(choice);
        add(choice);
    }

    protected MediaChooser _chooser;
    protected MediaSource.ResultReceiver _receiver;
    protected MediaSource _selection;
    protected JComponent _selcomp;
    protected ButtonGroup _sgroup = new ButtonGroup();
}
