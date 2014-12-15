package server;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import util.Util;
import bean.User;

/**
 * 
 * @author LD
 *
 */
public class Server {
    private final String DEFAULT_PORT = "8080";
    
    private final int F_WIDTH    = 300;
    private final int F_HEIGHT  = 155;
    private final String MESSAGE_TAG    = "@";
    private final String COMMAND_TAG = "@@";
    private final String ADD_USER           = "ADD_USER";
    private final String ONLINE_USERS = "ONLINE_USERS";
    private final String DELETE_USER    = "DELETE_USER";

    private JButton     btn_start;  
    private JButton     btn_stop;
    private JTextField txt_port;
    private JFrame     f;
    
    //the status is success when the server start! 
    private boolean                   isServerStart;
    private boolean                   isAllowRun;
    private ServerSocket            serverSocket;
    private Thread                      serverThread;
    private List<ClientThread> clients;
    

    public Server() {
        createServerGUI();
    }
    
    /**
     *  main
     * @param args
     */
    public static void main(String[] args) {
        new Server();
    }
    
    /**
     * start server
     * @param port
     * @throws BindException 
     */
    public void startServer(int port) throws BindException, IOException, IllegalArgumentException{
        try {
            serverSocket    = new ServerSocket(port);
            String serverIp = InetAddress.getLocalHost().getHostAddress();
            f.setTitle("服务器地址:" + serverIp);
            
            serverThread   = new Thread(new ServerThread(serverSocket));
            serverThread.start();
            clients = new ArrayList<Server.ClientThread>();
            isServerStart   = true;
            isAllowRun      = true;
        }catch(BindException e){
            isServerStart = false;
            throw new BindException("端口号已被占用......");
        }catch (IOException e) {
            isServerStart = false;
            throw new IOException("启动服务器异常......");
        }catch (IllegalArgumentException e) {
            isServerStart = false;
            throw new IllegalArgumentException("端口号不正确......");
        }
    }
    
    public void stopServer(){
        try {
            if(serverThread != null && serverThread.isAlive()){
                serverThread.interrupt();
            }
            if(serverSocket != null){
                serverSocket.close();
            }
            isServerStart   = false;
            isAllowRun      = false;
        } catch (IOException e) {
            e.printStackTrace();
            isAllowRun = true;
        }
    }
    
    
    /**
     * server Thread
     *
     */
    class ServerThread implements Runnable{
        private ServerSocket serverSocket;
        @Override
        public void run() {
            while(isAllowRun){
                try {
                    Socket s = serverSocket.accept();
                    ClientThread client = new ClientThread(s) ;
                    clients.add(client);
                    new Thread(client).start();
                    sendCommandToAll(ADD_USER + COMMAND_TAG + client.getUser().getName());
                } catch (IOException e) {
                    e.printStackTrace();
                    continue;
                }
            }
        }

        public ServerThread(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }
    }
    
    class ClientThread implements Runnable{
        private Socket                  socket;
        private PrintWriter         writer;
        private BufferedReader  reader;
        private User                     user;
        private boolean             isAllow = true;

        public Socket getSocket() {
            return socket;
        }

        public PrintWriter getWriter() {
            return writer;
        }

        public BufferedReader getReader() {
            return reader;
        }

        public User getUser() {
            return user;
        }

