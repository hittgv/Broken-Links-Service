package io.restall.hittgv.broken;

import com.sun.org.apache.regexp.internal.RE;

public enum State {
    SUCCESS, FAIL, REDIRECT;

    public static State fromStatusCode(Integer statusCode) {
        if (statusCode == null) {
            return FAIL;
        }
        if (statusCode > 399) {
            return FAIL;
        }
        if (statusCode > 299) {
            return REDIRECT;
        }
        return SUCCESS;
    }
    }
