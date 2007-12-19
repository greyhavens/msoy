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
 * Provides a base class for <code>ConfigVariable</code> and
 * <code>RepeatableConfigVariable</code>. This abstract class encapsulates all
 * of the functionality that any ConfigVariable must have that does not
 * involve "setting" it.
 *
 * Consumers of this class must implement the <code>addToCommandline</code>
 * method.
 */
public abstract class BaseConfigVariable implements OptionSource
{
    /**
     * The <code>OptionSpec</code> describing the names that this <code>ConfigVariable</code> should match.
     */
    protected final OptionSpec spec;

    /**
     * Create a Configuration Variable with the specified <code>OptionSpec</code>.
     */
    protected BaseConfigVariable(OptionSpec spec)
    {
        this.spec = spec;
    }

    /**
     * Adds arguments to the end of <code>cmdl</code> corresponding to the state of this variable.
     *
     * @param cmld The Commandline object to which arguments correspond to this option should be added
     */
    public abstract void addToCommandline(Commandline cmdl);

    /**
     * @return the OptionSpec associated with this instance.
     */
    public OptionSpec getSpec()
    {
        return spec;
    }

    /**
     * Returns the result of calling matches() on <code>spec</code> with <code>option</code> as the argument.
     *
     * @return true of <code>option</code> matches <code>spec</code>, and false otherwise.
     */
    public boolean matches(String option)
    {
        return spec.matches(option);
    }
}
