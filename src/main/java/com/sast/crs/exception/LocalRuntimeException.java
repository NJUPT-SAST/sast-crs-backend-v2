package com.sast.crs.exception;

import com.sast.crs.enums.ErrorEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
public class LocalRuntimeException extends RuntimeException {

    private ErrorEnum errorEnum;

    public LocalRuntimeException(String message) {
        super(message);
    }

    public LocalRuntimeException(ErrorEnum errorEnum) {
        super(errorEnum.getErrMsg());
        this.errorEnum = errorEnum;
    }

}
