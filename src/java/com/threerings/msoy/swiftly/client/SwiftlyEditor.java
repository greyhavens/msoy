package com.threerings.msoy.swiftly.client;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.io.IOException;

import javax.swing.JTextPane;
import javax.swing.JTabbedPane;
import javax.swing.JScrollPane;

public class SwiftlyEditor extends JTabbedPane {

    public class SwiftlyScrollPane extends JScrollPane {
        protected JTextPane textPane;

        public SwiftlyScrollPane(SwiftlyTextPane pane) {
            super(pane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                  JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
            textPane = pane;
        }
        
        public JTextPane getTextPane() {
            return textPane; 
        }       

        public void setPage(String url) {
            try {
                textPane.setPage(url);
            } catch (IOException ie) {            
            }
        }       
    }

    public class SwiftlyTextPane extends JTextPane {
        // TODO this class is going to have all our syntax highlighting bits
    }

    public void addEditorTab(String tabName, String url) {
        SwiftlyTextPane textPane = new SwiftlyTextPane();
        SwiftlyScrollPane scroller = new SwiftlyScrollPane(textPane);

        addTab(tabName, scroller);
        int tabIndex = getTabCount() - 1;
        // ALT+tabIndex selects this tab. TODO don't less this go beyond 10.
        setMnemonicAt(tabIndex, KeyEvent.VK_1 + tabIndex);
        setTabPage(tabIndex, url);
    }

    public void setTabPage(int tabIndex, String url) {
        SwiftlyScrollPane pane = (SwiftlyScrollPane)getComponentAt(tabIndex);
        pane.setPage(url);
    }
}
