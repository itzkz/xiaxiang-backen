package com.itzkz.usercenter.controller;

import com.itzkz.usercenter.common.BaseResponse;
import com.itzkz.usercenter.common.ResultUtils;
import com.itzkz.usercenter.model.domain.Tags;
import com.itzkz.usercenter.service.TagsService;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Api(tags = "标签接口")
@RequestMapping("/tags")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class TagsController {
    @Resource
    private TagsService tagsService;


    @GetMapping("/list")
    public BaseResponse<Map<String, List<String>>> tagsList() {
        // 获取所有标签
        List<Tags> tagsList = tagsService.list();

        // 构建标签Map
        Map<String, List<String>> tagsMap = new HashMap<>();
        for (Tags tags : tagsList) {
            tagsMap.put(tags.getParenttagname(), Arrays.asList(tags.getChildtags().replaceAll("\\[|\\]|\"", "").split(",")));
        }
        return ResultUtils.success(tagsMap);
    }


}
