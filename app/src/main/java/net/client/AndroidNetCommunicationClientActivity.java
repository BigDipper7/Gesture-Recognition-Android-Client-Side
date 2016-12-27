package net.client;

import eneter.messaging.diagnostic.EneterTrace;
import eneter.messaging.endpoints.typedmessages.*;
import eneter.messaging.messagingsystems.messagingsystembase.*;
import eneter.messaging.messagingsystems.tcpmessagingsystem.TcpMessagingSystemFactory;
import eneter.net.system.EventHandler;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;

public class AndroidNetCommunicationClientActivity extends Activity {
    private static final String TAG = AndroidNetCommunicationClientActivity.class.getSimpleName();

    // Request message type
    // The message must have the same name as declared in the service.
    // Also, if the message is the inner class, then it must be static.
    public static class MyRequest {
        public String Text;
    }

    // Response message type
    // The message must have the same name as declared in the service.
    // Also, if the message is the inner class, then it must be static.
    public static class MyResponse {
        public int Length;
    }

    // UI controls
    private Handler myRefresh = new Handler();
    private EditText myMessageTextEditText;
    private EditText myResponseEditText;
    private Button mySendRequestBtn;
    private Button mBtnEstablish;
    private Button mBtnOpen;
    private Button mBtnClose;


    // Sender sending MyRequest and as a response receiving MyResponse.
    private IDuplexTypedMessageSender<MyResponse, MyRequest> mySender;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Get UI widgets.
        myMessageTextEditText = (EditText) findViewById(R.id.messageTextEditText);
        myResponseEditText = (EditText) findViewById(R.id.messageLengthEditText);
        mySendRequestBtn = (Button) findViewById(R.id.sendRequestBtn);
        mBtnEstablish = (Button) findViewById(R.id.btnEst);
        mBtnOpen = (Button) findViewById(R.id.btnOpen);
        mBtnClose = (Button) findViewById(R.id.btnClose);

        // Subscribe to handle the button click.
        mySendRequestBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG,"Click");
                onSendRequest(view);
                Log.d(TAG, "After Click");
            }
        });

        mBtnOpen.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG,"Click");
                myMessageTextEditText.setText("Open");
                onSendRequest(view);
                Log.d(TAG, "After Click");

            }
        });

        mBtnClose.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG,"Click");
                myMessageTextEditText.setText("Close");
                onSendRequest(view);
                Log.d(TAG, "After Click");

            }
        });

        mBtnEstablish.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                // Open the connection in another thread.
                // Note: From Android 3.1 (Honeycomb) or higher
                //       it is not possible to open TCP connection
                //       from the main thread.
                Thread anOpenConnectionThread = new Thread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try {
                            openConnection();
                            Log.d(TAG, "Open end");
                        }
                        catch (Exception err) {
                            EneterTrace.error("Open connection failed.", err);
                        }
                    }
                });
                anOpenConnectionThread.start();
            }
        });
//        // Open the connection in another thread.
//        // Note: From Android 3.1 (Honeycomb) or higher
//        //       it is not possible to open TCP connection
//        //       from the main thread.
//        Thread anOpenConnectionThread = new Thread(new Runnable()
//            {
//                @Override
//                public void run()
//                {
//                    try
//                    {
//                        openConnection();
//                    }
//                    catch (Exception err)
//                    {
//                        EneterTrace.error("Open connection failed.", err);
//                    }
//                }
//            });
//        anOpenConnectionThread.start();
    }

    @Override
    public void onDestroy() {
        if (mySender != null)
            // Stop listening to response messages.
            mySender.detachDuplexOutputChannel();

        super.onDestroy();
    }

    private void openConnection() throws Exception {
        // Create sender sending MyRequest and as a response receiving MyResponse
        IDuplexTypedMessagesFactory aSenderFactory = new DuplexTypedMessagesFactory();
        mySender = aSenderFactory.createDuplexTypedMessageSender(MyResponse.class, MyRequest.class);

        // Subscribe to receive response messages.
        mySender.responseReceived().subscribe(myOnResponseHandler);

        // Create TCP messaging for the communication.
        // Note: 10.0.2.2 is a special alias to the loopback (127.0.0.1)
        //       on the development machine.
        IMessagingSystemFactory aMessaging = new TcpMessagingSystemFactory();
        IDuplexOutputChannel anOutputChannel
            = aMessaging.createDuplexOutputChannel("tcp://192.168.43.167:8067/");
            //= aMessaging.createDuplexOutputChannel("tcp://192.168.173.1:8060/");
        Log.d(TAG, " binding success -------- ");
        // Attach the output channel to the sender and be able to send
        // messages and receive responses.
        mySender.attachDuplexOutputChannel(anOutputChannel);
        Log.d(TAG,"Attach.....");
    }

    /**
     * sent request if possible
     * @param v
     */
    private void onSendRequest(View v) {
        // Create the request message.
        final MyRequest aRequestMsg = new MyRequest();
        aRequestMsg.Text = myMessageTextEditText.getText().toString();

        Log.i(TAG, "My Request: "+aRequestMsg.Text);

        // Send the request message.
        try {
            mySender.sendRequestMessage(aRequestMsg);
            Log.i(TAG, "Msg sent!");
        }
        catch (Exception err) {
            EneterTrace.error("Sending the message failed.", err);
            Log.e(TAG, "Sent Fail");
        }

    }

    /**
     * get response
     * @param sender
     * @param e
     */
    private void onResponseReceived(Object sender,
                                    final TypedResponseReceivedEventArgs<MyResponse> e) {
        // Display the result - returned number of characters.
        // Note: Marshal displaying to the correct UI thread.
        myRefresh.post(new Runnable()
            {
                @Override
                public void run()
                {
                    myResponseEditText.setText(String.format("this is %d", e.getResponseMessage().Length));
                    Log.i(TAG, e.getResponseMessage().Length+"");
                }
            });
    }

    private EventHandler<TypedResponseReceivedEventArgs<MyResponse>> myOnResponseHandler =
            new EventHandler<TypedResponseReceivedEventArgs<MyResponse>>() {
        @Override
        public void onEvent(Object sender, TypedResponseReceivedEventArgs<MyResponse> e) {
            onResponseReceived(sender, e);
            Log.i(TAG, "On Received");
        }
    };

    /**
     * send Click Listener
     */
    private OnClickListener myOnSendRequestClickHandler = new OnClickListener() {
        @Override
        public void onClick(View v) {
            Log.i(TAG, "Clicked");
            onSendRequest(v);
        }
    };
}