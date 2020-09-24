package top.hserver.core.server.handlers;

import top.hserver.core.interfaces.*;
import top.hserver.core.ioc.IocUtil;
import top.hserver.core.server.context.*;
import top.hserver.core.server.exception.BusinessException;
import top.hserver.core.server.router.RouterInfo;
import top.hserver.core.server.router.RouterManager;
import top.hserver.core.server.router.RouterPermission;
import top.hserver.core.server.util.ExceptionUtil;
import top.hserver.core.server.util.ParameterUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 分发器
 *
 * @author hxm
 */
@Slf4j
public class DispatcherHandler {

    private final static StaticHandler STATIC_HANDLER = new StaticHandler();

    /**
     * 静态文件的处理
     *
     * @param hServerContext
     * @return
     */
    static HServerContext staticFile(HServerContext hServerContext) {
        StaticFile handler = STATIC_HANDLER.handler(hServerContext.getRequest().getUri(), hServerContext);
        if (handler != null) {
            hServerContext.setStaticFile(true);
            hServerContext.setStaticFile(handler);
        }
        return hServerContext;
    }

    /**
     * 权限验证
     *
     * @param hServerContext
     * @return
     */
    static HServerContext permission(HServerContext hServerContext) {

        //如果是静态文件就不进行权限验证了
        if (hServerContext.isStaticFile()) {
            return hServerContext;
        }
        List<PermissionAdapter> listBean = IocUtil.getListBean(PermissionAdapter.class);
        if (listBean != null) {
            RouterPermission routerPermission = RouterManager.getRouterPermission(hServerContext.getRequest().getUri(), hServerContext.getRequest().getRequestType());
            if (routerPermission != null) {
                for (PermissionAdapter permissionAdapter : listBean) {
                    if (routerPermission.getRequiresPermissions() != null) {
                        try {
                            permissionAdapter.requiresPermissions(routerPermission.getRequiresPermissions(), hServerContext.getWebkit());
                            if (hServerContext.getWebkit().httpResponse.hasData()) {
                                break;
                            }
                        } catch (Exception e) {
                            throw new BusinessException(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), "权限验证", e, hServerContext.getWebkit());
                        }
                    }
                    if (routerPermission.getRequiresRoles() != null) {
                        try {
                            permissionAdapter.requiresRoles(routerPermission.getRequiresRoles(), hServerContext.getWebkit());
                            if (hServerContext.getWebkit().httpResponse.hasData()) {
                                break;
                            }
                        } catch (Exception e) {
                            throw new BusinessException(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), "角色验证", e, hServerContext.getWebkit());
                        }
                    }
                    if (routerPermission.getSign() != null) {
                        try {
                            permissionAdapter.sign(routerPermission.getSign(), hServerContext.getWebkit());
                            if (hServerContext.getWebkit().httpResponse.hasData()) {
                                break;
                            }
                        } catch (Exception e) {
                            throw new BusinessException(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), "Sign验证", e, hServerContext.getWebkit());
                        }
                    }
                }
            }
        }
        return hServerContext;
    }

    /**
     * 拦截器
     *
     * @param hServerContext
     * @return
     */
    public static HServerContext filter(HServerContext hServerContext) {

        /**
         * 如果静态文件就跳过当前的处理，否则就去执行控制器的方法
         */
        if (hServerContext.isStaticFile()) {
            return hServerContext;
        }

        /**
         * 检测下Filter的过滤哈哈
         */
        if (IocUtil.getListBean(FilterAdapter.class) != null) {
            try {
                List<FilterAdapter> listBean = IocUtil.getListBean(FilterAdapter.class);
                for (FilterAdapter filterAdapter : listBean) {
                    filterAdapter.doFilter(hServerContext.getWebkit());
                    if (hServerContext.getWebkit().httpResponse.hasData()) {
                        break;
                    }
                }
            } catch (Exception e) {
                throw new BusinessException(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), "拦截器异常", e, hServerContext.getWebkit());
            }
        }
        return hServerContext;
    }


    /**
     * 去执行控制器的方法
     *
     * @param hServerContext
     * @return
     */
    static HServerContext findController(HServerContext hServerContext) {

        /**
         * 如果静态文件就跳过当前的处理，否则就去执行控制器的方法
         */
        if (hServerContext.isStaticFile()) {
            return hServerContext;
        }

        /**
         * 检查下Webkit是否设置了值
         *  文本输出
         *  下载
         *  重定向
         *
         */
        if (hServerContext.getWebkit().httpResponse.hasData()) {
            return hServerContext;
        }

        RouterInfo routerInfo = RouterManager.getRouterInfo(hServerContext.getRequest().getUri(), hServerContext.getRequest().getRequestType(), hServerContext);
        if (routerInfo == null) {
            StringBuilder error = new StringBuilder();
            error.append("未找到对应的控制器，请求方式：")
                    .append(hServerContext.getRequest().getRequestType().toString())
                    .append("，请求路径：")
                    .append(hServerContext.getRequest().getUri())
                    .append("，来源IP：")
                    .append(hServerContext.getRequest().getIp());
            throw new BusinessException(HttpResponseStatus.NOT_FOUND.code(), error.toString(), new Exception("不能找到处理当前请求的资源"), hServerContext.getWebkit());
        }
        Method method = routerInfo.getMethod();
        Class<?> aClass = routerInfo.getAClass();
        Object bean = IocUtil.getBean(aClass);
        //检查下方法参数
        Object res;
        //如果控制器有参数，那么就进行，模糊赋值，在检测是否有req 和resp
        try {
            Object[] methodArgs;
            try {
                methodArgs = ParameterUtil.getMethodArgs(aClass, method, hServerContext);
            } catch (Exception e) {
                throw new BusinessException(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), "控制器方法调用时传入的参数异常", e, hServerContext.getWebkit());
            }
            res = method.invoke(bean, methodArgs);
            //调用结果进行设置
            if (res == null) {
                hServerContext.getResponse().sendText("");
            } else if (String.class.getName().equals(res.getClass().getName())) {
                hServerContext.getResponse().sendText(res.toString());
            } else {
                //非字符串类型的将对象转换Json字符串
                try {
                    hServerContext.getResponse().sendJson(res);
                } catch (Exception e) {
                    throw new BusinessException(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), "返回的数据非字符串被转换JSON异常", e, hServerContext.getWebkit());
                }
            }
        } catch (InvocationTargetException e) {
            throw new BusinessException(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), "调用方法失败", e.getTargetException(), hServerContext.getWebkit());
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new BusinessException(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), "调用控制器时参数异常", e, hServerContext.getWebkit());
        }
        return hServerContext;
    }

    /**
     * 构建返回对象
     *
     * @param hServerContext
     * @return
     */
    static FullHttpResponse buildResponse(HServerContext hServerContext) {
        try {
            FullHttpResponse response = null;
            /**
             * 如果是文件特殊处理下,是静态文件，同时需要下载的文件
             */
            if (hServerContext.isStaticFile()) {
                //显示型的静态文件
                response = BuildResponse.buildStaticShowType(hServerContext);
            } else if (hServerContext.getResponse().isDownload()) {
                //控制器下载文件的
                response = BuildResponse.buildControllerDownloadType(hServerContext.getResponse());
            } else if (hServerContext.getResponse().getResult() != null) {
                String tempResult = hServerContext.getResponse().getResult();
                List<ResponseAdapter> responseAdapters = IocUtil.getListBean(ResponseAdapter.class);
                if (responseAdapters != null) {
                    for (ResponseAdapter responseAdapter : responseAdapters) {
                        tempResult = responseAdapter.result(hServerContext.getResponse().getResult());
                    }
                }
                hServerContext.getResponse().setResult(tempResult);
                //是否Response的
                response = BuildResponse.buildHttpResponseData(hServerContext.getResponse());
            } else {
                //使用了代理模式，让用户自由控制ctx 不受框架操作
                if (hServerContext.getResponse().isProxy()) {
                    return null;
                }
            }
            return BuildResponse.buildEnd(response, hServerContext.getResponse());
        } catch (Exception e) {
            throw new BusinessException(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), "构建Response对象异常", e, hServerContext.getWebkit());
        }
    }


    /**
     * 异常Resp
     *
     * @param webkit
     * @return
     */
    private static FullHttpResponse buildExceptionResponse(Webkit webkit) {
        FullHttpResponse response = null;
        Response httpResponse = (Response) webkit.httpResponse;
        if (httpResponse.isDownload()) {
            //控制器下载文件的
            response = BuildResponse.buildControllerDownloadType(httpResponse);
        } else if (httpResponse.getResult() != null) {
            String tempResult = httpResponse.getResult();
            List<ResponseAdapter> responseAdapters = IocUtil.getListBean(ResponseAdapter.class);
            if (responseAdapters != null) {
                for (ResponseAdapter responseAdapter : responseAdapters) {
                    tempResult = responseAdapter.result(httpResponse.getResult());
                }
            }
            httpResponse.setResult(tempResult);
            //是否Response的
            response = BuildResponse.buildHttpResponseData(httpResponse);
        }
        if (response == null) {
            response = BuildResponse.buildString("你开启了全局异常处理，但是你没有处理.");
        }
        return BuildResponse.buildEnd(response, httpResponse);
    }

    /**
     * 构建返回对象
     *
     * @param e
     * @return
     */
    static FullHttpResponse handleException(Throwable e) {
        try {
            //一般是走自己的异常
            if (e.getCause() instanceof BusinessException) {
                BusinessException e1 = (BusinessException) e.getCause();
                if (e1.getHttpCode() == HttpResponseStatus.NOT_FOUND.code()) {
                    log.error(e1.getErrorDescription());
                } else {
                    log.error(ExceptionUtil.getMessage(e1.getThrowable()));
                }
                List<GlobalException> listBean = IocUtil.getListBean(GlobalException.class);
                if (listBean != null) {
                    for (GlobalException globalException : listBean) {
                        globalException.handler(e1.getThrowable(), e1.getHttpCode(), e1.getErrorDescription(), e1.getWebkit());
                        if (e1.getWebkit().httpResponse.hasData()) {
                            break;
                        }
                    }
                    return buildExceptionResponse(e1.getWebkit());
                } else {
                    return BuildResponse.buildError(e1);
                }
            } else {
                log.error(ExceptionUtil.getMessage(e));
                return BuildResponse.buildError(e);
            }
        } catch (Exception e2) {
            return BuildResponse.buildError(e2);
        }
    }

    /**
     * 终极输出
     *
     * @param ctx
     * @param future
     * @param msg
     */
    static void writeResponse(ChannelHandlerContext ctx, CompletableFuture<HServerContext> future, FullHttpResponse msg) {
        //等于null 是让用户自己调度
        if (msg != null) {
            List<ResponseAdapter> responseAdapters = IocUtil.getListBean(ResponseAdapter.class);
            if (responseAdapters != null) {
                for (ResponseAdapter responseAdapter : responseAdapters) {
                    msg = responseAdapter.response(msg);
                }
            }
            ctx.writeAndFlush(msg);
            future.complete(null);
        }
    }

}
