package com.itzkz.usercenter.controller;

import com.itzkz.usercenter.common.BaseResponse;
import com.itzkz.usercenter.common.ResultUtils;
import com.itzkz.usercenter.tools.AliOSSUtils;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;

@RestController
@Slf4j
@Api(tags = "文件接口")
@RequestMapping("/file")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class UploadController {
    @Resource
    private AliOSSUtils aliOSSUtils;

    //    本地存储文件
//    @PostMapping("/upload")
//    public Result upload(String username , Integer age, MultipartFile image) throws IOException {
//        log.info("文件上传:{},{},{}",username,age,image);
//
//        String originalFilename = image.getOriginalFilename();
//        int index = originalFilename.lastIndexOf(".");
//        String extname = originalFilename.substring(index);
//        String newFileName =  UUID.randomUUID().toString()+extname;
//        image.transferTo(new File("E:\\image\\"+ newFileName));
//
//        return  Result.success();
//
//    }
    @PostMapping("/upload/image")
    public BaseResponse<String> upload(@RequestPart("file") MultipartFile multipartFile) throws IOException {
        log.info("文件上传, 文件名:{}", multipartFile.getOriginalFilename());
        String url = aliOSSUtils.upload(multipartFile);
        log.info("文件上传完成,文件访问的url:{}", url);
        return ResultUtils.success(url);
    }
}
