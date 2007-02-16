/*
 * JSwiff is an open source Java API for Macromedia Flash file generation
 * and manipulation
 *
 * Copyright (C) 2004-2005 Ralf Terdic (contact@jswiff.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.jswiff.investigator;

import com.jswiff.SWFDocument;
import com.jswiff.SWFReader;
import com.jswiff.SWFWriter;
import com.jswiff.listeners.SWFDocumentReader;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.net.URL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;


/**
 * Implements a simple Flash file structure viewer. Either pass an SWF file as
 * argument, or choose one in the open dialog.
 */
public final class Investigator extends JFrame {
  static int threadCounter            = 0;
  File file;
  JPanel treePanel;
  JTree tree;
  private JToolBar statusBar;
  JTextField searchTextField;
  private JCheckBox caseSenseCheckBox;
  private JCheckBox backwardsCheckBox;
  JButton expandButton;
  private JButton findButton;
  JButton refreshButton;
  JButton openButton;
  JButton infoButton;
  private JButton copyButton;
  DefaultMutableTreeNode rootNode;
  int nodeNumber;
  private Clipboard clipboard         = getToolkit().getSystemClipboard();

  /**
   * Main method.
   *
   * @param args arguments (an optional SWF file path)
   */
  public static void main(final String[] args) {
    SwingUtilities.invokeLater(
      new Runnable() {
        public void run() {
          Investigator investigator = new Investigator();
          URL logoUrl               = getClass().getResource(
              "/com/jswiff/investigator/resources/logo16x16.png");
          investigator.setIconImage(new ImageIcon(logoUrl).getImage());
          investigator.run(args);
        }
      });
    threadCounter++;
  }

  void copy() {
    TreePath[] selectionPaths = tree.getSelectionPaths();
    if (selectionPaths == null) {
      return;
    }
    StringBuffer nodeStringBuffer = new StringBuffer();

    // get minimum depth
    int minDepth                  = Integer.MAX_VALUE;
    for (int i = 0; i < selectionPaths.length; i++) {
      TreePath selectionPath = selectionPaths[i];
      minDepth = Math.min(minDepth, selectionPath.getPath().length);
    }
    for (int i = 0; i < selectionPaths.length; i++) {
      TreePath selectionPath = selectionPaths[i];
      if (selectionPath != null) {
        TreeNode selectedNode = (TreeNode) selectionPath.getLastPathComponent();
        if (i > 0) {
          nodeStringBuffer.append("\n");
        }
        int depth = selectionPath.getPath().length - minDepth;

        // add 'depth' spaces
        for (int j = 0; j < depth; j++) {
          nodeStringBuffer.append(' ');
        }
        nodeStringBuffer.append(filterHTML(selectedNode.toString()));
      }
    }
    clipboard.setContents(
      new NodeStringTransferable(nodeStringBuffer.toString()), null);
  }

  void displayInfo() {
    infoButton.setEnabled(false);
    SplashWindow splashWindow = new SplashWindow(this);
    splashWindow.enableCloseOnClick();
  }

  void displayNodeNumber() {
    displayStatus(nodeNumber + " nodes.");
  }

  void displayStatus(String status) {
    statusBar.removeAll();
    statusBar.add(new JLabel(status));
    statusBar.revalidate();
    statusBar.repaint();
  }

  void displayTree() {
    treePanel.removeAll();
    rootNode   = new DefaultMutableTreeNode(file.getAbsolutePath());
    tree       = new JTree(rootNode);
    // if Arial Unicode MS is installed, let's use it
    tree.setFont(new Font("Arial Unicode MS", Font.PLAIN, 11));
    setAccelerators(tree);
    read();
    tree.setScrollsOnExpand(true);
    tree.setRootVisible(true);
    tree.setShowsRootHandles(true);
    treePanel.add(new JScrollPane(tree));
    treePanel.revalidate();
    treePanel.repaint();
    displayStatus(nodeNumber + " nodes read.");
  }

