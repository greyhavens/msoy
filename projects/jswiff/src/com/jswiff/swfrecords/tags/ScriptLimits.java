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
package com.jswiff.swfrecords.tags;

import com.jswiff.io.InputBitStream;
import com.jswiff.io.OutputBitStream;

import java.io.IOException;


/**
 * This tag is used to override the default values for maximum recursion depth
 * (i.e. how often a function can call itself) and timeout regarding the
 * execution of actions.
 *
 * @since SWF 7
 */
public final class ScriptLimits extends Tag {
	private int maxRecursionDepth;
	private int scriptTimeoutSeconds;

	/**
	 * Creates a new ScriptLimits tag. Supply the maximum recursion depth and
	 * the timeout in seconds.
	 *
	 * @param maxRecursionDepth maximum recursion depth (at most 65535)
	 * @param scriptTimeoutSeconds timeout in seconds
	 */
	public ScriptLimits(int maxRecursionDepth, int scriptTimeoutSeconds) {
		code						  = TagConstants.SCRIPT_LIMITS;
		this.maxRecursionDepth		  = maxRecursionDepth;
		this.scriptTimeoutSeconds     = scriptTimeoutSeconds;
	}

	ScriptLimits() {
		// empty
	}

	/**
	 * Sets the maximum recursion depth, i.e. how often a function can
	 * successively call itself.
	 *
	 * @param maxRecursionDepth maximum recursion depth
	 */
	public void setMaxRecursionDepth(int maxRecursionDepth) {
		this.maxRecursionDepth = maxRecursionDepth;
	}

	/**
	 * Returns the maximum recursion depth, i.e. how often a function can
	 * successively call itself.
	 *
	 * @return maximum recursion depth
	 */
	public int getMaxRecursionDepth() {
		return maxRecursionDepth;
	}

	/**
	 * Sets the timeout, i.e. the maximum time allowed for the execution of an
	 * action block. If this time has elapsed and the execution hasn't
	 * finished, Flash Player asks the user whether to continue or to abort
	 * further execution.
	 *
	 * @param scriptTimeoutSeconds timeout value in seconds
	 */
	public void setScriptTimeoutSeconds(int scriptTimeoutSeconds) {
		this.scriptTimeoutSeconds = scriptTimeoutSeconds;
	}

	/**
	 * Returns the timeout, i.e. the maximum time allowed for the execution of
	 * an action block. If this time has elapsed and the execution hasn't
	 * finished, Flash Player asks the user whether to continue or to abort
	 * further execution.
	 *
	 * @return timeout value in seconds
	 */
	public int getScriptTimeoutSeconds() {
		return scriptTimeoutSeconds;
	}

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		outStream.writeUI16(maxRecursionDepth);
		outStream.writeUI16(scriptTimeoutSeconds);
	}

	void setData(byte[] data) throws IOException {
		InputBitStream inStream = new InputBitStream(data);
		maxRecursionDepth		 = inStream.readUI16();
		scriptTimeoutSeconds     = inStream.readUI16();
	}
}
