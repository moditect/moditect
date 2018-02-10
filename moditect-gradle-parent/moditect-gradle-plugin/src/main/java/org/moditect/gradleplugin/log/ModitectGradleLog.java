/**
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
package org.moditect.gradleplugin.log;

import org.moditect.spi.log.Log;
import org.gradle.api.logging.Logger;

/**
 * Gradle logger wrapper for Log SPI.
 *
 * @author Pratik Parikh
 */
public class ModitectGradleLog implements Log {

    private final Logger delegate;

    public ModitectGradleLog(Logger delegate) {
        this.delegate = delegate;
    }

    @Override
    public void debug(CharSequence message) {
        delegate.debug( String.valueOf(message) );
    }

    @Override
    public void info(CharSequence message) {
        delegate.info( String.valueOf(message) );
    }

    @Override
    public void warn(CharSequence message) {
        delegate.warn( String.valueOf(message) );
    }

    @Override
    public void error(CharSequence message) {
        delegate.error( String.valueOf(message) );
    }
}
