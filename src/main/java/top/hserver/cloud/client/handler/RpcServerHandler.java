package top.hserver.cloud.client.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import top.hserver.cloud.bean.InvokeServiceData;
import top.hserver.cloud.bean.RegRpcData;
import top.hserver.cloud.bean.ResultData;
import top.hserver.cloud.bean.ServiceData;
import top.hserver.cloud.common.Msg;
import top.hserver.cloud.future.SyncWrite;
import top.hserver.cloud.future.SyncWriteFuture;
import top.hserver.cloud.future.SyncWriteMap;
import top.hserver.cloud.util.DynamicRoundRobin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author hxm
 */
@Slf4j
public class RpcServerHandler {

    public final static Map<String, DynamicRoundRobin<ServiceData>> CLASS_STRING_MAP = new ConcurrentHashMap<>();

    static InvokeServiceData readData(ChannelHandlerContext ctx, Msg msg) {
        switch (msg.getMsg_type()) {
            case REG:
                RegRpcData data = ((Msg<RegRpcData>) msg).getData();
                ServiceData serviceData = new ServiceData();
                serviceData.setName(data.getName());
                serviceData.setChannel(ctx.channel());
                data.getClasses().forEach(a -> {
                    if (CLASS_STRING_MAP.containsKey(a)) {
                        CLASS_STRING_MAP.get(a).add(serviceData);
                    } else {
                        DynamicRoundRobin<ServiceData> sd = new DynamicRoundRobin<>();
                        sd.add(serviceData);
                        CLASS_STRING_MAP.put(a, sd);
                    }
                });
                log.debug(data.toString());
                break;
            case RESULT:
                ResultData resultData = ((Msg<ResultData>) msg).getData();
                String requestId = resultData.getUUID();
                SyncWriteFuture future = (SyncWriteFuture) SyncWriteMap.syncKey.get(requestId);
                if (future != null) {
                    future.setResultData(resultData);
                }
            case PINGPONG:
                String s = ((Msg<ResultData>) msg).getData().getData().toString();
                log.debug(s);
                break;
            default:
                break;

        }
        return null;
    }

    public static Object sendInvoker(InvokeServiceData invokeServiceData) throws Exception {
        int size = CLASS_STRING_MAP.get(invokeServiceData.getAClass()) != null ? CLASS_STRING_MAP.get(invokeServiceData.getAClass()).size() : 0;
        for (int i = 0; i < size; i++) {
            ServiceData serviceData = CLASS_STRING_MAP.get(invokeServiceData.getAClass()).choose();
            if (serviceData != null) {
                Channel channel = serviceData.getChannel();
                if (channel != null && channel.isActive()) {
                    ResultData response = new SyncWrite().writeAndSync(channel, invokeServiceData, 5000);
                    switch (response.getCode()) {
                        case 200:
                            return response.getData();
                        case 404:
                            return new NullPointerException("暂无服务");
                        default:
                            return new NullPointerException("远程调用异常");
                    }
                } else {
                    //如果这个服务是不活跃的就干掉他
                    CLASS_STRING_MAP.get(invokeServiceData.getAClass()).remove(serviceData);
                }
            }
        }
        return new NullPointerException("暂无服务");
    }


    /**
     * 关闭一个链接通道
     * @param className
     */
    public static void closeChannel(String className){
        DynamicRoundRobin<ServiceData> serviceDataDynamicRoundRobin = CLASS_STRING_MAP.get(className);
        if (serviceDataDynamicRoundRobin!=null){
            List<ServiceData> all = serviceDataDynamicRoundRobin.getAll();
            for (int i = 0; i < all.size(); i++) {
                all.get(i).getChannel().close();
                all.remove(all.get(i));
            }
            CLASS_STRING_MAP.remove(className);
        }
    }
}