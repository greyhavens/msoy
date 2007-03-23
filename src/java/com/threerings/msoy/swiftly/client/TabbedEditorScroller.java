package com.threerings.msoy.swiftly.client;

import java.awt.Component;

import javax.swing.JScrollPane;

import com.threerings.msoy.swiftly.data.PathElement;

public class TabbedEditorScroller extends JScrollPane
    implements TabbedEditorComponent
{
    public TabbedEditorScroller (Component view, PathElement pathElement)
    {
        super(view, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
              JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        _pathElement = pathElement;
    }

    // from TabbedEditorComponent
    public PathElement getPathElement ()
    {
        return _pathElement;
    }

    // from TabbedEditorComponent
    public void setPathElement (PathElement element)
    {
        _pathElement = element;
    }

    protected PathElement _pathElement;
}
