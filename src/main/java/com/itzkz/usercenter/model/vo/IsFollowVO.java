package com.itzkz.usercenter.model.vo;

import lombok.Data;

import java.io.Serializable;

@Data
public class IsFollowVO implements Serializable {

    private static final long serialVersionUID = -2341968611683471172L;
    /**
     * 默认没有关注
     */
    private boolean follow = false;

}
