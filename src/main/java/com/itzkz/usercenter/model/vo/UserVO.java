package com.itzkz.usercenter.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class UserVO implements Serializable {
    private static final long serialVersionUID = -8009503147750729031L;
    /**
     *
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String username;

    /**
     * 账号
     */
    private String useraccount;

    /**
     * 用户头像
     */
    private String avatarurl;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 状态 0-正常
     */
    private Integer userstatus;

    /**
     * 用户介绍
     */
    private String profile;

    /**
     * 电话
     */
    private String phone;
    /**
     * 标签列表
     */
    private String tags;
    /**
     * 创建时间
     */
    private Date createtime;

    /**
     * 更新时间
     */
    private Date updatetime;
    /**
     * 用户标签是否为空  true 空 false 不空
     */
    private boolean tagsisnoll;
    /**
     * 用户角色 0普通用户 1 管理员
     */
    private int userrole;

}
