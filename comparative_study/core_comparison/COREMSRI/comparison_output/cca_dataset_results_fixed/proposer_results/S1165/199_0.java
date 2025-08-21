```java
/*
 * Copyright (C) 2015 Matija Mazi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package si.mazi.rescu;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class AwareException extends RuntimeException implements InvocationAware, HttpResponseAware {

    @Nullable
    private final RestInvocation invocation;
    private final Map<String, List<String>> responseHeaders;

    public AwareException(Exception e, @Nullable RestInvocation invocation, Map<String, List<String>> responseHeaders) {
        super(e);
        this.invocation = invocation;
        this.responseHeaders = responseHeaders;
    }

    @Override
    public void setInvocation(@Nullable RestInvocation invocation) {
        throw new UnsupportedOperationException("AwareException is immutable");
    }

    @Nullable
    public RestInvocation getInvocation() {
        return invocation;
    }

    @Override
    public void setResponseHeaders(Map<String, List<String>> headers) {
        throw new UnsupportedOperationException("AwareException is immutable");
    }

    @Override
    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }
}
