package com.itzkz.usercenter.common;

import lombok.Data;

import java.io.Serializable;

@Data
public class ResultPage implements Serializable {
    private static final long serialVersionUID = -3554621124348757984L;
    /**
     * 每页展示数据量
     */
    private int pageSize = 10;

    /**
     * 当前页数为几
     */
    private int pageNum = 1;

}