  /*
   * This method provides testing functionality hidden from the GUI - invoke it
   * by pressing ctrl+d. Chosen files are parsed to SWFDocument instances
   * which are finally written to /copies subdirectory. The author encourages
   * you to use this on complex files and then compare the behavior of copies
   * and original files, which, of course, should be identical. Also watch
   * the console output.
   */
  void duplicateFiles() {
    JFileChooser chooser = new JFileChooser(file);
    chooser.setFileFilter(new SWFFileFilter());
    chooser.setMultiSelectionEnabled(true);
    chooser.setDialogTitle("Choose files to duplicate");
    File[] sourceFiles = null;
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      sourceFiles = chooser.getSelectedFiles();
    }
    if ((sourceFiles != null) && (sourceFiles.length > 0)) {
      try {
        for (int i = 0; i < sourceFiles.length; i++) {
          File sourceFile = sourceFiles[i];
          System.out.println("Duplicating " + sourceFile);
          String destPathString = sourceFile.getParentFile().getAbsolutePath() +
            File.separatorChar + "copies";
          new File(destPathString).mkdir();
          File destFile               = new File(
              destPathString + File.separatorChar + sourceFile.getName());
          SWFReader reader            = new SWFReader(
              new FileInputStream(sourceFile));
          SWFDocumentReader docReader = new SWFDocumentReader();
          reader.addListener(docReader);
          reader.read();
          SWFDocument doc  = docReader.getDocument();
          SWFWriter writer = new SWFWriter(doc, new FileOutputStream(destFile));
          writer.write();
        }
        JOptionPane.showMessageDialog(
          this,
          sourceFiles.length + " files processed - check console for details.",
          "Completed", JOptionPane.INFORMATION_MESSAGE);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  void expand() {
    tree.setScrollsOnExpand(false);
    Enumeration en                = rootNode.postorderEnumeration();
    ProgressDialog progressDialog = new ProgressDialog(
        this, "Expanding tree...", "Expanded nodes:", "0", 0, nodeNumber, true);
    int nodeCounter               = 0;
    progressDialog.setProgressValue(nodeCounter);
    while (en.hasMoreElements()) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) en.nextElement();
      if (node.isLeaf()) {
        tree.makeVisible(getPath(node));
      }
      nodeCounter++;
      if ((nodeCounter & 15) == 0) {
        if (progressDialog.isCanceled()) {
          break;
        }
        progressDialog.setProgressValue(nodeCounter);
        progressDialog.setNote(Integer.toString(nodeCounter));
      }
    }
    progressDialog.close();
    expandButton.setEnabled(true);
    openButton.setEnabled(true);
    refreshButton.setEnabled(true);
    tree.setScrollsOnExpand(true);
  }

  void expandRoot() {
    tree.expandPath(new TreePath(rootNode));
  }

  void expandTree() {
    Thread expandThread = new Thread() {
        public void run() {
          expand();
        }
      };
    expandButton.setEnabled(false);
    openButton.setEnabled(false);
    refreshButton.setEnabled(false);
    expandThread.start();
  }

  void find(String searchText) {
    if (searchText.length() == 0) {
      return;
    }
    boolean backwards      = this.backwardsCheckBox.isSelected();
    boolean caseSense      = this.caseSenseCheckBox.isSelected();
    TreeNode selectedNode;
    TreePath selectionPath = tree.getSelectionPath();
    if (selectionPath == null) {
      // startPath = new TreePath(tree.getModel().getRoot());
      selectedNode = rootNode;
    } else {
      selectedNode = (TreeNode) selectionPath.getLastPathComponent();
    }
    findButton.setEnabled(false);
    TreeNode foundNode = findNode(
        selectedNode, searchText, caseSense, backwards);
    if (foundNode == null) {
      displayStatus("\"" + searchText + "\" not found!");
    } else {
      TreePath foundPath = getPath(foundNode);
      tree.setSelectionPath(foundPath);
      tree.scrollPathToVisible(foundPath);
    }
    findButton.setEnabled(true);
  }

