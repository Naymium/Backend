package com.spaceNav.nyamium.apiPayLoad.exception.handler;

import com.spaceNav.nyamium.apiPayLoad.code.BaseErrorCode;
import com.spaceNav.nyamium.apiPayLoad.exception.GeneralException;

public class GraphHandler extends GeneralException {

    public GraphHandler(BaseErrorCode code) {
        super(code);
    }
}
