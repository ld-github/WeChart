package client;

import java.awt.Color;
import java.awt.Cursor;
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
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import util.Util;

/**
 * 
 * @author LD
 *
 */
public class Client {
    private final int F_WIDTH  = 1000;
    private final int F_HEIGHT = 600;
    private final String MESSAGE_TAG    = "@";
    private final String COMMAND_TAG = "@@";
    private final String ADD_USER           = "ADD_USER";
    private final String ONLINE_USERS  = "ONLINE_USERS";
    private final String DELETE_USER     = "DELETE_USER";

    private JFrame            f;
    private JTextField       txt_url;
    private JTextField       txt_port;
    private JTextField       txt_name;
    private JTextField       txt_message;
    private JButton          btn_connect;
    private JButton          btn_break;
    private JButton          btn_send;
    private JList               list_user;
    private JTextArea      txtarea_message;
    private JScrollPane   sp_msglist;
    private DefaultListModel online_users;

    //the status: when the connect to server is success! 
    private boolean        isConnect;
    
    private Socket                  socket;
    private PrintWriter         writer;
    private BufferedReader  reader;
    private MessageThread  messageThread;

    /**
     * mian
     * @param args
     */
    public static void main(String[] args) {
        new Client();
    }

    /**
     * create client Gui
     */
    private void createClientGUI() {
        f = new JFrame("客户端");
        f.setSize(F_WIDTH, F_HEIGHT);
        f.setLayout(null);

        JPanel pl_top = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        pl_top.setSize(new Dimension(994, 65));
        pl_top.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "连接信息"));

        JLabel lbl_url = new JLabel("地址:");
        txt_url = new JTextField("192.168.60.136");
        txt_url.setPreferredSize(new Dimension(150, 28));

        JLabel lbl_port = new JLabel("端口号:");
        txt_port = new JTextField("8080");
        txt_port.setPreferredSize(new Dimension(150, 28));

        JLabel lbl_name = new JLabel("用户名:");
        txt_name = new JTextField();
        txt_name.setPreferredSize(new Dimension(150, 28));

        btn_connect = new JButton("链接");
        btn_break = new JButton("断开");
        btn_break.setEnabled(false);

        pl_top.add(lbl_url);
        pl_top.add(txt_url);
        pl_top.add(lbl_port);
        pl_top.add(txt_port);
        pl_top.add(lbl_name);
        pl_top.add(txt_name);
        pl_top.add(btn_connect);
        pl_top.add(btn_break);

        JPanel pl_left = new JPanel(new FlowLayout());
        pl_left.setSize(new Dimension(200, 430));
        pl_left.setLocation(0, 70);
        pl_left.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "在线用户"));
        online_users = new DefaultListModel();
        list_user = new JList(online_users);
        list_user.setCursor(new Cursor(Cursor.HAND_CURSOR));
        list_user.setSelectionBackground(Color.GRAY);
        list_user.setFixedCellHeight(24);
        list_user.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        JScrollPane sp_userlist = new JScrollPane(list_user);
        sp_userlist.setPreferredSize(new Dimension(185, 385));
        pl_left.add(sp_userlist);

        JPanel pl_right = new JPanel();
        pl_right.setSize(new Dimension(783, 430));
        pl_right.setLocation(210, 70);
        pl_right.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "聊天信息"));
        
        txtarea_message = new JTextArea();
        txtarea_message.setLineWrap(true); 
        txtarea_message.setEditable(false);
        txtarea_message.setForeground(Color.RED);
        
        sp_msglist = new JScrollPane(txtarea_message);
        sp_msglist.setPreferredSize(new Dimension(760, 385));

        pl_right.add(sp_msglist);
        
        JPanel pl_bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pl_bottom.setSize(new Dimension(993,  65));
        pl_bottom.setLocation(0, 505);
        pl_bottom.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), "发送消息"));
        
        txt_message = new JTextField();
        txt_message.setPreferredSize(new Dimension(800, 28));
        txt_message.setEditable(false);
        
        btn_send = new JButton("发送消息");
        btn_send.setPreferredSize(new Dimension(100, 28));
        btn_send.setEnabled(false);
        
        pl_bottom.add(txt_message);
        pl_bottom.add(btn_send);
        
        f.add(pl_top);
        f.add(pl_left);
        f.add(pl_right);
        f.add(pl_bottom);

        f.setResizable(false);
        f.setLocationRelativeTo(null);
        f.setVisible(true);

        f.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                finishReply();
                System.exit(0);
            }
        });

        btn_connect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String ip        = txt_url.getText();
                String port    = txt_port.getText();
                String name  = txt_name.getText();

                if(ip.equals("")){
                    Util.showMessage("请输入服务器地址!", "错误提示", f, JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if(port.equals("")){
                    Util.showMessage("请输入服务器端口号!", "错误提示", f, JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if(!Util.checkIsNum(port)){
                    Util.showMessage("服务器端口号只能为数字!", "错误提示", f, JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if(name.equals("")){
                    Util.showMessage("请输入用户名!", "错误提示", f, JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    connectServer(ip, Integer.parseInt(port), name);
                } catch (NumberFormatException e1) {
                    e1.printStackTrace();
                    Util.showMessage("请输入正确的服务器端口号!", "错误提示", f, JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (UnknownHostException e1) {
                    e1.printStackTrace();
                    Util.showMessage("请输入正确的服务器地址!", "错误提示", f, JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (IOException e1) {
                    e1.printStackTrace();
                    Util.showMessage("服务器链接失败!", "错误提示", f, JOptionPane.ERROR_MESSAGE);
                    return;
                }catch (IllegalArgumentException e1) {
                    e1.printStackTrace();
                    Util.showMessage("请输入正确的端口号!", "错误提示", f, JOptionPane.ERROR_MESSAGE);
                }

                if (isConnect) {
                    btn_connect.setEnabled(false);
                    btn_break.setEnabled(true);
                    txt_url.setEditable(false);
                    txt_port.setEditable(false);
                    txt_name.setEditable(false);
                    txt_message.setEditable(true);
                    btn_send.setEnabled(true);
                    f.setTitle(name);
                }
            }
        });

        btn_break.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                finishReply();
                if (!isConnect) {
                    btn_connect.setEnabled(true);
                    btn_break.setEnabled(false);
                    txt_url.setEditable(true);
                    txt_port.setEditable(true);
                    txt_name.setEditable(true);
                    txt_message.setEditable(false);
                    btn_send.setEnabled(false);
                    f.setTitle("客户端");
                    online_users.removeAllElements();
                }
            }
        });
        
        btn_send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                String message = txt_message.getText();
                if(message.trim().equals("")){
                    Util.showMessage("消息不能为空!", "提示", f, JOptionPane.WARNING_MESSAGE);
                    return;
                }
                
                Object [] users = list_user.getSelectedValues();
                if(users.length == 0){
                    Util.showMessage("请选择发送的人!", "提示", f, JOptionPane.WARNING_MESSAGE);
                    return;
                }

                sendMessage(f.getTitle(), users, message);
                
                txt_message.setText("");
            }
        });
    }

    /**
     * connect server
     * @param ip
     * @param port
     * @param name
     * @return
     * @throws IOException 
     * @throws UnknownHostException 
     */
    public void connectServer(String ip, int port, String name) throws UnknownHostException, 
        IOException, IllegalArgumentException{
            try {
                socket = new Socket(ip, port);
                writer = new PrintWriter(socket.getOutputStream());
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                messageThread = new MessageThread(reader);
                new Thread(messageThread).start();
                String message = name + MESSAGE_TAG + socket.getLocalAddress().toString();
                isConnect = true;
                sendMessage(message);
            } catch (UnknownHostException e) {
                isConnect = false;
                throw new UnknownHostException("找不到正确的主机地址......");
            }catch (IOException e) {
                isConnect = false;
                throw new IOException("请求服务器失败......");
            }catch (IllegalArgumentException e) {
                isConnect = false;
                throw new IOException("端口号不正确......");
            }
    }
    
    public void finishReply(){
        if(isConnect){
            this.writer.println(DELETE_USER + COMMAND_TAG + f.getTitle());
            this.writer.flush();
            isConnect = false;
            messageThread.isAllow = false;
        }
    }
    
    class MessageThread implements Runnable{

        private BufferedReader  reader;
        private boolean             isAllow = true;
        
        public MessageThread(BufferedReader reader) {
            this.reader = reader;
        }

        @Override
        public void run() {
            String message = null;
            while(isAllow){
                try {
                    message = reader.readLine();
                    StringTokenizer st = new StringTokenizer(
                            message, COMMAND_TAG);
                    String command = st.nextToken();
                    if(command.equals(ADD_USER)){
                        addElementToList(st.nextToken());
                    }else if(command.equals(ONLINE_USERS)){
                        String [] names = st.nextToken().split(",");
                        for(String name : names){
                            addElementToList(name);
                        }
                    }else if(command.equals(DELETE_USER)){
                        String name = st.nextToken();
                        removeElementToList(name);
                    }else {
                        showMessage(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    
    public Client() {
        createClientGUI();
    }
    
    /**
     * send message
     * @param message
     */
    private void sendMessage(String message){
        if(!isConnect){
            Util.showMessage("请重新连接服务器!", "提示", f, JOptionPane.WARNING_MESSAGE);
            return;
        }
        writer.println(message);
        writer.flush();
    }
    
    public void sendMessage(String sendUser,Object [] receiveUsers, String message){
        StringBuffer strReceiveUsers = new StringBuffer();
        for(Object u : receiveUsers){
            strReceiveUsers.append(u).append(",");
        }
        String sendMessage = sendUser + MESSAGE_TAG + strReceiveUsers.toString() + MESSAGE_TAG + message;
        sendMessage(sendMessage);
        showMessage("你对" + strReceiveUsers + "说:" + message);
    }
    
    public void showMessage(String message){
        txtarea_message.append(message + "\n\n");
        txtarea_message.selectAll();
    }
    
    public void addElementToList(String name){
        online_users.addElement(name);
    }
    
    public void removeElementToList(String name){
        online_users.removeElement(name);
    }
}
