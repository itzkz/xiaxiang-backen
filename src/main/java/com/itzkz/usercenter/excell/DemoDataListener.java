package com.itzkz.usercenter.excell;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class DemoDataListener implements ReadListener<DemoData> {


    @Override
    public void invoke(DemoData data, AnalysisContext context) {

        System.out.println(data);
    }

    /**
     * 所有数据解析完成了 都会来调用
     *
     * @param context
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 这里也要保存数据，确保最后遗留的数据也存储到数据库
        log.info("所有数据解析完成！");
    }


}