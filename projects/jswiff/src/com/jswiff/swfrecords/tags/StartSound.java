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
import com.jswiff.swfrecords.SoundInfo;

import java.io.IOException;


/**
 * This tag instructs the Flash Player to play a sound previously defined with
 * the <code>DefineSound</code> tag. Playback parameters can be defined by
 * supplying a <code>SoundInfo</code> instance.
 *
 * @see SoundInfo
 * @since SWF 1
 */
public final class StartSound extends Tag {
	private int soundId;
	private SoundInfo soundInfo;

	/**
	 * Creates a new StartSound tag. Supply the character ID of the sound to be
	 * played, and playback otions in an <code>SoundInfo</code> instance.
	 *
	 * @param soundId the sound's character ID
	 * @param soundInfo playback options
	 */
	public StartSound(int soundId, SoundInfo soundInfo) {
		code			   = TagConstants.START_SOUND;
		this.soundId	   = soundId;
		this.soundInfo     = soundInfo;
	}

	StartSound() {
		// empty
	}

	/**
	 * Sets the character ID of the sound to be played.
	 *
	 * @param soundId sound's character ID
	 */
	public void setSoundId(int soundId) {
		this.soundId = soundId;
	}

	/**
	 * Returns the character ID of the sound to be played.
	 *
	 * @return sound's character ID
	 */
	public int getSoundId() {
		return soundId;
	}

	/**
	 * Sets the defined playback parameters for the sound to be played.
	 *
	 * @param soundInfo playback options
	 */
	public void setSoundInfo(SoundInfo soundInfo) {
		this.soundInfo = soundInfo;
	}

	/**
	 * Returns the defined playback parameters for the sound to be played.
	 *
	 * @return playback options
	 */
	public SoundInfo getSoundInfo() {
		return soundInfo;
	}

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		outStream.writeUI16(soundId);
		soundInfo.write(outStream);
	}

	void setData(byte[] data) throws IOException {
		InputBitStream inStream = new InputBitStream(data);
		soundId		  = inStream.readUI16();
		soundInfo     = new SoundInfo(inStream);
	}
}