        @Override
        public void run() {
            while(isAllow){
                String message = null;
                try {
                    message = reader.readLine();
                    StringTokenizer st = new StringTokenizer(message, COMMAND_TAG);
                    if(st.nextToken().equals(DELETE_USER)){
                        String name = st.nextToken();
                        for(int i = clients.size() - 1; i >= 0 ; i--){
                            ClientThread client = clients.get(i);
                            if(client.user.getName().equals(name)){
                                client.writer.close();
                                client.reader.close();
                                client.socket.close();
                                client.isAllow = false;
                                clients.remove(client);
                            }else{
                                client.getWriter().println(DELETE_USER + COMMAND_TAG + name);
                                client.getWriter().flush();
                                client.getWriter().println("服务器消息:" + user.getName() + "-" + user.getIp() + "下线了!");
                                client.getWriter().flush();
                            }
                        }
                    }else{
                        dispatcherMessage(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public ClientThread(Socket socket) {
            this.socket = socket;
            try {
                writer  = new PrintWriter(socket.getOutputStream());
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String loginInfo     = reader.readLine();
                StringTokenizer st = new StringTokenizer(loginInfo, MESSAGE_TAG);
                this.user = new User(st.nextToken(), st.nextToken());
                writer.println("服务器消息:" + user.getName() + "-" + user.getIp() + "与服务器连接成功!");
                writer.flush();
                if(clients.size() > 0){
                    StringBuffer online_usernames = new StringBuffer();
                    for(ClientThread c : clients){
                        online_usernames.append(c.user.getName()).append(",");
                        c.getWriter().println("服务器消息:" + user.getName() + "-" + user.getIp() + "上线了!");
                        c.getWriter().flush();
                    }
                    writer.println(ONLINE_USERS + COMMAND_TAG + online_usernames.toString());
                    writer.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        /**
         * dispatcher message
         * @param info
         */
        public void dispatcherMessage(String info){
            StringTokenizer st = new StringTokenizer(info, MESSAGE_TAG);
            String sendUser         = st.nextToken();
            String receiveUsers   = st.nextToken();
            String message          = st.nextToken();
            
            String [] receiveUser = receiveUsers.split(",");
            for(String name: receiveUser){
                if(name.equals(sendUser)){
                    continue;
                }
                for(ClientThread client : clients){
                    if(name.equals(client.getUser().getName())){
                        String sendMessage = sendUser + "对你说:" + message;
                        client.getWriter().println(sendMessage);
                        client.getWriter().flush();
                    }
                }
            }
        }
    }
    
    
    public void sendCommandToAll(String Command){
        for(ClientThread client : clients){
            client.writer.println(Command); 
            client.writer.flush();
        }
    }
    
    
    /**
     * create server Gui
     */
    private void createServerGUI() {
        f = new JFrame("服务器");
        f.setSize(F_WIDTH, F_HEIGHT);

        JPanel jPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 25));

        JLabel lbl_port = new JLabel("端口号:");
        txt_port = new JTextField(DEFAULT_PORT);

        txt_port.setPreferredSize(new Dimension(200, 30));
        btn_start = new JButton("启动");
        btn_stop = new JButton("结束");
        btn_stop.setEnabled(false);

        jPanel.add(lbl_port);
        jPanel.add(txt_port);
        jPanel.add(btn_start);
        jPanel.add(btn_stop);

        f.add(jPanel);
        f.setResizable(false);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
        
        f.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if(isServerStart){
                    stopServer();
                }
                System.exit(0);
            }
        });
        
        btn_start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String port = txt_port.getText().trim();
                if(port.equals("")){
                    Util.showMessage("请输入服务器端口号!", "错误提示", f, JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                if(!Util.checkIsNum(port)){
                    Util.showMessage("服务器端口号只能为数字!", "错误提示", f, JOptionPane.ERROR_MESSAGE);
                    return;
                }
                try {
                    startServer(Integer.parseInt(port));
                } catch (BindException e1) {
                    e1.printStackTrace();
                    Util.showMessage("服务器端口号已被占用!", "错误提示", f, JOptionPane.ERROR_MESSAGE);
                } catch(IOException e1){
                    e1.printStackTrace();
                    Util.showMessage("启动服务器异常!", "错误提示", f, JOptionPane.ERROR_MESSAGE);
                }catch (IllegalArgumentException e1) {
                    e1.printStackTrace();
                    Util.showMessage("请输入正确的端口号!", "错误提示", f, JOptionPane.ERROR_MESSAGE);
                }
                
                if(isServerStart){
                    txt_port.setEditable(false);
                    btn_start.setEnabled(false);
                    btn_stop.setEnabled(true);
                }
            }
        });
        
        btn_stop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(isServerStart){
                    stopServer();
                }
                if(!isServerStart){
                    btn_start.setEnabled(true);
                    txt_port.setEditable(true);
                    btn_stop.setEnabled(false);
                    f.setTitle("服务器");
                }
            }
        });
    }
}