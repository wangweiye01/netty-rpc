package cc.wangweiye.service;

import cc.wangweiye.SomeService;

/**
 * @author wangweiye
 */
public class SomeServiceImpl implements SomeService {
    @Override
    public String hello(String name) {
        return name + "欢迎你";
    }
}
