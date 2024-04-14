package com.itzkz.usercenter.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;

@Data
public class TeamUserVO implements Serializable {
    private static final long serialVersionUID = 526818456756645627L;
    /**
     * id
     */
    private Long id;

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
    private Date expiretime;

    /**
     * 用户id
     */
    private Long userid;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;
    /**
     * 创建时间
     */
    private Date createtime;
    /**
     * 更新时间
     */
    private Date updatetime;
    /**
     * 队伍中用户信息
     */
    HashSet<UserVO> joinTeamUser;


}