  void openFile() {
    if (chooseFile()) {
      displayTree();
    }
  }

  void run(String[] args) {
    setLAF();
    setExtendedState(MAXIMIZED_BOTH);
    setVisible(true);
    if (threadCounter == 1) {
      (new SplashWindow(this, 5000)).enableCloseOnClick();
    }
    if (args.length != 0) {
      file = new File(args[0]);
      if (!file.exists() || !file.canRead()) {
        JOptionPane.showMessageDialog(
          this,
          "Cannot read file " + args[0] + ", please choose another SWF file!",
          "Error", JOptionPane.ERROR_MESSAGE);
        file = null;
        setTitle("Choose SWF file...");
        chooseFile();
      }
    } else {
      setTitle("Choose SWF file...");
      chooseFile();
    }
    if (file == null) {
      System.exit(0);
    }
    setTitle();
    display();
    displayTree();
    setVisible(false);
    pack();
    setExtendedState(MAXIMIZED_BOTH);
    setVisible(true);
  }

  private void setAccelerators(JComponent component) {
    // F5 = Refresh
    component.getActionMap().put(
      "Refresh",
      new AbstractAction("Refresh") {
        public void actionPerformed(ActionEvent evt) {
          displayTree();
        }
      });
    component.getInputMap().put(
      KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "Refresh");
    // Ctrl+C = Copy
    component.getActionMap().put(
      "Copy",
      new AbstractAction("Copy") {
        public void actionPerformed(ActionEvent evt) {
          copy();
        }
      });
    component.getInputMap().put(KeyStroke.getKeyStroke("control C"), "Copy");
    // Ctrl+E = Expand
    component.getActionMap().put(
      "Expand",
      new AbstractAction("Expand") {
        public void actionPerformed(ActionEvent evt) {
          expand();
        }
      });
    component.getInputMap().put(KeyStroke.getKeyStroke("control E"), "Expand");
    // F3 = Expand
    component.getActionMap().put(
      "Find",
      new AbstractAction("Find") {
        public void actionPerformed(ActionEvent evt) {
          find(searchTextField.getText());
        }
      });
    component.getInputMap().put(
      KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "Find");
    // Ctrl+O = Open
    component.getActionMap().put(
      "Open",
      new AbstractAction("Open") {
        public void actionPerformed(ActionEvent evt) {
          openFile();
        }
      });
    component.getInputMap().put(KeyStroke.getKeyStroke("control O"), "Open");
    // Ctrl+N = New
    component.getActionMap().put(
      "New",
      new AbstractAction("New") {
        public void actionPerformed(ActionEvent evt) {
          String[] args = new String[1];
          args[0] = file.getAbsolutePath();
          main(args);
        }
      });
    component.getInputMap().put(KeyStroke.getKeyStroke("control N"), "New");
    // Ctrl+D = Duplicate
    component.getActionMap().put(
      "Duplicate",
      new AbstractAction("Duplicate") {
        public void actionPerformed(ActionEvent evt) {
          duplicateFiles();
        }
      });
    component.getInputMap().put(
      KeyStroke.getKeyStroke("control D"), "Duplicate");
  }

  private void setLAF() {
    try {
      UIManager.setLookAndFeel(
        "com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
    } catch (Exception e) {
      // continue with Java l&f
    }
  }

  private TreePath getPath(TreeNode node) {
    java.util.List list = new ArrayList();

    // Add all nodes to list
    while (node != null) {
      list.add(node);
      node = node.getParent();
    }
    Collections.reverse(list);
    // Convert array of nodes to TreePath
    return new TreePath(list.toArray());
  }

  private void setTitle() {
    setTitle("JSwiff Investigator - " + file);
  }

  private boolean chooseFile() {
    JFileChooser chooser;
    if (file != null) {
      chooser = new JFileChooser(file);
    } else {
      chooser = new JFileChooser();
    }
    chooser.setFileFilter(new SWFFileFilter());
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      file = chooser.getSelectedFile();
    } else {
      return false;
    }
    setTitle();
    return true;
  }

