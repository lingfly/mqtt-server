package lingfly;

import lingfly.proto.Decode;
import lingfly.proto.Header;
import lingfly.proto.Message;
import lingfly.proto.constant.MessageType;
import lingfly.proto.messages.*;
import lingfly.proto.mqtt.Qos;
import lingfly.proto.mqtt.RetCode;
import lingfly.proto.mqtt.TopicQos;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class Main {

    static Lock clientsMu = new ReentrantLock();
    static ConcurrentMap<String, IncomingConn> clients = new ConcurrentHashMap<>();

    public static void main(String[] args) {

        int port = 1883;
        String host = "127.0.0.1";
        SocketAddress address = new InetSocketAddress(host, port);
        try {
            //监听端口
            ServerSocket serverSocket = new ServerSocket();
            serverSocket.bind(address);
            Server svr = newServer(serverSocket);
            svr.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    static Server newServer(ServerSocket serverSocket){
        Server svr = new Server(serverSocket,newSubscriptions(2),false,Duration.ofSeconds(10),new Stats());
        svr.run();
        return svr;
    }
    static Subscriptions newSubscriptions(int workers){
        Subscriptions s = new Subscriptions(workers, new LinkedBlockingQueue<>(), new HashMap<>(),new HashMap<>());
        for (int i = 0; i < s.wokers; i++){
            s.run(i);
        }
        return s;
    }
}
class Server{
    ServerSocket serverSocket;
    Subscriptions subs;
    Stats stats;
    volatile boolean Done;
    Duration statsInterval;
    boolean dupm;
    Random rand;

    public Server(ServerSocket serverSocket, Subscriptions subs, boolean done, Duration statsInterval, Stats stats) {
        this.serverSocket = serverSocket;
        this.subs = subs;
        Done = done;
        this.statsInterval = statsInterval;
        this.stats = stats;
    }
    public void run(){
        Thread statRunnable = new Thread(new StatRunnable(),"statRunnable");
        statRunnable.start();
    }
    public void start(){
        Thread serverThread = new Thread(new ServerRunnable(), "serverThread");
        serverThread.start();
    }
    IncomingConn newIncomingConn(Socket conn){
        return new IncomingConn(this, conn, new LinkedBlockingQueue<>(), false);
    }
    class ServerRunnable implements Runnable{
        @Override
        public void run() {
            while (true){
                try {
                    Socket conn = serverSocket.accept();
                    //创建新的线程处理连接
                    IncomingConn cli = newIncomingConn(conn);
                    stats.clientConnect();
                    cli.start();
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

            }
        }
    }
    class StatRunnable implements Runnable{

        @Override
        public void run() {
            while (!Done){
                stats.publish(subs,statsInterval);
            }
            try {
                Thread.sleep(statsInterval.getNano());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
class Subscriptions{
    int wokers;                         //处理分发消息的线程数
    BlockingQueue<Post> posts;          //处理分发消息的工作队列
    Lock mu = new ReentrantLock();      //互斥锁，保护以下字段
    Map<String,List<IncomingConn>> subs;//主题-客户端映射
    List<Wild> wildcards;               //分级主题
    Map<String,Retain> retain;          //遗嘱消息
    Stats stats;                        //分发消息的状态

    public Subscriptions(int wokers, BlockingQueue<Post> posts, Map<String, List<IncomingConn>> subs, Map<String, Retain> retain) {
        this.wokers = wokers;
        this.posts = posts;
        this.subs = subs;
        this.retain = retain;
        wildcards = new ArrayList<>();

    }
    public void submit(IncomingConn c, Publish m){
        try {
            posts.put(new Post(c,m));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void run(int id){
        Thread thread = new Thread(new SubRunnable(id));
        thread.start();
    }
    List<IncomingConn> subscribers(String topic){
        try {
            mu.lock();
            List<IncomingConn> res = subs.get(topic);
            String[] parts = topic.split("/");
            for (Wild w : wildcards){
                if (w.matches(parts)){
                    res.add(w.c);
                }
            }
            return res;
        }
        finally {
            mu.unlock();
        }
    }
    class SubRunnable implements Runnable{
        Logger log = Logger.getLogger("ServerRunnable");
        int id;
        volatile boolean done = false;
        public SubRunnable(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            log.info("worker "+ id + " started");
            while (!done){
                try {
                    Post post = posts.take();
                    boolean isRetain = post.m.getHeader().isRetain();
                    post.m.getHeader().setRetain(false);

                    if (isRetain && post.m.getPayload().size()==0){
                        mu.lock();
                        retain.remove(post.m.getTopicName());
                        mu.unlock();
                        return;
                    }
                    List<IncomingConn> conns = subscribers(post.m.getTopicName());
                    //主题没有任何客户端订阅
                    if (conns == null){
                        continue;
                    }
                    for (IncomingConn c : conns){
//                        if (c == post.c){//不发给发送方
//                            continue;
//                        }
                        if (c != null){
                            c.submit(post.m);
                        }
                    }

                    if (isRetain){
                        mu.lock();
                        Publish msg = post.m;
                        msg.getHeader().setRetain(true);
                        retain.put(post.m.getTopicName(),new Retain(msg));
                        mu.unlock();
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void unsubAll(IncomingConn c){

    }
    public void add(String topic, IncomingConn c){
        try {
            mu.lock();
            if (Wild.isWildcard(topic)){
                Wild w = Wild.newWild(topic, c);
                if (w.valid()){
                    wildcards.add(w);
                }
            }
            else {
                if (subs.containsKey(topic)){
                    subs.get(topic).add(c);
                }
                else {
                    List<IncomingConn> connList = new ArrayList<>();
                    connList.add(c);
                    subs.put(topic, connList);
                }
            }
        }
        finally {
            mu.unlock();
        }
    }
    public void sendRetain(String topic, IncomingConn c){
        List<String> tlist = new ArrayList<>();
        try {
            mu.lock();
            if (Wild.isWildcard(topic)){
                //TODO:
            }
            else {
                tlist.add(topic);
            }
            for (String t : tlist){
                if (retain.containsKey(t)){
                    c.submit(retain.get(t).m);
                }
            }
        }
        finally {
            mu.unlock();
        }
    }
    public void unsub(String topic, IncomingConn c){
        try {
            mu.lock();
            if (subs.containsKey(topic)){
                List<IncomingConn> subConns = subs.get(topic);
                Iterator<IncomingConn> iter = subConns.iterator();
                while (iter.hasNext()){
                    IncomingConn conn = iter.next();
                    if (c.equals(conn)){
                        iter.remove();
                    }
                }

                if (subConns.size() == 0){
                    subs.remove(topic);
                }
            }
        }
        finally {
            mu.unlock();
        }
    }
}
class Post{
    IncomingConn c;
    Publish m;

    public Post(IncomingConn c, Publish m) {
        this.c = c;
        this.m = m;
    }

}
class IncomingConn{
    Server svr;                 //连接所在的MQTT服务
    Socket conn;                //连接所对应的TCP套接字
    BlockingQueue<Job> jobs;    //工作队列
    String clientid;            //客户端标识符
    boolean Done;               //连接是否要关闭
    boolean connected;          //是否已建立连接
    Thread reader;              //读线程
    Thread writer;              //写线程

    void closeConnect(){

    }

    void submit(Message m){
        Job j=new Job(m);
        try {
            jobs.put(j);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public IncomingConn(Server svr, Socket conn, BlockingQueue<Job> jobs, boolean done) {
        this.svr = svr;
        this.conn = conn;
        this.jobs = jobs;
        Done = done;
    }
    public void start(){
        reader = reader();
        writer = writer();
    }
    Thread reader(){
            Thread readThread = new Thread(new ReaderRunnable(this),"ReadThread");
            readThread.start();
            return readThread;
    }
    class ReaderRunnable implements Runnable{
        private Logger log = Logger.getLogger(this.getClass().getName());
        IncomingConn c;

        public ReaderRunnable(IncomingConn c) {
            this.c = c;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    Message msg;
                    msg = Decode.decodeOneMessage(conn.getInputStream());

                    if (msg == null){
                        log.severe("没有匹配的报文类型");
                        return;
                    }

                    svr.stats.messageRecv();

                    //TODO:日志

                    switch (msg.getClass().getName()) {
                        case MessageType.CONNECT:
                            Connect connect = (Connect) msg;
                            if (connected == false ){
                                connected = true;
                            }
                            else {//收到第二个CONNECT报文
                                log.severe("Error: 收到第二个CONNECT报文");
                                return;
                            }
                            int rc = RetCode.RetCodeAccepted;
                            //如果发现不支持的协议级别，服务端必须给发送一个返回码为0x01（不支持的协议
                            //级别）的CONNACK报文响应CONNECT报文，然后断开客户端的连接
                            if (!connect.getProtocolName() .equals("MQTT") ||
                                    connect.getProtocalVersion() != 4) {
                                log.info("reader: reject connection from " + connect.getProtocolName() + " version " + connect.getProtocalVersion());
                                rc = RetCode.RetCodeUnacceptableProtocolVersion;
                            }

                            if (connect.getClientId().length() < 1 || connect.getClientId().length() > 23) {
                                rc = RetCode.RetCodeIdentifierRejected;
                            }
                            clientid = connect.getClientId();
                            if (connect.isCleanSession()){
                                IncomingConn existing = add();
                                if (existing != null) {
                                    Receipt r = existing.submitSync(new Disconnect());
                                    //TODO:超时退出
                                    add();
                                }
                            }
                            else {//CleanSession=1不支持
                                log.info("CONNECT Error: 不支持CleanSession=1");
                                return;
                            }
                            ConnAck connack = new ConnAck();
                            connack.setReturnCode(rc);
                            submit(connack);
                            if (rc != RetCode.RetCodeAccepted) {
                                return;
                            }
                            break;
                        case MessageType.PUBLISH:
                            Publish publish = (Publish) msg;
                            int qos = publish.getHeader().getQosLevel();
                            if(qos == Qos.QosExactlyOnce){
                                System.out.println("reader: no support for QoS " + qos);
                                return;
                            }
                            if (qos != Qos.QosAtMostOnce && publish.getMessageId() == 0){
                                System.out.println("reader: invalid MessageId in PUBLISH.");
                                return;
                            }
                            if (Wild.isWildcard(publish.getTopicName())){
                                System.out.println("reader: ignoring PUBLISH with wildcard topic " + publish.getTopicName());
                                return;
                            }

                            svr.subs.submit(c,publish);

                            if (qos == Qos.QosAtLeastOnce){
                                submit(new PubAck(publish.getMessageId()));
                            }
                            break;
                        case MessageType.DISCONNECT:
                            return;

                        case MessageType.SUBSCRIBE:
                            Subscribe subscribe = (Subscribe) msg;
                            if (subscribe.getHeader().getQosLevel() != Qos.QosAtLeastOnce){
                                continue;
                            }
                            if (subscribe.getMessageId() == 0){
                                System.out.println("reader: invalid MessageId in SUBSCRIBE.");
                                continue;
                            }
                            SubAck subAck = new SubAck(subscribe.getMessageId());;
                            for (TopicQos topicQos : subscribe.getTopics()){
                                svr.subs.add(topicQos.getTopic(),c);
                                subAck.getTopicsQos().add(Qos.QosAtMostOnce);
                            }
                            submit(subAck);
                            for (TopicQos tq : subscribe.getTopics()){
                                svr.subs.sendRetain(tq.getTopic(),c);
                            }
                            break;
                        case MessageType.UNSUBSCRIBE:
                            Unsubscribe unsubscribe = (Unsubscribe) msg;
                            qos = unsubscribe.getHeader().getQosLevel();
                            if (qos != Qos.QosAtMostOnce && unsubscribe.getMessageId() == 0){
                                System.out.println("reader: invalid MessageId in UNSUBSCRIBE.");
                                return;
                            }
                            for (String t : unsubscribe.getTopics()){
                                svr.subs.unsub(t,c);
                            }
                            UnsubAck unsubAck = new UnsubAck(unsubscribe.getMessageId());
                            submit(unsubAck);
                            break;
                        case MessageType.PINGREQ:
                            submit(new PingResp());
                            break;
                        default:
                            break;
                    }
                }

            }
            catch (IOException e){
                e.printStackTrace();
            }
            finally {
                try {
                    conn.close();
                    log.info("连接已关闭: "+conn.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                svr.stats.clientDisconnect();
            }
        }
    }
    IncomingConn add(){
        if (Main.clients.containsKey(clientid)){
            return Main.clients.get(clientid);
        }
        else {
            Main.clients.put(clientid,this);
            return null;
        }
    }
    void del(){
        Main.clients.remove(clientid);
    }
    Receipt submitSync(Message m){
        Job j = new Job(m,new Receipt());
        jobs.add(j);
        return j.r;
    }
    Thread writer(){
        Thread writeThread = new Thread(new WriterRunnable(this),"writeThread");
        writeThread.start();
        return writeThread;
    }
    class WriterRunnable implements Runnable{
        private Logger log = Logger.getLogger(this.getClass().getName());
        IncomingConn c;

        public WriterRunnable(IncomingConn c) {
            this.c = c;
        }

        @Override
        public void run() {
            try {
                while (true){
                    try {
                        Job job = jobs.take();
                        switch (job.m.getClass().getName()){
                            case MessageType.CONNECT:
                                Connect connect = (Connect) job.m;
                                job.m = connect;
                                break;
                            case MessageType.PUBLISH:
                                Publish publish = (Publish) job.m;
                                job.m = publish;
                                break;
                            case MessageType.CONNACK:
                                ConnAck connAck = (ConnAck) job.m;
                                job.m = connAck;
                                break;
                            case MessageType.PUBACK:
                                PubAck pubAck = (PubAck) job.m;
                                job.m = pubAck;
                                break;
                            case MessageType.SUBACK:
                                SubAck subAck = (SubAck) job.m;
                                job.m = subAck;
                                break;
                            case MessageType.UNSUBACK:
                                UnsubAck unsubAck = (UnsubAck) job.m;
                                job.m = unsubAck;
                                break;
                            case MessageType.PINGRESP:
                                PingResp pingResp = (PingResp) job.m;
                                job.m = pingResp;
                                break;
                            default:
                                continue;

                        }

                        if (svr.dupm){
                            //是重发消息
                        }
                        job.m.encode(conn.getOutputStream());
                        svr.stats.messageSend();

                        log.info("已发送报文"+job.m.getClass());
                        //TODO：发的是断开连接报文
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            finally {
                try {
                    conn.close();
                    del();
                    svr.subs.unsubAll(c);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
class Job{
    Message m;
    Receipt r;

    public Job(Message m) {
        this.m = m;
    }

    public Job(Message m, Receipt r) {
        this.m = m;
        this.r = r;
    }
}
//用于做超时退出
class Receipt{


}
class Wild{
    String[] wild;
    IncomingConn c;
    boolean matches(String[] parts){
        int i=0;
        for(;i<parts.length;i++){
            if (i>=wild.length){
                return false;
            }
            if (wild[i]=="#"){
                return true;
            }
            if (parts[i]!=wild[i] && wild[i]!="+"){
                return false;
            }
        }
        if (i==wild.length-1 && wild[wild.length-1] == "#"){
            return true;
        }
        if (i==wild.length){
            return true;
        }
        return false;
    }
    boolean valid(){
        for (int i=0;i<wild.length;i++){
            if (isWildcard(wild[i]) && wild[i].length()!=1){
                return false;
            }
            if (wild[i] == "#" && i != wild[i].length()-1){
                return false;
            }
        }
        return true;
    }
    public static boolean isWildcard(String topic){
        if (topic.contains("#") || topic.contains("+")){
            return true;
        }
        return false;
    }
    public static Wild newWild(String topic, IncomingConn c){
        return new Wild(topic.split("/"),c);
    }
    public Wild(String[] wild, IncomingConn c) {
        this.wild = wild;
        this.c = c;
    }
}
class Retain{
    Publish m;
    Wild wild;

    public Retain(Publish m) {
        this.m = m;
    }
}
class Stats{
    long recv;
    long sent;
    long clients;
    long clientsMax;
    long lastmsgs;
    public void publish(Subscriptions sub, Duration interval){
        synchronized (this){
            if (clients > clientsMax){
                clientsMax = clients;
            }
        }

        //TODO:状态信息



    }
    public synchronized void messageRecv(){
        recv++;
    }
    public synchronized void messageSend(){
        sent++;
    }
    public synchronized void clientConnect(){
        clients++;
    }
    public synchronized void clientDisconnect(){
        clients--;
    }
}
