package sofronio.cn.danmaku;

//import android.annotation.TargetApi;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import static android.speech.tts.TextToSpeech.*;
import static android.util.Log.d;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private int TTS_DATA_CHECK_CODE = 0;
    private int RESULT_TALK_CODE = 1;
    private TextToSpeech myTTS;
    private Context context;
    private Boolean switchDanmaku = true;
    //Thread TDanmaku;
    //private InterruptThread TDanmaku;
    private DanmakuServer dmkServer;
    //声明Sharedpreferenced对象 保存配置文件
    //private DanmakuServer dmkServer;
    private SharedPreferences sp;
    private EditText iptRoomId;
    private Button btnConnect;
    private Button btnDisconnect;
    private Button btnNotice;
    private Button btnMute;
    private Button btnRandom;
    private CheckBox cbRead;
    private CheckBox cbGift;
    private CheckBox cbOverRead;
    private CheckBox cbReconnect;
    private ListView dmkList;
//    private JsonRegex jsonRegex[];

    private Boolean WelcomeFlag = false;

    private List<String> dmkData = new ArrayList<String>();
    private ArrayAdapter<String> adapter;

    @Override
    public void onBackPressed() {//重写的Activity返回

        Intent intent = new Intent();
        intent.setAction("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        startActivity(intent);

    }

    private class JsonRegex {
        private String Regex;
        private String Chinese;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerWidget();
        registerEvent();
        configureTTS();
        Welcome();
        loadSetting();
    }
    private String regex(String str_input){
        String json = loadJson();
        TypeToken typeToken = new TypeToken<List<JsonRegex>>() {};
        Type type = typeToken.getType();
        Gson gson = new Gson();
        List<JsonRegex> jsonRegex = gson.fromJson(json, type);
        for(int i = 0; i < jsonRegex.size(); i++)
        {
            //Log.d("DEBUG", jsonRegex.get(i).Chinese);
            //Log.d("DEBUG", jsonRegex.get(i).Regex);
            String str_pattern = jsonRegex.get(i).Regex;
            String str_replace= jsonRegex.get(i).Chinese;
            //Log.d("DEBUG", str_input);
            str_input = str_input.replaceAll(str_pattern, str_replace);
            //Log.d("DEBUG", str_input);
        }
        return str_input;
    }

    private static int countLines(String str){
        String[] lines = str.split("\r\n|\r|\n");
        return  lines.length;
    }

    private String loadJson() {
        //String fileName = "soso_Google_Hime_Regex.json";

        String json = null;
        try {
            InputStream is = getAssets().open("soso_Google_Hime_Regex.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
            //Log.d("DEBUG", (String)json);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return json;
    }


    private void registerEvent() {
        //注册事件
        btnConnect.setOnClickListener(this);
        btnDisconnect.setOnClickListener(this);
        btnNotice.setOnClickListener(this);
        btnMute.setOnClickListener(this);
        btnRandom.setOnClickListener(this);
        cbReconnect.setOnClickListener(this);
        cbRead.setOnClickListener(this);
        cbGift.setOnClickListener(this);
        cbOverRead.setOnClickListener(this);
    }

    private void registerWidget() {
        //注册界面组件
        iptRoomId = (EditText) findViewById(R.id.iptRoomId);
        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        btnNotice = (Button) findViewById(R.id.btnNotice);
        btnMute = (Button) findViewById(R.id.btnMute);
        btnRandom = (Button) findViewById(R.id.btnRandom);
        cbReconnect = (CheckBox) findViewById(R.id.cbReconnect);
        cbRead = (CheckBox) findViewById(R.id.cbRead);
        cbGift = (CheckBox) findViewById(R.id.cbGift);
        cbOverRead = (CheckBox) findViewById(R.id.cbOverRead);
        dmkList = (ListView) findViewById(R.id.dmkList);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, dmkData);
        dmkList.setAdapter(adapter);
    }

    private void Welcome() {
        if (!WelcomeFlag) {
            dmkData.add("sosoB站手机弹幕姬0.0.6版 by Sofronio");
            //adapter.notifyDataSetChanged();
            dmkData.add("B站直播间20767 BuildDate 2019-11-27");
            //adapter.notifyDataSetChanged();
            dmkData.add("感谢原B站手机弹幕姬作者 by 姪乃浜梢");
            //adapter.notifyDataSetChanged();
            dmkData.add("B站直播间59379");
            adapter.notifyDataSetChanged();
        }
        WelcomeFlag = true;
    }

    public void TTSManager(Context baseContext) {
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
        if (requestCode == TTS_DATA_CHECK_CODE) {
            // The user has the TTS data installed
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                myTTS = new TextToSpeech(this, new OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status == TextToSpeech.SUCCESS) {
                            myTTS.setLanguage(Locale.US);
                            //readyToSpeak = true;
                        } else
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

    private void installTTS() {
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
        UtteranceProgressListener uteranceListener = new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                d("[DEBUG]TTS.onStart ", "started");
            }

            @Override
            public void onDone(String utteranceId) {
                d("[DEBUG]TTSonDone ", "done");
            }

            @Override
            public void onError(String utteranceId) {
                d("[DEBUG]TTS.onError ", "error");
            }
        };
        myTTS.setOnUtteranceProgressListener(uteranceListener);
    }

    private void setNotification() {
        String response = "弹幕测试12345";
        if (myTTS != null && cbRead.isChecked()) {
            //String[] str_temp = speechText.split("说:");
            if (cbOverRead.isChecked()) {
                myTTS.speak(regex(response), QUEUE_FLUSH, null);
            } else {
                myTTS.speak(regex(response), QUEUE_ADD, null);
            }
        }
        //dmkData.add(0, response);
        dmkData.add(response);
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

    private void loadSetting()
    {
        try {
            String value = sp.getString("RoomId", "20767");
            iptRoomId.setText(value);

            value = sp.getString("cbReconnect", "false");
            cbReconnect.setChecked(Boolean.parseBoolean(value));

            value = sp.getString("cbRead", "false");
            cbRead.setChecked(Boolean.parseBoolean(value));

            value = sp.getString("cbGift", "false");
            cbGift.setChecked(Boolean.parseBoolean(value));

            value = sp.getString("cbOverRead", "false");
            cbOverRead.setChecked(Boolean.parseBoolean(value));
        } catch (RuntimeException e) {
            d("load setting error:", String.valueOf(e));
        }
    }


    private void saveSetting(String Name, String Value) {
        sp = getSharedPreferences("User", Context.MODE_PRIVATE);

        //获取到edit对象
        SharedPreferences.Editor edit = sp.edit();
        //通过editor对象写入数据
        edit.putString(Name, Value);
        //提交数据存入到xml文件中
        edit.apply();
    }

    private void btnConnect_onClick() {
        //Toast.makeText(MainActivity.this, "Start!!", Toast.LENGTH_SHORT).show();
        //new Thread(dmkServer).start();
        dmkServer = new DanmakuServer(iptRoomId.getText().toString(), handle);
        Thread TDanmaku = new Thread(dmkServer);
        TDanmaku.start();
        iptRoomId.setEnabled(false);
        btnConnect.setEnabled(false);
        saveSetting("iptRoomId", iptRoomId.getText().toString());
    }

    private void btnDisconnect_onClick() {
        dmkServer.disconnect();
        iptRoomId.setEnabled(true);
        btnConnect.setEnabled(true);
        dmkData.add(getString(R.string.t_disconnect_text));
        adapter.notifyDataSetChanged();
    }

    private void btnTest_onClick() {
        setNotification();
    }

    private void btnMute_onClick() {
        myTTS.stop();
        String value = sp.getString("RoomId", "20767");
        iptRoomId.setText(value);
    }

    private void btnRandom_onClick() {
        java.util.Random r = new java.util.Random();
        String random = String.valueOf(r.nextInt(101));
        if (myTTS != null && cbRead.isChecked()) {
            //String[] str_temp = speechText.split("说:");
            if (cbOverRead.isChecked()) {
                myTTS.speak(random, QUEUE_FLUSH, null);
            } else {
                myTTS.speak(random, QUEUE_ADD, null);
            }
        }
        dmkData.add(random);
        adapter.notifyDataSetChanged();
    }

    private void cbReconnect_checkedChanged() {
        if (cbReconnect.isChecked()) {
            saveSetting("cbReconnect", "true");
        } else {
            saveSetting("cbReconnect", "false");
        }
    }

    private void cbRead_checkedChanged() {
        if (cbRead.isChecked()) {
            saveSetting("cbRead", "true");
        } else {
            saveSetting("cbRead", "false");
        }
    }

    private void cbGift_checkedChanged() {
        if (cbGift.isChecked()) {
            saveSetting("cbGift", "true");
        } else {
            saveSetting("cbGift", "false");
        }
    }

    private void cbOverRead_checkedChanged() {
        if (cbOverRead.isChecked()) {
            saveSetting("cbOverRead", "true");
        } else {
            saveSetting("cbOverRead", "false");
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnConnect:
                btnConnect_onClick();
                break;
            case R.id.btnDisconnect:
                btnDisconnect_onClick();
                break;
            case R.id.btnNotice:
                btnTest_onClick();
                break;
            case R.id.btnMute:
                btnMute_onClick();
                break;
            case R.id.btnRandom:
                btnRandom_onClick();
                break;
            case R.id.cbReconnect:
                cbReconnect_checkedChanged();
                break;
            case R.id.cbRead:
                cbRead_checkedChanged();
                break;
            case R.id.cbGift:
                cbGift_checkedChanged();
                break;
            case R.id.cbOverRead:
                cbOverRead_checkedChanged();
                break;
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler handle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DanmakuResponse.DEBUG:
                    Log.d("DEBUG", (String) msg.obj);
                    //dmkData.add(0, (String) msg.obj);
                    dmkData.add((String) msg.obj);
                    adapter.notifyDataSetChanged();
                    break;
                case DanmakuResponse.VIEWER_COUNT:
                    //dmkData.add(0, "观众人数 " + msg.obj);
                    setTitle( getString(R.string.room) + iptRoomId.getText().toString() + getString(R.string.t_count_text) + msg.obj);
                    adapter.notifyDataSetChanged();
                    break;
                case DanmakuResponse.DANMU_MSG:
                    String response = (String) msg.obj;
                    speechDanmaku(response);
                    dmkData.add( response);
                    adapter.notifyDataSetChanged();
                    break;
                case DanmakuResponse.SEND_GIFT:
                    String response2 = (String) msg.obj;
                    speechGift(response2);
                    dmkData.add(response2);
                    adapter.notifyDataSetChanged();
                    break;
                case DanmakuResponse.PLAYER_COMMAND:
                    break;
                case DanmakuResponse.DISCONNECT:
                    iptRoomId.setEnabled(true);
                    btnConnect.setEnabled(true);
                    if (cbReconnect.isChecked())
                    {
                        Handler mHanlder = new Handler();
                        Runnable runnable=new Runnable(){
                            @Override
                            public void run() {

                                btnConnect.callOnClick();
                                //要做的事情，这里再次调用此Runnable对象，以实现每两秒实现一次的定时器操作
                                //handler.postDelayed(this, 2000);
                            }
                        };
                        mHanlder.postDelayed(runnable,3000);
                        //timer.cancel();
                        //btnConnect.callOnClick();
                        //dmkData.add(0, "重新连接");
                        dmkData.add( "重新连接");
                        d("[soso] test ", "重新连接");
                    }
                    break;
            }
        }
        private void speechGift(String speechText) {
            if (myTTS != null && cbRead.isChecked() && cbGift.isChecked()) {
                //String[] str_temp = speechText.split("说:");
                if (cbOverRead.isChecked()) {
                    myTTS.speak(speechText, QUEUE_FLUSH, null);
                } else {
                    myTTS.speak(speechText, QUEUE_ADD, null);
                }
            }
        }

        private void speechDanmaku(String speechText) {
            if (myTTS != null && cbRead.isChecked()) {
                String[] str_temp = speechText.split("说:");

                if (cbOverRead.isChecked()) {
                    myTTS.speak(regex(str_temp[1]), QUEUE_FLUSH, null);
                } else {
                    myTTS.speak(regex(str_temp[1]), QUEUE_ADD, null);
                }
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

/*
TODO 语速调整
TODO 设置界面
TODO 弹幕姬文字正则匹配
TODO 礼物防火墙
TODO 弹幕发送者颜色
TODO 音量调整
TODO 黑名单
TODO 保存弹幕
TODO 黑色界面
 */
