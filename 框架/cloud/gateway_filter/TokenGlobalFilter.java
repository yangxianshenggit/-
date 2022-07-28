package com.kayak.cloud.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @ProjectName: cloud-demo
 * @Package: com.kayak.cloud.filter
 * @Description: java类作用描述
 * @Author: yangshuai
 * @CreateDate: 2022/7/28 9:25
 * @Version 1.0
 */
@Component
public class TokenGlobalFilter implements GlobalFilter, Ordered {
    //@Value("${preferPaths}")
    public static List<String> preferPaths = new ArrayList<>();
    static {
        preferPaths.add("login-service");
        preferPaths.add("register-service");
    }
    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * token校验步骤:通常和前端定好：一般token都放在请求头，key:Authorization value:bearer token
     * 1、所有服务除登录注册不校验token，其余都进行校验
     * 2、从请求头获取token
     * 3、校验token是否在缓存中存在
     * 4、存在则放行
     *
     * @param exchange
     * @param chain
     * @return
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain){

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String[] split = path.split("/");
        if (preferPaths.contains(split[1]))
            return chain.filter(exchange);
        List<String> authorization = request.getHeaders().get("Authorization");
        if (authorization!=null&&!authorization.isEmpty()) {
            String token = authorization.get(0).replace("bearer ", "");
            token = redisTemplate.opsForValue().get(token);
            if (StringUtils.isNotEmpty(token))
                return chain.filter(exchange);
        }
        ServerHttpResponse response = exchange.getResponse();
        HashMap<String, Object> map = new HashMap<>();
        map.put("code", HttpStatus.UNAUTHORIZED.value());
        map.put("msg", "未授权");
        byte[] bytes = new byte[0];
        try {
            bytes = new ObjectMapper().writeValueAsBytes(map);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        DataBuffer wrap = response.bufferFactory().wrap(bytes);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON_UTF8);
        return response.writeWith(Mono.just(wrap));
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
