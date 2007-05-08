package com.threerings.msoy.swiftly.client;

import java.awt.Component;

import javax.swing.JScrollPane;

import com.threerings.msoy.swiftly.data.PathElement;

public class TabbedEditorScroller extends JScrollPane
    implements TabbedEditorComponent
{
    public TabbedEditorScroller (PositionableComponent view, PathElement pathElement)
    {
        super(view.getComponent(), JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
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

    // from TabbedEditorComponent
    public Component getEditingComponent ()
    {
        return getViewport().getView();
    }

    // from PositionableComponent
    public Component getComponent ()
    {
        return this;
    }

    // from PositionableComponent
    public void gotoLocation (int row, int column, boolean highlight)
    {
        // unless the setters on JScrollPane are overriden and the view is stored as an instance
        // variable as a PositionableComponent, this cast is probably unavoidable and presumptuous 
        ((PositionableComponent)getEditingComponent()).gotoLocation(row, column, highlight);
    }

    protected PathElement _pathElement;
}
