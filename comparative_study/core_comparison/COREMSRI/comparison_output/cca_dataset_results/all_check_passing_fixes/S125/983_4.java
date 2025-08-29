// Copyright 2014-02-09 PlanBase Inc. & Glen Peterson
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.organicdesign.fp;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 A dumping ground for utility functions that aren't useful enough to belong in
 {@link StaticImports}.
 */
public class FunctionUtils {

    // I don't want any instances of this class.
    private FunctionUtils() {
        throw new UnsupportedOperationException("No instantiation");
    }

    public static @NotNull String stringify(@Nullable Object o) {
        if (o == null) { return "null"; }
        if (o instanceof String) { return "\"" + o + "\""; }
        return o.toString();
    }

    public static @NotNull String ordinal(final int origI) {
        final int i = (origI < 0) ? -origI : origI;
        final int modTen = i % 10;
        if ( (modTen < 4) && (modTen > 0)) {
            int modHundred = i % 100;
            if ( (modHundred < 21) && (modHundred > 3) ) {
                return Integer.toString(origI) + "th";
            }
            switch (modTen) {
                case 1: return Integer.toString(origI) + "st";
                case 2: return Integer.toString(origI) + "nd";
                case 3: return Integer.toString(origI) + "rd";
            }
        }
        return Integer.toString(origI) + "th";
    }
}

