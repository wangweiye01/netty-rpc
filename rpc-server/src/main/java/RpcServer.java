import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wangweiye
 */
public class RpcServer {
    /**
     * 注册表，存放接口类和实现对象的对应关系 ex: key=cc.wangweiye.SomeService  value=SomeServiceImpl(实例化的对象)
     */
    private Map<String, Object> registerMap = new HashMap<>();
    /**
     * 存放指定包中业务接口实现类的全限定名 ex: cc.wangweiye.service.SomeServiceImpl
     */
    private List<String> classCache = new ArrayList<>();


    /**
     * 发布服务：将指定包中的业务接口实现类实例写入到注册表
     *
     * @param basePackage
     * @throws Exception
     */
    public void publish(String basePackage) throws Exception {
        // 将指定包中的业务接口实现类名写入到classCache中
        cacheClassCache(basePackage);
        // 将指定包中的业务接口实现类实例写入到注册表
        doRegister();
    }

    private void cacheClassCache(String basePackage) {
        // 获取指定包目录中的资源
        URL resource = this.getClass().getClassLoader()
                // com.abc.service  =>  com/abc/service
                .getResource(basePackage.replaceAll("\\.", "/"));

        // 增强程序健壮性，若指定的目录中没有资源，则直接返回
        if (resource == null) {
            return;
        }

        File dir = new File(resource.getFile());
        // 遍历指定目录中的所有文件
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                // 若当前file为目录，则递归
                cacheClassCache(basePackage + "." + file.getName());
            } else if (file.getName().endsWith(".class")) {
                // 去掉文件名后的.class后辍
                String fileName = file.getName().replace(".class", "").trim();
                // 将类的全限定性类名写入到classCache
                classCache.add(basePackage + "." + fileName);
            }
        }
    }

    /**
     * 将指定包中的业务接口实现类实例写入到注册表
     * 注册表是一个map
     * key为业务接口名，即微服务名称
     * value为该业务接口对应的实现类实例
     *
     * @throws Exception
     */
    private void doRegister() throws Exception {
        if (classCache.size() == 0) {
            return;
        }

        for (String className : classCache) {
            // 将当前遍历的类加载到内存
            Class<?> clazz = Class.forName(className);
            // 一个实现类只能实现一个接口，包括Dubbo也是这么玩的
            registerMap.put(clazz.getInterfaces()[0].getName(), clazz.getDeclaredConstructor().newInstance());
        }
    }

    /**
     * 启动服务器
     *
     * @throws InterruptedException
     */
    public void start() throws InterruptedException {
        EventLoopGroup parentGroup = new NioEventLoopGroup();
        EventLoopGroup childGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(parentGroup, childGroup)
                    // 用于指定当Server的连接请求处理线程全被占用时，
                    // 临时存放已经完成了三次握手的请求的队列的长度。
                    // 默认是50
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    // 指定使用心跳机制来保证TCP长连接的存活性
                    // 这个心跳机制是操作系统级别实现的
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            //编码器
                            pipeline.addLast(new ObjectEncoder());
                            //解码器
                            pipeline.addLast(new ObjectDecoder(Integer.MAX_VALUE,
                                    ClassResolvers.cacheDisabled(null)));
                            pipeline.addLast(new RpcServerHandler(registerMap));
                        }
                    });
            //监听端口，启动Server
            ChannelFuture future = bootstrap.bind(8888).sync();
            System.out.println("服务端已启动，监听的端口为：8888");
            future.channel().closeFuture().sync();
        } finally {
            parentGroup.shutdownGracefully();
            childGroup.shutdownGracefully();
        }
    }
}
