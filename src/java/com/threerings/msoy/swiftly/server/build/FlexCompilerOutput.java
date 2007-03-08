//
// $Id$

package com.threerings.msoy.swiftly.server.build;

/** A compiler message. */
public class FlexCompilerOutput
    implements CompilerOutput
{

    /**
     * Create a new FlexCompilerOutput instance by parsing the given compiler message.
     */
    public FlexCompilerOutput (String flexMessage)
    {
    }

    /**
     * Create a new FlexCompilerOutput instance with the provided settings.
     */
    public FlexCompilerOutput (String message, Level level, String fileName, int lineNumber)
    {
        _message = message;
        _level = level;
        _fileName = fileName;
        _lineNumber = lineNumber;
    }

    // from CompilerOutput interface
    public int getLineNumber()
    {
        return _lineNumber;
    }

    // from CompilerOutput interface
    public String getFileName()
    {
        return _fileName;
    }

    // from CompilerOutput interface
    public String getMessage()
    {
        return _message;
    }

    // from CompilerOutput interface
    public Level getLevel()
    {
        return _level;
    }

    @Override
    public boolean equals (Object other)
    {
        if (other instanceof CompilerOutput) {
            CompilerOutput that = (CompilerOutput) other;
            // Line numbers and levels must be equal
            if (_lineNumber != that.getLineNumber()) {
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
    protected int _lineNumber;

    /** File name referenced. null if no file. */
    protected String _fileName;

    /** Compiler text message. */
    protected String _message;

    /** Message severity level. */
    protected Level _level;
}
