package com.itzkz.usercenter.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class TeamUpdateDTO {

    /**
     * 队伍id
     */
    private long id;
    /**
     * 队伍名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 最大人数
     */
    private Integer maxnum;

    /**
     * 过期时间
     */
    @JsonFormat(pattern = "yyyy/MM/dd")
    private Date expiretime;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

    /**
     * 密码
     */
    private String password;

}
