package com.itzkz.usercenter.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SaveTagsDTO implements Serializable {

    private static final long serialVersionUID = 8683322055289518279L;

    /**
     * 用户保存的标签列表
     */
    private List<String> tagNameList;










}