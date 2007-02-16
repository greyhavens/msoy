/*
 * JSwiff is an open source Java API for Macromedia Flash file generation
 * and manipulation
 *
 * Copyright (C) 2004-2006 Ralf Terdic (contact@jswiff.com)
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
 * TODO: Comments
 */
public class ImportAssets2 extends ImportAssets {
  /**
   * Creates a new ImportAssets2 instance.
   *
   * @param url TODO: Comments
   * @param importMappings TODO: Comments
   */
  public ImportAssets2(String url, ImportMapping[] importMappings) {
    super(url, importMappings);
    code = TagConstants.IMPORT_ASSETS_2;
  }

  ImportAssets2() {
    // empty
  }

  protected void writeData(OutputBitStream outStream) throws IOException {
    outStream.writeString(url);
    outStream.writeUI8((short) 1);
    outStream.writeUI8((short) 0);
    int count = importMappings.length;
    outStream.writeUI16(count);
    for (int i = 0; i < count; i++) {
      outStream.writeUI16(importMappings[i].getCharacterId());
      outStream.writeString(importMappings[i].getName());
    }
  }

  void setData(byte[] data) throws IOException {
    InputBitStream inStream = new InputBitStream(data);
    url = inStream.readString();
    inStream.readUI16();
    int count = inStream.readUI16();
    importMappings = new ImportMapping[count];
    for (int i = 0; i < count; i++) {
      int characterId   = inStream.readUI16();
      String name       = inStream.readString();
      importMappings[i] = new ImportMapping(name, characterId);
    }
  }
}
