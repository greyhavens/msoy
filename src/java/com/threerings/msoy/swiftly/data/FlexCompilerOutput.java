//
// $Id$

package com.threerings.msoy.swiftly.data;

import java.util.HashMap;
import java.util.Map;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import com.threerings.io.Streamable;

/** A compiler message. */
public class FlexCompilerOutput
    implements CompilerOutput, Streamable
{
    public FlexCompilerOutput ()
    {
    }

    /**
     * Create a new FlexCompilerOutput instance by parsing the given compiler message.
     */
    public FlexCompilerOutput (String flexMessage)
    {
        Matcher match;

        // Match standard flex compiler messages.
        match = _FLEX_MSG_PATTERN.matcher(flexMessage);
        if (match.matches() == true) {
            String level;

            // If the regex matched, there's no way the integers can be
            // invalid.
            _fileName = match.group(1);
            _lineNumber = Integer.parseInt(match.group(2));                
            _columnNumber = Integer.parseInt(match.group(3));
            level = match.group(4);
            _message = match.group(5);

            // Parse the level string.
            if ((_level = _messageLevels.get(level)) == null) {
                _level = CompilerOutput.Level.UNKNOWN;
            }
            return;
        }

        // Unknown message type
        _message = flexMessage;
        _level = CompilerOutput.Level.UNKNOWN;
        _fileName = null;
        _lineNumber = -1;
        _columnNumber = -1;
        return;
    }

    /**
     * Create a new FlexCompilerOutput instance with the provided settings.
     */
    public FlexCompilerOutput (String message, Level level, String fileName, int lineNumber, int columnNumber)
    {
        _message = message;
        _level = level;
        _fileName = fileName;
        _lineNumber = lineNumber;
        _columnNumber = columnNumber;
    }

    // from CompilerOutput interface
    public int getLineNumber ()
    {
        return _lineNumber;
    }

    // from CompilerOutput interface
    public int getColumnNumber () {
        return _columnNumber;
    }

    // from CompilerOutput interface
    public String getFileName ()
    {
        return _fileName;
    }

    // from CompilerOutput interface
    public String getMessage ()
    {
        return _message;
    }

    // from CompilerOutput interface
    public Level getLevel ()
    {
        return _level;
    }

    @Override
    public boolean equals (Object other)
    {
        if (other instanceof CompilerOutput) {
            CompilerOutput that = (CompilerOutput) other;
            // Line numbers and columns must be equal
            if (_lineNumber != that.getLineNumber() || _columnNumber != that.getColumnNumber()) {
                return false;
            }

            // Levels must be equal
            if (!_level.equals(that.getLevel())) {
                return false;
            }

            // File name must either be null (and thus equal), or the strings
            // must be equal
            if (_fileName != that.getFileName() && !_fileName.equals(that.getFileName())) {
                return false;
            }

            // Message must be equal
            if (!_message.equals(that.getMessage())) {
                return false;
            }

            // We got this far, it must be true!
            return true;
        } else {
            return false;                
        }
    }
    
    /** Line number referenced. -1 if no line number. */
    protected int _lineNumber = -1;

    /** Line number referenced. -1 if no line number. */
    protected int _columnNumber = -1;

    /** File name referenced. null if no file. */
    protected String _fileName = null;

    /** Compiler text message. */
    protected String _message;

    /** Message severity level. */
    protected Level _level;

    /**
     * Matches standard flex error/warning messages.
     * Ex: /srcpath/Mirror/Mirror.as(39): col: 50 Error: Something bad happened true
     */
    protected static final Pattern _FLEX_MSG_PATTERN = Pattern.compile("(.*)\\(([0-9]+)\\): col: ([0-9]+) ([A-Za-z]+): (.*)");

    /** Map flex compiler level strings to CompilerOutput.Level enums. */
    protected static final Map<String,CompilerOutput.Level> _messageLevels = new HashMap<String,CompilerOutput.Level>();

    // Initialize String -> Enum level mappings.
    static {
        _messageLevels.put("Error", CompilerOutput.Level.ERROR);
    }
}
