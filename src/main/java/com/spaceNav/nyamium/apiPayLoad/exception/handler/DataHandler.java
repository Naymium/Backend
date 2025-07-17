package com.spaceNav.nyamium.apiPayLoad.exception.handler;

import com.spaceNav.nyamium.apiPayLoad.code.BaseErrorCode;
import com.spaceNav.nyamium.apiPayLoad.exception.GeneralException;

public class DataHandler extends GeneralException {
    public DataHandler(BaseErrorCode code) {
        super(code);
    }
}
