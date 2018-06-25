package sofronio.cn.danmaku;

//import android.annotation.TargetApi;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static android.speech.tts.TextToSpeech.*;
import static android.util.Log.d;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    // TTS Stuff
    private UtteranceProgressListener uteranceListener;
    private int TTS_DATA_CHECK_CODE = 0;
    private int RESULT_TALK_CODE = 1;
    private TextToSpeech myTTS;
    private Context context;

    //Danmaku Stuff
    private EditText iptRoomId;
    private Button btnConnect;
    private Button btnNotice;
    private ListView dmkList;

    private CheckBox cbReconnect;
    private boolean flag_redirect;

    private Boolean WelcomeFlag = false;

    private List<String> dmkData = new ArrayList<String>();
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerWidget();
        registerEvent();
        configureTTS();
        Welcome();
    }

    private void Welcome()
    {
        if (WelcomeFlag == false) {
            dmkData.add(0, "手机谷歌娘0.0.4版 作者Sofronio B站直播间20767 BuildDate 2016-09-02");
            adapter.notifyDataSetChanged();
            dmkData.add(0, "感谢B站手机弹幕姬作者姪乃浜梢老板 B站直播间59379");
            adapter.notifyDataSetChanged();
        }
        WelcomeFlag = true;
    }

    public void TTSManager(Context baseContext)
    {
        this.context = baseContext;
    }
    private void configureTTS() {
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, TTS_DATA_CHECK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        d("[DEBUG] doc.saulmm.text2spech.MainActivity.onActivityResult ", "Request_code: " + requestCode);

        if (requestCode == TTS_DATA_CHECK_CODE) {
            // The user has the TTS data installed
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                myTTS = new TextToSpeech(this, new OnInitListener()
                {
                    @Override
                    public void onInit(int status)
                    {
                        if (status == TextToSpeech.SUCCESS)
                        {
                            myTTS.setLanguage(Locale.US);
                            //readyToSpeak = true;
                        }
                        else
                            installTTS();
                    }
                });
                configureTTSCalbacks();
                // The app will prompt the user to install TTS data
            } else {
                disableUI();
                Intent installTTSIntent = new Intent();
                installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installTTSIntent);
            }
        } else if (requestCode == RESULT_TALK_CODE && data != null) {
            ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            //showResultDialog(text.get(0));
        }
    }

    private void installTTS()
    {
        Intent installIntent = new Intent();
        installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
        context.startActivity(installIntent);
    }

    private void disableUI() {
        /*
        speechEdit.setText("Android text to speech is not working :(");
        speechEdit.setEnabled(false);
        speechButton.setEnabled(false);*/
    }

    private void configureTTSCalbacks() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            uteranceListener = new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    d("[DEBUG] doc.saaulmm.text2spech.MainActivity.onStart ", "started");
                }
                @Override
                public void onDone(String utteranceId) {
                    d("[DEBUG] doc.saulmm.text2spech.MainActivity.onDone ", "done");
                }
                @Override
                public void onError(String utteranceId) {
                    d("[DEBUG] doc.saulmm.text2spech.MainActivity.onError ", "error");
                }
            };

            myTTS.setOnUtteranceProgressListener(uteranceListener);
        }
    }

    private void registerEvent() {
        //注册事件
        btnConnect.setOnClickListener(this);
        btnNotice.setOnClickListener(this);
        cbReconnect.setOnClickListener(this);
    }

    private void registerWidget()
    {
        //注册界面组件
        iptRoomId = (EditText) findViewById(R.id.iptRoomId);
        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnNotice = (Button) findViewById(R.id.btnNotice);
        cbReconnect = (CheckBox) findViewById(R.id.cbReconnect);
        dmkList = (ListView) findViewById(R.id.dmkList);

        adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, dmkData);
        dmkList.setAdapter(adapter);
    }
    private void setNotification() {
        String response = "这是一个测试1234567890";
        if (myTTS != null) {
            //String[] str_temp = speechText.split("说:");
            myTTS.speak(response,
                    QUEUE_FLUSH, //
                    null);
        }
        dmkData.add(0, response);
        adapter.notifyDataSetChanged();
        /*
        //添加通知栏
        dmkData.add(0, "系统提示：已经驻留通知栏");
        NotificationCompat.Builder mBuilder =
                (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("My notification")
                        .setContentText("Hello World!");
        Intent resultIntent = new Intent(this, MainActivity.class);
        int mNotificationId = 001;
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // Builds the notification and issues it.
        mNotifyMgr.notify(mNotificationId, mBuilder.build());
        */
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnConnect:
                Toast.makeText(MainActivity.this, "Start!!", Toast.LENGTH_SHORT).show();
                DanmakuServer dmkServer = new DanmakuServer(iptRoomId.getText().toString(), handle);
                new Thread(dmkServer).start();
                iptRoomId.setEnabled(false);
                btnConnect.setEnabled(false);
                break;
            case R.id.btnNotice:
                setNotification();
                break;
            case R.id.cbReconnect:
                d("[soso] test ", "点了一下cbReconnect");
                if (cbReconnect.isChecked() == false)
                {
                    //看似状态是反的，但这是点击之后进行的判断，是上一状态
                    flag_redirect = false;
                    d("[soso] test ", "flag_redirect设置为假了");
                    //dmkData.add(0, "flag_redirect设置为假了");
                    //cbReconnect.setChecked(false);
                }
                else
                {
                    flag_redirect = true;
                    d("[soso] test ", "flag_redirect设置为真了");
                    //dmkData.add(0, "flag_redirect设置为真了");
                    //cbReconnect.setChecked(true);
                }

        }
    }




    private Handler handle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DanmakuResponse.DEBUG:
                    Log.d("DEBUG", (String) msg.obj);
                    dmkData.add(0, (String) msg.obj);
                    adapter.notifyDataSetChanged();
                    break;
                case DanmakuResponse.VIEWER_COUNT:
                    //dmkData.add(0, "观众人数 " + msg.obj);
                    setTitle("soso谷歌娘·观众人数 " + msg.obj);
                    adapter.notifyDataSetChanged();
                    break;
                case DanmakuResponse.DANMU_MSG:
                    String response = (String) msg.obj;
                    speechDanmaku(response);
                    dmkData.add(0, response);
                    adapter.notifyDataSetChanged();
                    break;
                case DanmakuResponse.SEND_GIFT:
                    String response2 = (String) msg.obj;
                    speechGift(response2);
                    dmkData.add(0, response2);
                    adapter.notifyDataSetChanged();
                    break;
                case DanmakuResponse.PLAYER_COMMAND:
                    break;
                case DanmakuResponse.DISCONNECT:
                    iptRoomId.setEnabled(true);
                    btnConnect.setEnabled(true);
                    if (flag_redirect == true)
                    {
                        Handler mHanlder = new Handler();
                        Runnable runnable=new Runnable(){
                            @Override
                            public void run() {
                                // TODO Auto-generated method stub
                                btnConnect.callOnClick();
                                //要做的事情，这里再次调用此Runnable对象，以实现每两秒实现一次的定时器操作
                                //handler.postDelayed(this, 2000);
                            }
                        };
                        mHanlder.postDelayed(runnable,3000);
                        //timer.cancel();
                        //btnConnect.callOnClick();
                        dmkData.add(0, "重新连接");
                        d("[soso] test ", "重新连接");
                    }
                    break;
            }
        }
        private void speechGift(String speechText) {
            if (myTTS != null) {
                myTTS.speak(speechText,
                        QUEUE_FLUSH, //
                        null);
            }
        }
        private void speechDanmaku(String speechText) {
            if (myTTS != null) {
                String[] str_temp = speechText.split("说:");
                myTTS.speak(str_temp[1],
                        QUEUE_FLUSH, //
                        null);
            }
        }

        public void onInit(int status) {

            switch (status) {
                case SUCCESS:
                    configureTTSLocale();
                    break;
            }
        }


        private void configureTTSLocale() {
        /*
		Locale deviceLocale = Locale.getDefault();

		if(myTTS.isLanguageAvailable(deviceLocale) == TextToSpeech.LANG_AVAILABLE)
			myTTS.setLanguage(deviceLocale);
		*/
        }
    };
}