package com.itzkz.usercenter.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class JoinTeamDTO implements Serializable {

    private static final long serialVersionUID = 8683322055289518279L;

    /**
     * 队伍id
     */
    private long id;


    /**
     * 房间密码
     */
    private String password;








}