import cc.wangweiye.SomeService;

/**
 * @author wangweiye
 */
public class RpcConsumer {
    public static void main(String[] args) {
        SomeService service = RpcProxy.create(SomeService.class);
        System.out.println(service.hello("kkb"));
        System.out.println(service.hashCode());
    }
}
