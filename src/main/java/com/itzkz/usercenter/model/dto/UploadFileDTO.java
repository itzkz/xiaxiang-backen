package com.itzkz.usercenter.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 文件上传请求
 *
 * @author qimu
 */
@Data
public class UploadFileDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * 业务
     */
    private String biz;
}