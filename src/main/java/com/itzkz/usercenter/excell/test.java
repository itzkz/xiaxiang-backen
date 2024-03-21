package com.itzkz.usercenter.excell;

import com.alibaba.excel.EasyExcel;

public class test {
    public static void main(String[] args) {
        // since: 3.0.0-beta1
        String fileName = "E:\\project\\UserCenter\\src\\main\\resources\\test.xlsx";
        // 这里默认每次会读取100条数据 然后返回过来 直接调用使用数据就行
        // 具体需要返回多少行可以在`PageReadListener`的构造函数设置
        EasyExcel.read(fileName, DemoData.class, new DemoDataListener(
        )).sheet().doRead();
    }
}
