package com.threerings.msoy.swiftly.client;

import java.awt.Color;
import java.util.StringTokenizer;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

public class ActionScriptStyledDocument extends DefaultStyledDocument
{
    public ActionScriptStyledDocument ()
    {
        putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");
        initializeAttributes();
    }

    @Override // from DefaultStyledDocument
    public void insertString(int offset, String string, AttributeSet set)
        throws BadLocationException
    {
        super.insertString(offset, string, set);
        processChanges(offset, string.length());
    }

    @Override // from DefaultStyledDocument
    public void remove(int offset, int length)
        throws BadLocationException
    {
        super.remove(offset, length);
        processChanges(offset, 0);
    }

    protected void initializeAttributes ()
    {
        StyleConstants.setBold(_keyword, true);
        StyleConstants.setForeground(_keyword, Color.blue);
        StyleConstants.setBold(_types, true);
        StyleConstants.setForeground(_types, Color.orange);
        _separators = "0123456789() \n\t\r";
    }

    // TODO actually do something useful here
    protected void processChanges(int offset, int length)
    {
        Element element = getParagraphElement(offset);
        offset = element.getStartOffset();
        String line = "";
        try {
            line = getText(offset, element.getEndOffset() - offset); 
        } catch (BadLocationException be) {
            // TODO
        }

        // clear any attributes 
        setCharacterAttributes(offset, line.length(), _normal, true);

        StringTokenizer tokens = new StringTokenizer(line, _separators, true);
        String currentToken;
        int position;

        while (tokens.hasMoreTokens())
        {
            currentToken = tokens.nextToken();
            position = line.indexOf(currentToken);
            if (_tokens.reserved.contains(currentToken.trim())) {
                setCharacterAttributes(position + offset, currentToken.length(), _keyword, true);
            } else if (_tokens.types.contains(currentToken.trim())) {
                setCharacterAttributes(position + offset, currentToken.length(), _types, true);
            }
        }
    } 

    protected SimpleAttributeSet _normal = new SimpleAttributeSet();
    protected SimpleAttributeSet _keyword = new SimpleAttributeSet();
    protected SimpleAttributeSet _types = new SimpleAttributeSet();
    protected ActionScriptTokens _tokens = new ActionScriptTokens();
    protected String _separators;
}
