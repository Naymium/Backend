package com.spaceNav.nyamium.apiPayLoad.exception.handler;

import com.spaceNav.nyamium.apiPayLoad.code.BaseErrorCode;
import com.spaceNav.nyamium.apiPayLoad.exception.GeneralException;

public class FileHandler extends GeneralException {
  public FileHandler(BaseErrorCode code) {
    super(code);
  }
}
