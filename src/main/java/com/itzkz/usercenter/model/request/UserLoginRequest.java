package com.itzkz.usercenter.model.request;

import lombok.Data;

import java.io.Serializable;
@Data
public class UserLoginRequest implements Serializable {


    private static final long serialVersionUID = 6776081054871581240L;
    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户密码
     */
    private String userPassword;

}
