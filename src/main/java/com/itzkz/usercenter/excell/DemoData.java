package com.itzkz.usercenter.excell;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class DemoData {
    @ExcelProperty("用户账号")
    private String username;
    @ExcelProperty("用户编号")
    private String id;

}