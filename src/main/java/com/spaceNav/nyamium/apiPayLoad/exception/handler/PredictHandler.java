package com.spaceNav.nyamium.apiPayLoad.exception.handler;

import com.spaceNav.nyamium.apiPayLoad.code.BaseErrorCode;
import com.spaceNav.nyamium.apiPayLoad.exception.GeneralException;

public class PredictHandler extends GeneralException {

    public PredictHandler(BaseErrorCode code) {
        super(code);
    }
}
