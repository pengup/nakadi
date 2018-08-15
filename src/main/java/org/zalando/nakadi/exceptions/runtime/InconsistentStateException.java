package org.zalando.nakadi.exceptions.runtime;

public class InconsistentStateException extends NakadiRuntimeBaseException {

    public InconsistentStateException(final String msg, final Throwable cause) {
        super(msg, cause);
    }

    public InconsistentStateException(final String msg) {
        super(msg);
    }

}
