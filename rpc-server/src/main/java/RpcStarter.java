/**
 * @author wangweiye
 */
public class RpcStarter {
    public static void main(String[] args) throws Exception {
        RpcServer server = new RpcServer();
        server.publish("cc.wangweiye.service");
        server.start();
    }
}
