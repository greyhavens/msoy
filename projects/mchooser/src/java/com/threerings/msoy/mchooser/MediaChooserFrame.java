//
// $Id$

package com.threerings.msoy.mchooser;

import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

/**
 * Does something extraordinary.
 */
public class MediaChooserFrame extends JFrame
{
    public MediaChooserFrame ()
    {
        setTitle("Media Chooser");
        setSize(400, 300);

        JButton open = new JButton("Open chooser...");
        open.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                JFileChooser chooser = new JFileChooser();
                System.err.println("RV " + chooser.showOpenDialog(MediaChooserFrame.this));
            }
        });
        getContentPane().add(open, BorderLayout.CENTER);
    }
}
