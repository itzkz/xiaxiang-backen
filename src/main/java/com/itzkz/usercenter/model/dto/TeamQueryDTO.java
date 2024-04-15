package com.itzkz.usercenter.model.dto;

import com.itzkz.usercenter.common.ResultPage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
public class TeamQueryDTO extends ResultPage {


    private static final long serialVersionUID = -6481747545697422512L;
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
     * 创建人id
     */
    private Long userId;

    /**
     * 搜素词
     */
    private String searchText;


    /**
     * 过期时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expiretime =null;

    /**
     * 0 - 公开，1 - 私有，2 - 加密
     */
    private Integer status;

}
