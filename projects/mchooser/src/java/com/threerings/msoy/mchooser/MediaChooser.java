//
// $Id$

package com.threerings.msoy.mchooser;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.Stack;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.HGroupLayout;
import com.samskivert.swing.VGroupLayout;
import com.samskivert.swing.util.SwingUtil;
import com.samskivert.util.StringUtil;

import com.threerings.msoy.mchooser.modes.ChooseImageMode;
import com.threerings.msoy.mchooser.sources.LocalFileSource;

/**
 * The main media chooser "application".
 */
public class MediaChooser
{
    /** Used to manage the flow of the choosing process. */
    public static interface Mode
    {
        /** Activates this mode, displaying its UI. */
        public void activate (MediaChooser chooser);
    }

    /** The static log instance configured for use by this package. */
    public static Logger log = Logger.getLogger("com.threerings.msoy.mchooser");

    /** Used to store chooser related preferences. */
    public static Preferences prefs = Preferences.userNodeForPackage(MediaChooser.class);

    public static void main (String[] args)
    {
        MediaChooser chooser = new MediaChooser();
        chooser.getFrame().setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        chooser.pushMode(new ChooseImageMode());
    }

    public MediaChooser ()
    {
        _frame = new JFrame();
        _frame.setTitle("Media Chooser");
        _frame.setSize(FRAME_WIDTH, FRAME_HEIGHT); // TODO: remember these?

        // set up our content pane
        JPanel content = new JPanel(new BorderLayout(5, 5));
        content.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        _frame.setContentPane(content);

        // create our footer and control buttons
        _footer = new JPanel(new HGroupLayout(GroupLayout.STRETCH));
        _footer.add(_cancel = new JButton("Cancel"), GroupLayout.FIXED);
        _cancel.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                setSidebar(null);
                setMain(null);
                _modeStack.clear();
                _frame.setVisible(false);
            }
        });
        _footer.add(_back = new JButton("Back"), GroupLayout.FIXED);
        _back.addActionListener(new ActionListener() {
            public void actionPerformed (ActionEvent event) {
                popMode();
            }
        });
    }

    public JFrame getFrame ()
    {
        return _frame;
    }

    public void pushMode (Mode mode)
    {
        mode.activate(this);
        SwingUtil.centerWindow(_frame);
        _frame.setVisible(true);
        _modeStack.push(mode);
        _back.setEnabled(_modeStack.size() > 1);
    }

    public void popMode ()
    {
        _modeStack.pop();
        _modeStack.peek().activate(this);
        _back.setEnabled(_modeStack.size() > 1);
    }

    public void setSidebar (JComponent sidebar)
    {
        if (_sidebar != null) {
            _frame.getContentPane().remove(_sidebar);
            _sidebar = null;
        }
        if (sidebar != null) {
            _sidebar = new JPanel(new VGroupLayout(GroupLayout.STRETCH));
            _sidebar.add(sidebar);
            _sidebar.add(_footer, GroupLayout.FIXED);
            _frame.getContentPane().add(_sidebar, BorderLayout.WEST);
        }
    }

    public void setMain (JComponent main)
    {
        if (_main != null) {
            _frame.getContentPane().remove(_main);
        }
        if ((_main = main) != null) {
            _frame.getContentPane().add(_main, BorderLayout.CENTER);
        }
        SwingUtil.refresh((JPanel)_frame.getContentPane());
    }

    protected Stack<Mode> _modeStack = new Stack<Mode>();

    protected JFrame _frame;
    protected JPanel _sidebar;
    protected JComponent _main;
    protected JPanel _footer;
    protected JButton _cancel, _back, _next;

    protected static final int FRAME_WIDTH = 640;
    protected static final int FRAME_HEIGHT = 480;
}
