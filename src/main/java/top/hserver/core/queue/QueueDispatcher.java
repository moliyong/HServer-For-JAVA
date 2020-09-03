package top.hserver.core.queue;

import lombok.extern.slf4j.Slf4j;
import top.hserver.core.ioc.IocUtil;
import top.hserver.core.ioc.annotation.queue.QueueHanler;
import top.hserver.core.ioc.annotation.queue.QueueListener;
import top.hserver.core.ioc.ref.PackageScanner;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;


/**
 * @author hxm
 */
@Slf4j
public class QueueDispatcher {
    private static Map<String, QueueHandleInfo> handleMethodMap = new ConcurrentHashMap<>();

    private QueueDispatcher() {
    }

    /**
     * 初始化事件分发器
     */
    public static void init(PackageScanner scanner) throws IOException {
        // 载入事件处理类
        List<Class<?>> classes = scanner.getAnnotationList(QueueListener.class);
        // 解析事件处理类
        for (Class<?> clazz : classes) {
            QueueListener queueListener = clazz.getAnnotation(QueueListener.class);
            if (queueListener == null) {
                continue;
            }
            QueueHandleInfo eventHandleInfo = new QueueHandleInfo();
            eventHandleInfo.setQueueHandlerType(queueListener.type());
            eventHandleInfo.setQueueName(queueListener.queueName());
            eventHandleInfo.setBufferSize(queueListener.bufferSize());
            Object obj;
            try {
                obj = clazz.newInstance();
            } catch (Exception e) {
                log.error("initialize " + clazz.getSimpleName() + " error", e);
                continue;
            }
            IocUtil.addBean(queueListener.queueName(), obj);
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                QueueHanler queueHanler = method.getAnnotation(QueueHanler.class);
                if (queueHanler != null) {
                    eventHandleInfo.add(new QueueHandleMethod(method, queueHanler.size(), queueHanler.level()));
                    log.debug("寻找队列 [{}] 的方法 [{}.{}]", queueListener.queueName(), clazz.getSimpleName(),
                            method.getName());
                }
            }
            handleMethodMap.put(queueListener.queueName(), eventHandleInfo);
        }
    }

    /**
     * 创建队列
     */
    public static void startTaskThread() {
        handleMethodMap.forEach((k, v) -> {
            QueueFactory queueFactory = new QueueFactoryImpl();
            queueFactory.createQueue(v.getQueueName(), v.getBufferSize(), v.getQueueHandlerType(), v.getQueueHandleMethods());
            v.setQueueFactory(queueFactory);

        });
    }

    /**
     * 分发事件
     *
     * @param queueName 事件URI
     * @param args      事件参数
     */
    public static void dispatcherEvent(String queueName, Object... args) {
        QueueHandleInfo queueHandleInfo = handleMethodMap.get(queueName);
        if (queueHandleInfo != null) {
            queueHandleInfo.getQueueFactory().producer(args);
        } else {
            log.error("不存在:{} 队列", queueName);
        }
    }

    public static QueueInfo queueInfo(String queueName) {
        QueueHandleInfo queueHandleInfo = handleMethodMap.get(queueName);
        if (queueHandleInfo != null) {
            return queueHandleInfo.getQueueFactory().queueInfo();
        }
        return null;
    }

}