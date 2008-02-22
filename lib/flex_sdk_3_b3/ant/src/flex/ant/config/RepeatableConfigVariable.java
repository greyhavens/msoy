/*************************************************************************
 * 
 * ADOBE CONFIDENTIAL
 * __________________
 * 
 *  [2002] - [2007] Adobe Systems Incorporated 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 */
package flex.ant.config;

/**
 * Provides a base class for Configuration Variables that can take on multiple values.
 *
 * Consumers of this class must implement the <code>add</code> method.
 */
public abstract class RepeatableConfigVariable extends BaseConfigVariable
{
    /**
     * Creates a <code>RepeatableConfigVariable</code> instance with the specified <code>OpitonSpec</code>.
     */
    protected RepeatableConfigVariable(OptionSpec spec)
    {
        super(spec);
    }

    /**
     * Adds <code>value</code> as a value to this <code>RepeatableConfigVariable</code>.
     *
     * @param value the value to this <code>RepeatableConfigVariable</code>
     */
    public abstract void add(String value);

    /**
     * Adds every String in <code>values</code> as a value of this <code>RepeatableConfigVariable</code> by calling the <code>add</code> method with each String as an argument.
     * @param values an array of Strings
     */
    public void addAll(String[] values)
    {
        for (int i = 0; i < values.length; i++)
            this.add(values[i]);
    }

} //End of RepeatableConfigVariable
