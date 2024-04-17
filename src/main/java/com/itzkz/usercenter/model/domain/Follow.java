package com.itzkz.usercenter.model.domain;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 用户关注关系表
 * @TableName follow
 */
@TableName(value ="follow")
@Data
public class Follow implements Serializable {
    /**
     * 关注关系表ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关注者的用户ID
     */
    private Long followeruserid;

    /**
     * 被关注者的用户ID
     */
    private Long followeduserid;

    /**
     * 关注时间
     */
    private Date followtime;

    /**
     * 创建时间
     */
    private Date createtime;

    /**
     * 更新时间
     */
    private Date updatetime;

    /**
     * 是否删除，0表示未删除，1表示已删除
     */
    @TableLogic
    private Integer isdelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}