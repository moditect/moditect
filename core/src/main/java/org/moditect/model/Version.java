/*
 *  Copyright 2017 - 2018 The ModiTect authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.moditect.model;

import java.util.Objects;

/**
 * Simplified version of Runtime.Version (which requires Java 9+).
 */
public class Version {

	private final int feature;

	private Version(int feature) {
		this.feature = feature;
	}

	public static Version valueOf(int feature) {
		return new Version(feature);
	}

	public static Version valueOf(Object feature) {
		return new Version(Integer.parseInt(String.valueOf(feature)));
	}

	public int feature() {
		return feature;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Version))
			return false;
		Version version = (Version) o;
		return feature == version.feature;
	}

	@Override
	public int hashCode() {
		return Objects.hash(feature);
	}

	@Override
	public String toString() {
		return "Version " + feature;
	}

}
