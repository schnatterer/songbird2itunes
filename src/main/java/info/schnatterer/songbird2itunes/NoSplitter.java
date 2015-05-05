/**
 * Copyright (C) 2013 Johannes Schnatterer
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.schnatterer.songbird2itunes;

import java.util.LinkedList;
import java.util.List;

import com.beust.jcommander.converters.IParameterSplitter;

/**
 * jcommander parameter splitter that does not split parameters.
 * 
 * @author schnatterer
 * 
 */
public class NoSplitter implements IParameterSplitter {
	/*
	 * (non-Javadoc)
	 * 
	 * @see com.beust.jcommander.converters.IParameterSplitter#split(java.lang.String)
	 */
	@Override
	public List<String> split(final String value) {
		List<String> result = new LinkedList<String>();
		result.add(value);
		return result;
	}
}
