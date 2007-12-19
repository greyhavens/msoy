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

import org.apache.tools.ant.types.Commandline;

/**
 * Provides a base class for Configuration Variables that can be set with a
 * String value.
 *
 * Consumers of this class must implement the <code>set</code> <code>isSet</code> methods.
 */
public abstract class ConfigVariable extends BaseConfigVariable
{
    /**
     * Create a <code>ConfigVariable</code> instance with the specified <code>OptionSpec</code>.
     */
    protected ConfigVariable(OptionSpec spec)
    {
        super(spec);
    }

    /**
     * Set the value of this <code>ConfigVariable</code>
     *
     * @param value the value (as a String) that this <code>ConfigVariable</code> should be set to.
     */
    public abstract void set(String value);

    /**
     * Predicate specifying whether this ConfigVariable has been set. Implementation depends on the implementation of <code>set</code>.
     *
     * @return true if this <code>ConfigVariable</code> has been set, false otherwise.
     */
    public abstract boolean isSet();

} //End of ConfigVariable

