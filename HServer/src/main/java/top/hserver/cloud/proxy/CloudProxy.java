package top.hserver.cloud.proxy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.hserver.cloud.bean.InvokeServiceData;
import top.hserver.cloud.client.handler.RpcClientHandler;
import top.hserver.core.ioc.annotation.Resource;

/**
 * @author hxm
 */
public class CloudProxy {

    private static final Logger log = LoggerFactory.getLogger(CloudProxy.class);

    private static Map<String, Object> IOC = new HashMap<>();

    public static Object getProxy(Class clazz, Resource resource) throws InstantiationException, IllegalAccessException {

        String value = resource.value();
        if (value.trim().length() > 0) {
            Object o = IOC.get(value);
            if (o != null) {
                return o;
            }
        }
        Object o = IOC.get(clazz.getName());
        if (o != null) {
            return o;
        }
        // 代理工厂
        ProxyFactory proxyFactory = new ProxyFactory();
        // 设置需要创建子类的父类
        if (clazz.isInterface()) {
            proxyFactory.setInterfaces(new Class[]{clazz});
        } else {
            proxyFactory.setSuperclass(clazz);
        }

        Object o1 = proxyFactory.createClass().newInstance();
        ((ProxyObject)o1).setHandler((self, thisMethod, proceed, args) -> {
            //这里实现远程调用啦！
            InvokeServiceData invokeServiceData = new InvokeServiceData();
            invokeServiceData.setMethod(thisMethod);
            if (resource.value().trim().length() > 0) {
                invokeServiceData.setaClass(resource.value());
            } else {
                invokeServiceData.setaClass(clazz.getName());
            }
            String requestId = UUID.randomUUID().toString();
            invokeServiceData.setRequestId(requestId);
            invokeServiceData.setServerName(resource.serverName());
            invokeServiceData.setObjects(args);
            return RpcClientHandler.sendInvoker(invokeServiceData);
        });

        if (value.trim().length() > 0) {
            IOC.put(value, o1);
        } else {
            IOC.put(clazz.getName(), o1);
        }
        return o1;
    }

    public static void clearCache() {
        IOC.clear();
    }
}
