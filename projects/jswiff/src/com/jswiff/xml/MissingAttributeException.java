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

package com.jswiff.xml;

/**
 * Indicates that a mandatory attribute is missing within a specific parent
 * element.
 */
public class MissingAttributeException extends MissingNodeException {
  private String missingAttributeName;
  private String parentElementPath;

  /**
   * Creates a new MissingAttributeException instance. Pass the name of the
   * missing attribute and the path of the parent element (in XPath notation).
   *
   * @param missingAttributeName name of the missing element
   * @param parentElementPath TODO: Comments
   */
  public MissingAttributeException(
    String missingAttributeName, String parentElementPath) {
    super(
      "Mandatory attribute '" + missingAttributeName + "' missing from " +
      parentElementPath);
    this.missingAttributeName   = missingAttributeName;
    this.parentElementPath      = parentElementPath;
  }

  /**
   * Returns the name of the missing attribute.
   *
   * @return missing attribute name
   */
  public String getMissingAttributeName() {
    return missingAttributeName;
  }

  /**
   * Returns the path of the parent element (in XPath notation).
   *
   * @return parent element path
   */
  public String getParentElementPath() {
    return parentElementPath;
  }
}
