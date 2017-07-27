/**
 *  Copyright 2017 Gunnar Morling (http://www.gunnarmorling.de/)
 *  and/or other contributors as indicated by the @authors tag. See the
 *  copyright.txt file in the distribution for a full listing of all
 *  contributors.
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
package org.moditect.mavenplugin.util;

import org.moditect.spi.log.Log;

/**
 * Log delegating to the Maven logging API.
 *
 * @author Gunnar Morling
 */
public class MojoLog implements Log {

    private final org.apache.maven.plugin.logging.Log delegate;

    public MojoLog(org.apache.maven.plugin.logging.Log delegate) {
        this.delegate = delegate;
    }

    @Override
    public void debug(CharSequence message) {
        delegate.debug( message );
    }

    @Override
    public void info(CharSequence message) {
        delegate.info( message );
    }

    @Override
    public void warn(CharSequence message) {
        delegate.warn( message );
    }

    @Override
    public void error(CharSequence message) {
        delegate.error( message );
    }
}
