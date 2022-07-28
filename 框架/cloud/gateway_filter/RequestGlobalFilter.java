package com.kayak.cloud.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;

/**
 * @ProjectName: cloud-demo
 * @Package: com.kayak.cloud.filter
 * @Description: 自定义全局过滤器
 *             GlobalFilter：没有集成web，不能使用Filter接口，使用gateway自带的
 *             Ordered：过滤器排序的接口，责任链设计模式：过滤器链、mybatis二级缓存变种责任链模式
 * @Author: yangshuai
 * @CreateDate: 2022/7/27 14:01
 * @Version 1.0
 */
@Component
public class RequestGlobalFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //ServerHttpRequest（webFlux 响应式编程）和HttpServletRequest(web)差不多
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        //uri:ip/域名+port+路径
        String path = request.getURI().getPath();
        //头信息
        HttpHeaders headers = request.getHeaders();
        //请求方式
        String name = request.getMethod().name();
        //ipv4
        String ip = headers.getHost().getHostString();

        //设置响应的字符编码集
        //response.getHeaders().set("content-type","application/json;charset=utf-8");
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
        //组装业务返回值
        HashMap<String, Object> map = new HashMap<>(4);
        map.put("code", HttpStatus.UNAUTHORIZED.value());
        map.put("msg","未授权");
        byte[] bytes=new byte[0];
        try {
            //把map转成字节数组
             bytes = new ObjectMapper().writeValueAsBytes(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        //通过buffer工厂将字节数组包装成一个数据包
        DataBuffer wrap = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(wrap));

        //放行
        //return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        //返回数字越小，顺序越靠前
        return 0;
    }
}
