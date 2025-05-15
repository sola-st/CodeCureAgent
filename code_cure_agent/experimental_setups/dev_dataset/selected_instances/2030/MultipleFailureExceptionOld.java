package org.junit.internal.runners.model;

import java.util.List;

@Deprecated
public class MultipleFailureExceptionOld extends org.junit.runners.model.MultipleFailureException {
    private static final long serialVersionUID = 1L;

    public MultipleFailureExceptionOld(List<Throwable> errors) {
        super(errors);
    }
}
