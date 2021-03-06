package top.hserver.cloud.client.handler;

import io.netty.channel.Channel;
import io.netty.channel.pool.SimpleChannelPool;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import top.hserver.cloud.bean.InvokeServiceData;
import top.hserver.cloud.bean.ResultData;
import top.hserver.cloud.bean.ServiceData;
import top.hserver.cloud.client.RpcClient;
import top.hserver.cloud.future.HFuture;
import top.hserver.cloud.future.RpcWrite;
import top.hserver.cloud.util.DynamicRoundRobin;
import top.hserver.core.server.exception.RpcException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author hxm
 */
public class RpcClientHandler {

    //服务名，List<服务集群host和IP>
    public final static Map<String, DynamicRoundRobin> CLASS_STRING_MAP = new ConcurrentHashMap<>();

    private static void reg(ServiceData serviceData) {
        DynamicRoundRobin dynamicRoundRobin = CLASS_STRING_MAP.get(serviceData.getServerName());
        if (dynamicRoundRobin != null) {
            dynamicRoundRobin.add(serviceData);
        } else {
            dynamicRoundRobin = new DynamicRoundRobin();
            dynamicRoundRobin.add(serviceData);
            CLASS_STRING_MAP.put(serviceData.getServerName(), dynamicRoundRobin);
        }
    }

    public static void nacosClear(String serverName) {
        CLASS_STRING_MAP.remove(serverName);
    }

    public static void nacosReg(String host, Integer port, String serverName) {
        ServiceData serviceData = new ServiceData();
        serviceData.setHost(host);
        serviceData.setPort(port);
        serviceData.setServerName(serverName);
        reg(serviceData);
    }

    public static void defaultReg(String addressData) throws RuntimeException {
        try {
            String[] split = addressData.split(",");
            for (String s : split) {
                String[] split1 = s.split("@");
                String name = split1[1];
                String[] split2 = split1[0].split(":");
                String address = split2[0];
                String port = split2[1];
                ServiceData serviceData = new ServiceData();
                serviceData.setHost(address);
                serviceData.setPort(Integer.parseInt(port));
                serviceData.setServerName(name);
                reg(serviceData);
            }
        } catch (Exception e) {
            throw new RuntimeException("格式异常，例子: 127.0.0.1:8888@ServerName");
        }

    }


    public static Object sendInvoker(InvokeServiceData invokeServiceData) throws Exception {
        DynamicRoundRobin dynamicRoundRobin = CLASS_STRING_MAP.get(invokeServiceData.getServerName());
        if (dynamicRoundRobin != null) {
            ServiceData serviceData = dynamicRoundRobin.choose();
            if (serviceData != null) {
                HFuture hFuture = new HFuture();
                SimpleChannelPool pool = RpcClient.channels.get(serviceData.getInetSocketAddress());
                Future<Channel> acquire = pool.acquire();
                acquire.addListener((FutureListener<Channel>) future -> {
                    //给服务端发送数据
                    Channel channel = future.getNow();
                    RpcWrite.writeAndSync(channel, invokeServiceData, hFuture);
                    // 连接放回连接池，这里一定记得放回去
                    pool.release(channel);
                });
                try {
                    ResultData response = hFuture.get(5, TimeUnit.SECONDS);
                    if (response.getCode().code() == 200) {
                        return response.getData();
                    }
                    if (response.getError() != null) {
                        throw new RpcException(response.getError());
                    } else {
                        throw new RpcException("远程调用异常");
                    }
                } catch (Exception e) {
                    throw new RpcException(e.getMessage());
                } finally {
                    RpcWrite.removeKey(invokeServiceData.getRequestId());
                }
            }
        }
        throw new RpcException("暂无服务");
    }
}
