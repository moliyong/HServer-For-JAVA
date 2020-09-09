package test1.hook;

import top.hserver.core.interfaces.HookAdapter;
import top.hserver.core.ioc.annotation.Autowired;
import top.hserver.core.ioc.annotation.Hook;
import test1.service.HelloService;
import test1.service.Test;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Hook(value = Test.class, method = {"show","ac"})
public class HookTest implements HookAdapter {

    @Autowired
    private HelloService helloService;

    @Override
    public void before(Object[] objects) {
        log.debug("aop.-前置拦截");
    }

    @Override
    public Object after(Object object) {
        return object + "aop-后置拦截"+helloService.sayHello();
    }
}
