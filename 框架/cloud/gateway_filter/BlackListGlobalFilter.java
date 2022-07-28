package com.kayak.cloud.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @ProjectName: cloud-demo
 * @Package: com.kayak.cloud.filter
 * @Description: 黑名单IP拦截, 白名单IP拦截同理
 * @Author: yangshuai
 * @CreateDate: 2022/7/27 15:03
 * @Version 1.0
 */
@Component
public class BlackListGlobalFilter implements GlobalFilter, Ordered {

    public static final List<String> blackList = new ArrayList<>();
    static{
        blackList.add("127.0.0.1");
//        blackList.add("localhost");
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获取请求ip
        String ip = exchange.getRequest().getHeaders().getHost().getHostString();
        //查询数据库 查看ip是否在黑名单存在    考虑到数据库并发，通常项目启动时会读取数据存放到缓存中
        if (!blackList.contains(ip))
            //放行
            return chain.filter(exchange);
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
        HashMap<String, Object> map = new HashMap<>();
        map.put("code", HttpStatus.UNAUTHORIZED.value());
        map.put("msg","该用户是黑名单用户");
        byte[] bytes=new byte[0];
        try {
            bytes = new ObjectMapper().writeValueAsBytes(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        //通过buffer工厂将字节数组包装成一个数据包
        DataBuffer wrap = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(wrap));
    }

    @Override
    public int getOrder() {
        return -5;
    }
}
