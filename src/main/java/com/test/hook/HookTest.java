package com.test.hook;

import com.hserver.core.interfaces.HookAdapter;
import com.hserver.core.ioc.annotation.Hook;
import com.test.service.Test;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Hook(value = Test.class, method = "show")
public class HookTest implements HookAdapter {

    @Override
    public void before(Object[] objects) {
//        log.info("aop.-前置拦截：" + objects[0]);
        objects[0]="666";
    }

    @Override
    public Object after(Object object) {
        return object + "aop-后置拦截";
    }
}