  private void display() {
    // setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    addWindowListener(
      new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          if (threadCounter > 1) {
            threadCounter--;
            // Thread.currentThread().interrupt();
            setVisible(false);
            dispose();
          } else {
            System.exit(0);
          }
        }
      });

    JToolBar toolBar = new JToolBar();
    setAccelerators(toolBar);
    displayToolBar(toolBar);
    initTreePanel();
    getContentPane().add(toolBar, "North");
    getContentPane().add(treePanel);
    statusBar = new JToolBar();
    setAccelerators(statusBar);
    statusBar.setFloatable(false);
    getContentPane().add(statusBar, "South");
  }

  private void displayToolBar(JToolBar toolBar) {
    toolBar.setFloatable(false);
    openButton = new JButton("Open");
    openButton.setToolTipText("Open new file (Ctrl+O)");
    openButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          openFile();
        }
      });
    openButton.addMouseListener(
      new MouseAdapter() {
        public void mouseEntered(MouseEvent e) {
          displayStatus("Opens a new file.");
        }

        public void mouseExited(MouseEvent e) {
          displayNodeNumber();
        }
      });
    refreshButton = new JButton("Refresh");
    refreshButton.setToolTipText("Refresh tree (F5)");
    refreshButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          displayTree();
        }
      });
    refreshButton.addMouseListener(
      new MouseAdapter() {
        public void mouseEntered(MouseEvent e) {
          displayStatus("Reads the file again and refreshes the tree view.");
        }

        public void mouseExited(MouseEvent e) {
          displayNodeNumber();
        }
      });
    expandButton = new JButton("Expand");
    expandButton.setToolTipText("Expand all nodes (Ctrl+E)");
    expandButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          expandTree();
        }
      });
    expandButton.addMouseListener(
      new MouseAdapter() {
        public void mouseEntered(MouseEvent e) {
          displayStatus(
            "Expands all nodes of the tree. This may take long for big files.");
        }

        public void mouseExited(MouseEvent e) {
          displayNodeNumber();
        }
      });
    findButton = new JButton("Find: ");
    findButton.setToolTipText("Find text (F3)");
    findButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          find(searchTextField.getText());
        }
      });
    findButton.addMouseListener(
      new MouseAdapter() {
        public void mouseEntered(MouseEvent e) {
          displayStatus(
            "Searches for text in the tree, starting at the selected node.");
        }

        public void mouseExited(MouseEvent e) {
          displayNodeNumber();
        }
      });
    copyButton = new JButton("Copy");
    copyButton.setToolTipText("Copies selected nodes (Ctrl+C)");
    copyButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          copy();
        }
      });
    copyButton.addMouseListener(
      new MouseAdapter() {
        public void mouseEntered(MouseEvent e) {
          displayStatus(
            "Copies selected nodes to clipboard. Use Ctrl/Shift for" +
            " multiple selections. Selection order and indentation" +
            " levels are preserved.");
        }

        public void mouseExited(MouseEvent e) {
          displayNodeNumber();
        }
      });
    caseSenseCheckBox = new JCheckBox("Case sensitive");
    // caseSenseCheckBox.setSelected(false);
    caseSenseCheckBox.setToolTipText("Perform case sensitive searches");
    setAccelerators(caseSenseCheckBox);
    backwardsCheckBox = new JCheckBox("Backwards");
    // backwardsCheckBox.setSelected(false);
    backwardsCheckBox.setToolTipText("Search backwards");
    setAccelerators(backwardsCheckBox);
    searchTextField = new JTextField();
    searchTextField.setToolTipText("Enter search text");
    searchTextField.addMouseListener(
      new MouseAdapter() {
        public void mouseEntered(MouseEvent e) {
          displayStatus("Enter here the text you want to search for.");
        }

        public void mouseExited(MouseEvent e) {
          displayNodeNumber();
        }
      });
    searchTextField.addKeyListener(
      new KeyListener() {
        public void keyPressed(KeyEvent e) {
          if (e.getKeyChar() == '\n') {
            find(searchTextField.getText());
          }
        }

        public void keyReleased(KeyEvent e) {
          // do nothing
        }

        public void keyTyped(KeyEvent e) {
          // do nothing
        }
      });
    infoButton = new JButton("Info");
    infoButton.setToolTipText("Displays program info");
    infoButton.addActionListener(
      new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          displayInfo();
        }
      });
    infoButton.addMouseListener(
      new MouseAdapter() {
        public void mouseEntered(MouseEvent e) {
          displayStatus("Displays information about the program.");
        }

        public void mouseExited(MouseEvent e) {
          displayNodeNumber();
        }
      });
    toolBar.add(openButton);
    toolBar.addSeparator();
    toolBar.add(refreshButton);
    toolBar.addSeparator();
    toolBar.add(expandButton);
    toolBar.addSeparator();
    toolBar.add(copyButton);
    toolBar.addSeparator();
    toolBar.add(findButton);
    toolBar.add(searchTextField);
    toolBar.add(caseSenseCheckBox);
    toolBar.add(backwardsCheckBox);
    toolBar.addSeparator();
    toolBar.add(infoButton);
  }

  private String filterHTML(String string) {
    StringBuffer result = new StringBuffer();
    boolean tag         = false;
    for (int i = 0; i < string.length(); i++) {
      char c = string.charAt(i);
      if (c == '<') {
        tag = true;
      } else if (c == '>') {
        tag = false;
      } else if (tag == false) {
        result.append(c);
      }
    }
    return result.toString();
  }

  private TreeNode findNode(
    TreeNode selectedNode, String searchText, boolean caseSense,
    boolean backwards) {
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedNode;
    if (!caseSense) {
      searchText = searchText.toUpperCase();
    }
    while (true) {
      node = backwards ? node.getPreviousNode() : node.getNextNode();
      if (node == null) {
        break;
      }
      if (
        (caseSense && (node.toString().lastIndexOf(searchText) != -1)) ||
            (!caseSense &&
            (node.toString().toUpperCase().lastIndexOf(searchText) != -1))) {
        return node;
      }
    }
    return null;
  }

  private void initTreePanel() {
    treePanel = new JPanel();
    treePanel.setLayout(new GridLayout(1, 1));
    setAccelerators(treePanel);
  }

  private void read() {
    Thread readThread = new Thread() {
        public void run() {
          expandButton.setEnabled(false);
          openButton.setEnabled(false);
          refreshButton.setEnabled(false);
          SWFReader reader;
          try {
            reader = new SWFReader(new FileInputStream(file));
          } catch (FileNotFoundException e) {
            // do nothing, just print stack trace
            e.printStackTrace();
            return;
          }
          SWFTreeListener listener = new SWFTreeListener(
              rootNode, Investigator.this);
          reader.addListener(listener);
          reader.read();
          nodeNumber = listener.getNodeNumber() + 1; // +1 because of root node
          displayNodeNumber();
          tree.setScrollsOnExpand(true);
          tree.setRootVisible(true);
          tree.setShowsRootHandles(true);
          expandButton.setEnabled(true);
          openButton.setEnabled(true);
          refreshButton.setEnabled(true);
          treePanel.revalidate();
          treePanel.repaint();
          System.gc(); // let's release some precious RAM
          if (listener.isProtected()) {
            JOptionPane.showMessageDialog(
              Investigator.this,
              "This SWF document contains a Protect tag. Make sure you don't violate any copyrights!",
              "Protected SWF", JOptionPane.WARNING_MESSAGE);
          }
        }
      };
    readThread.start();
  }

  private final class SWFFileFilter extends FileFilter {
    public String getDescription() {
      return "Flash files (*.swf)";
    }

    public boolean accept(File f) {
      if (f.isDirectory()) {
        return true;
      }
      String name = f.getName().toLowerCase();
      if (name.endsWith(".swf")) {
        return true;
      }
      return false;
    }
  }

  private final class SplashWindow extends JWindow {
    private boolean showLicense;

    public SplashWindow(Frame f) {
      super(f);
      showLicense = true;
      openSplash(f);
    }

    public SplashWindow(final Frame f, final long millisecs) {
      super(f);
      Thread splashThread = new Thread() {
          public void run() {
            openSplash(f);
            try {
              Thread.sleep(millisecs);
            } catch (InterruptedException e) {
              // do nothing
            } finally {
              closeSplash();
            }
          }
        };
      splashThread.start();
    }

    public void enableCloseOnClick() {
      addMouseListener(
        new MouseAdapter() {
          public void mousePressed(MouseEvent e) {
            closeSplash();
          }
        });
    }

    void closeSplash() {
      setVisible(false);
      dispose();
      if (infoButton != null) {
        infoButton.setEnabled(true);
      }
    }

    void openSplash(Frame f) {
      JPanel infoPane = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
      URL logoUrl     = ClassLoader.getSystemResource(
          "com/jswiff/investigator/resources/logo.png");
      JLabel logo     = new JLabel(new ImageIcon(logoUrl));
      logo.setBorder(new BevelBorder(BevelBorder.LOWERED));
      infoPane.add(logo);
      JLabel copy = new JLabel(
          "<html><h3>JSwiff Investigator (v " + SWFDocument.JSWIFF_VERSION +
          ")</h3>" +
          "This software is free, you are welcome to redistribute it<br>" +
          "under the terms of the GNU General Public License." +
          "<br><p>Part of <b>JSwiff</b>, an open source Java framework<br>" +
          "for Macromedia Flash file generation and manipulation.<br>" +
          "More information at http://www.jswiff.com<br><br>" +
          "&copy;&nbsp; 2004-2005 Ralf Terdic.<br></<html>");
      infoPane.add(copy);
      JPanel mainPane = new JPanel();
      mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.Y_AXIS));
      mainPane.setBorder(new SoftBevelBorder(BevelBorder.RAISED));
      mainPane.add(infoPane);
      if (showLicense) {
        JScrollPane licenseScrollPane = getLicenseScrollPane();
        mainPane.add(licenseScrollPane);
      }
      getContentPane().add(mainPane);
      pack();
      setLocationRelativeTo(f);
      setVisible(true);
    }

    private JScrollPane getLicenseScrollPane() {
      URL licenseUrl          = getClass().getResource(
          "/com/jswiff/investigator/resources/license.html");
      JEditorPane licensePane = new JEditorPane();
      licensePane.setEditable(false);
      licensePane.setContentType("text/html");
      try {
        licensePane.setPage(licenseUrl);
      } catch (IOException e) {
        e.printStackTrace();
      }
      JScrollPane licenseScrollPane = new JScrollPane(licensePane);
      licenseScrollPane.setPreferredSize(new Dimension(440, 150));
      licenseScrollPane.setMaximumSize(new Dimension(440, 150));
      licenseScrollPane.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
      return licenseScrollPane;
    }
  }

  private class NodeStringTransferable implements Transferable {
    private String nodeString;
    private DataFlavor stringFlavor = new DataFlavor(String.class, "String");

    public NodeStringTransferable(String nodeString) {
      this.nodeString = nodeString;
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
      if (flavor == stringFlavor) {
        return true;
      }
      return false;
    }

    public Object getTransferData(DataFlavor flavor) {
      return nodeString;
    }

    public DataFlavor[] getTransferDataFlavors() {
      return new DataFlavor[] { stringFlavor };
    }
  }
}
