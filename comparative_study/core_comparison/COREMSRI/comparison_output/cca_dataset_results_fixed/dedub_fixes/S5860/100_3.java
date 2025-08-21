/*
 * Copyright 2017-2020 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.opentracing.contrib.jdbc.parser;

import java.util.regex.Pattern;


/**
 * Parser for DB2
 *
 * @author oburgosm
 * @since 0.2.12
 */
public class DB2URLParser extends AbstractMatcherURLParser {


    private static final Pattern DB2_URL_PATTERN = Pattern
        .compile("jdbc:db2:\\/\\/(?<host>[^:\\/]+)(:(?<port>\\d+))?\\/(?<instance>[^:]+)(:(?<options>.*))?");

    private static final String DB2_TYPE = "db2";

    public DB2URLParser() {
        super(DB2_URL_PATTERN, DB2_TYPE);
    }

}
```
Since the code only defines the pattern and does not use any group by number or ignore the named groups, there is no violation to fix here. The pattern defines named groups and presumably, the superclass AbstractMatcherURLParser uses these names properly. The code does not use group numbers or unused groups. Therefore, no change is necessary.