//
// $Id$

package com.threerings.msoy.mchooser.editors;

import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

import java.util.List;

import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.google.common.collect.Lists;

/**
 * Provides image editing facilities.
 */
public class ImageEditor extends JComponent
{
    public ImageEditor ()
    {
        _model.addChangeListener(new ChangeListener() {
            public void stateChanged (ChangeEvent event) {
                repaint();
            }
        });
    }

    public EditorModel getModel ()
    {
        return _model;
    }

    public void applyOp (EditorOp op)
    {
        op.apply(_model);
        _ops.add(op);
    }

    public void undo ()
    {
        if (_ops.size() > 0) {
            _ops.remove(_ops.get(_ops.size()-1));
            _model.reset(_ops);
        }
    }

    @Override // from JComponent
    protected void paintComponent (Graphics g)
    {
        super.paintComponent(g);

        Graphics2D gfx = (Graphics2D)g;
        AffineTransform xform = gfx.getTransform();
        try {
            _model.paint(gfx);
        } finally {
            gfx.setTransform(xform);
        }
    }

    protected EditorModel _model = new EditorModel();
    protected List<EditorOp> _ops = Lists.newArrayList();
}
