package com.cv4j.netdiscovery.core;

import com.alibaba.fastjson.JSON;
import com.cv4j.netdiscovery.core.domain.SpiderEntity;
import com.cv4j.netdiscovery.core.http.Request;
import com.cv4j.netdiscovery.core.queue.Queue;
import com.cv4j.proxy.ProxyPool;
import com.cv4j.proxy.domain.Proxy;
import com.safframework.tony.common.utils.Preconditions;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tony on 2018/1/2.
 */
@Slf4j
public class SpiderEngine {

    private List<Spider> spiders = new ArrayList<>();

    private Queue queue;

    private SpiderEngine() {
    }

    public static SpiderEngine create() {

        return new SpiderEngine();
    }

    public SpiderEngine queue(Queue queue) {

        this.queue = queue;
        return this;
    }

    public SpiderEngine proxyList(List<Proxy> proxies) {

        ProxyPool.addProxyList(proxies);
        return this;
    }

    public SpiderEngine addSpider(Spider spider) {

        if (spider!=null) {
            spiders.add(spider);
        }
        return this;
    }

    public void httpd(int port) {

        HttpServer server = Vertx.vertx().createHttpServer();

        Router router = Router.router(Vertx.vertx());

        if (Preconditions.isNotBlank(spiders)) {

            for (Spider spider:spiders) {
                router.route("/netdiscovery/spider/"+spider.getName()).handler(routingContext -> {

                    // 所有的请求都会调用这个处理器处理
                    HttpServerResponse response = routingContext.response();
                    response.putHeader("content-type", "application/json");

                    SpiderEntity entity = new SpiderEntity();
                    entity.setSpiderName(spider.getName());
                    entity.setSpiderStatus(spider.getSpiderStatus());
                    entity.setLeftRequestSize(spider.getQueue().getLeftRequests(spider.getName()));

                    // 写入响应并结束处理
                    response.end(JSON.toJSONString(entity));
                });
            }
        }

        server.requestHandler(router::accept).listen(port);
    }

    public void run() {

        if (Preconditions.isNotBlank(spiders)) {

            spiders.stream().forEach(spider -> spider.run());
        }
    }

    public static void main(String[] args) {

        SpiderEngine engine = new SpiderEngine();

        Spider spider = Spider.create()
                .name("tony")
                .request(new Request("http://www.163.com/"))
                .request(new Request("https://www.baidu.com/"))
                .request(new Request("https://www.baidu.com/"));

        engine.addSpider(spider);
        engine.run();

        engine.httpd(8080);
    }

}
