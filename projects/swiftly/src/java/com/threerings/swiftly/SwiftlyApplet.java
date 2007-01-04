package com.threerings.swiftly;

import javax.swing.JApplet;
import javax.swing.JTextPane;
import javax.swing.JScrollPane;

public class SwiftlyApplet extends JApplet {
    public void init() {
        JTextPane textPane = new JTextPane();
        JScrollPane scroller = new JScrollPane(textPane);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        add(scroller);
  }
} 
