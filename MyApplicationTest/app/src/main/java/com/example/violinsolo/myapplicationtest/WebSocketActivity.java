package com.example.violinsolo.myapplicationtest;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class WebSocketActivity extends AppCompatActivity {
    private final String TAG = WebSocketActivity.class.getSimpleName();
    private Context mContext = WebSocketActivity.this;
    private WebSocketClient webSocketClient;
    private final String address = "ws://192.168.12.36:8090/";
    private EditText etMsgSentContent;
    private Button btnConnect;
    private Button btnDisconnect;
    private Button btnSend;
    private Button btnOpen;
    private Button btnClose;
    private TextView tvMsgReceived;

    private Handler mRefreshHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    Toast.makeText(mContext, (String) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
                case 1:
                    tvMsgReceived.setText((CharSequence) msg.obj);
                    break;
                default:
                    break;
            }
        }
    };

    private void showInfo(String msg) {
        Message message = new Message();
        message.what = 0;
        message.obj = msg;
        mRefreshHandler.sendMessage(message);
    }

    private void showETInfo(String msg) {
        Message message = new Message();
        message.what = 1;
        message.obj = msg;
        mRefreshHandler.sendMessage(message);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_socket);

        init();

    }

    private void init() {
        //UI init
        btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnSend = (Button) findViewById(R.id.btnSendMsg);
        btnOpen = (Button) findViewById(R.id.btnOpen);
        btnClose = (Button) findViewById(R.id.btnClose);
        etMsgSentContent = (EditText) findViewById(R.id.etMsgContent);
        tvMsgReceived = (TextView) findViewById(R.id.tvMsgReceived);

        //Listener
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showETInfo("Connecting ...");
                connect();
                //showInfo("connected!");//not really connected... in another thread
            }
        });
        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showETInfo("Disconnected ...");
                closeConnect();
                showInfo("connection closed!");
            }
        });
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String content = etMsgSentContent.getText().toString();
                if(content.isEmpty())
                    showInfo("No msg");
                else{
                    sendMsg(content);
                    showInfo("Send Success! "+content);
                }
            }
        });
        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMsg("Open");
                Log.i(TAG, "Send Open MSG to server");
            }
        });
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMsg("Close");
                Log.i(TAG, "Send Close MSG to server");
            }
        });


        try {
            initWebSocketClient(address);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
//        final String address = "";
//        webSocketClient = new WebSocketClient(new URI(address)) {
//
//            @Override
//            public void onOpen(ServerHandshake serverHandshake) {
//                showInfo("opened connection");
//            }
//
//            @Override
//            public void onMessage(String s) {
//                Log.i(TAG, "onMsg: {- " + s + " -}");
//                showInfo("received:{- " + s + " -}");
//
//                Message msg = new Message();
//                msg.what = 1;
//                msg.obj = s.isEmpty()?"EMPTY!":s;
//                mRefreshHandler.sendMessage(msg);
//
//            }
//
//            @Override
//            public void onClose(int i, String s, boolean remote) {
//                //连接断开，remote判定是客户端断开还是服务端断开
//                showInfo("Connection closed by " + ( remote ? "remote peer" : "us" ) + ", info=" + s);
//                //
//                closeConnect();
//            }
//
//            @Override
//            public void onError(Exception e) {
//                showInfo("error:" + e);
//            }
//        };
    }

    private void initWebSocketClient(String addr) throws URISyntaxException {
        webSocketClient = new WebSocketClient(new URI(addr)) {

            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                showInfo("opened connection");
                showETInfo("Connection established ...");
            }

            @Override
            public void onMessage(String s) {
                Log.i(TAG, "onMsg: {- " + s + " -}");
                showInfo("received:{- " + s + " -}");

                Message msg = new Message();
                msg.what = 1;
                msg.obj = s.isEmpty()?"EMPTY!":s;
                mRefreshHandler.sendMessage(msg);

            }

            @Override
            public void onClose(int i, String s, boolean remote) {
                //连接断开，remote判定是客户端断开还是服务端断开
                showInfo("Connection closed by " + ( remote ? "remote peer" : "us" ) + ", info=" + s);
                //
                closeConnect();
                showETInfo("Connection lost ...");
            }

            @Override
            public void onError(Exception e) {
                showInfo("error:" + e);
                showETInfo("Error occurs: "+e.getMessage());
            }
        };
    }


    //连接
    private void connect() {
        new Thread(){
            @Override
            public void run() {
                String addr = etMsgSentContent.getText().toString();

                if(!addr.isEmpty()) {
                    try {
                        initWebSocketClient(addr);
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                        showETInfo("Error while init connection ...");
                    }
                }

                webSocketClient.connect();
            }
        }.start();
    }


    //断开连接
    private void closeConnect() {
        try {
            webSocketClient.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        finally {
            webSocketClient = null;
        }
    }


    //发送消息
    /**
     *
     * @param msg
     */
    private void sendMsg(String msg) {
        webSocketClient.send(msg);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeConnect();
    }
}
