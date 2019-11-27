package sofronio.cn.danmaku;

import android.os.Handler;
import android.os.Message;
import android.os.PatternMatcher;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by Shiromi on 2/6/16.
 * Update to 0.0.4 by Sofronio 2018-06-11
 */
public class DanmakuServer implements Runnable {
    private static final String TAG = "DanmakuServer";
    private static final int protocolVersion = 1;
    private List list = new ArrayList();
    private String chatHost = "broadcastlv.chat.bilibili.com";
    private int chatPort = 2243;

    private String urlCIDInfo = "https://api.live.bilibili.com/room/v1/Room/room_init?id=";

    private boolean connected = false;

    private String roomId = "";

    private Handler handler;

    private Socket socket;
    private OutputStream serverO;
    private InputStream serverI;

    private DataOutputStream dataO;
    private DataInputStream dataI;

    private Thread heartBeat;

    public DanmakuServer(String roomId, Handler handler) {
        this.roomId = roomId;
        this.handler = handler;
    }

    @Override
    public void run() {
        Log.i(TAG, "Run Task!");

        //String chatHost;
        //if ((chatHost = getRoomInfo()) != null) {
            sendMessage(DanmakuResponse.DEBUG, "Connecting:" + chatHost);
            try {
                socket = new Socket(chatHost, chatPort);

                serverI = socket.getInputStream();
                serverO = socket.getOutputStream();

                dataO = new DataOutputStream(serverO);
                dataI = new DataInputStream(serverI);

                if (sendJoinChannel()) {
                    connected = true;
                    Log.i(TAG, "Heart Beat Start!!");
                    heartBeat = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                while (connected) {
                                    sendSocketData(2);
                                    Log.i(TAG, "Heart Beat!");
                                    synchronized (this) {
                                        this.wait(30000);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    heartBeat.start();
                     Log.i(TAG, "Receive Start!");
                     receiveMessageLoop();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        //}
        sendMessage(DanmakuResponse.DISCONNECT, "");
    }

    public void disconnect()
    {
        try {
             connected = false;
             if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getRoomInfo() {
        sendMessage(DanmakuResponse.DEBUG, "GetRoomInfo:" + roomId);

        HttpURLConnection connection = null;
        try {
            URL requestUrl = new URL(urlCIDInfo + roomId);
            sendMessage(DanmakuResponse.DEBUG, "Request:" + urlCIDInfo + roomId);
            connection = (HttpURLConnection) requestUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);

            InputStream in = connection.getInputStream();
            BufferedReader bufReader = new BufferedReader(new InputStreamReader(in));

            String buf;

            Pattern p = Pattern.compile("<server>(.*)</server>");
            Matcher m;
            while ((buf = bufReader.readLine()) != null) {
                sendMessage(DanmakuResponse.DEBUG, "Received:" + buf);
                m = p.matcher(buf);
                if (m.matches()) {
                    //sendMessage(DanmakuResponse.DEBUG, "ServerFound:" + m.group(1));
                    sendMessage(DanmakuResponse.DEBUG, "m ok" + m.group(1));
                    return m.group(1);
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
            /*
            String disconnectString = "https://live.bilibili.com/api/player?id=cid:0";

            if(e.getMessage().toLowerCase().contains(disconnectString.toLowerCase())) {
                sendMessage(DanmakuResponse.DEBUG, "Disconnected!\n");
                danmakuWwitch = false;
            } else {
                */
            sendMessage(DanmakuResponse.DEBUG, "Connect Failed! Error:\n" + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        sendMessage(DanmakuResponse.DEBUG, "null!");
        return null;

    }

    private void receiveMessageLoop()
    {
        try {
            while (connected) {
                // packet length
                int packetLength = dataI.readInt();
                if (packetLength < 16) {
                    throw new Exception("Receive Data Faild");
                }
                // hlen
                short hlen = dataI.readShort();
                // ver
                short ver = dataI.readShort();
                if (ver > 1 || ver < 0) {
                    break;
                }
                // operation
                int operation = dataI.readInt() - 1;
                // seq
                int seq = dataI.readInt();

                // payload length
                int payloadLength = packetLength - 16;
                if (payloadLength == 0) {
                    continue;
                }
                Log.d(TAG, "Operation:" + operation + " Length:" + packetLength + " hlen:" + hlen + " ver:" + ver + " seq:" + seq);
                // parser
                byte[] buffer = new byte[payloadLength];
                switch (operation) {
                    case 0: // viewer count
                    case 1:
                    case 2:
                        int s = dataI.readInt();
                        sendMessage(DanmakuResponse.VIEWER_COUNT, "" + s);
                        break;
                    case 3: // player command
                    case 4:
                        dataI.read(buffer, 0, payloadLength);
                        try {
                            JSONObject res = new JSONObject(new String(buffer));
                            parserCommand(res);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        break;
                    case 5: // scroll message
                        dataI.read(buffer, 0, payloadLength);
                        sendMessage(DanmakuResponse.SCROLL_MESSAGE, new String(buffer));
                        break;
                    default:
                        dataI.read(buffer, 0, payloadLength);
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parserCommand(JSONObject res) {
        Log.d(TAG, res.toString());
        try {
            switch (res.getString("cmd")) {
                case "SEND_GIFT":
                    sendMessage(DanmakuResponse.SEND_GIFT,
                            res.getJSONObject("data").getString("uname") +
                            res.getJSONObject("data").getString("action") + "了" +
                            res.getJSONObject("data").getString("num") + "个" +
                            res.getJSONObject("data").getString("giftName")
                    );
                    break;
                case "DANMU_MSG":
                    sendMessage(DanmakuResponse.DANMU_MSG, res.getJSONArray("info").getJSONArray(2).getString(1) + "说:" + res.getJSONArray("info").getString(1));
                    //list.add(0,res.getJSONArray("info").getJSONArray(2).getString(1));
                    //list.add(0,res.res.getJSONArray("info").getString(1));

                   //sendMessage(DanmakuResponse.DANMU_MSG, res.getJSONArray("info").getString(1));
                    //sendMessage(DanmakuResponse.DANMU_MSG, list);
                    break;
                default:
                    sendMessage(DanmakuResponse.PLAYER_COMMAND, res.toString());
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendSocketData(int operation) throws Exception  {
        sendSocketData(operation, "");
    }

    private void sendSocketData(int operation, String body) throws Exception  {
        sendSocketData(0, (short) 16, (short) protocolVersion, operation, 1, body);
    }

    private void sendSocketData(int packetLength, short hlen, short ver, int operation) throws Exception  {
        sendSocketData(packetLength, hlen, ver, operation, 1);
    }

    private void sendSocketData(int packetLength, short hlen, short ver, int operation, int seq) throws Exception  {
        sendSocketData(packetLength, hlen, ver, operation, seq, "");
    }

    private void sendSocketData(int packetLength, short hlen, short ver, int operation, int seq, String body) throws Exception {
        byte[] payload = body.getBytes("UTF-8");

        if (packetLength == 0) {
            packetLength = payload.length + 16;
        }

        dataO.writeInt(packetLength);
        dataO.writeShort(hlen);
        dataO.writeShort(ver);
        dataO.writeInt(operation);
        dataO.writeInt(seq);
        if (payload.length > 0) {
            dataO.write(payload);
        }

        dataO.flush();
    }

    private boolean sendJoinChannel() {
        try {
            Random rand = new Random();
            long uid = (long) (1e14 + 2e14 * rand.nextDouble());
            JSONObject packet = new JSONObject();
            packet.put("roomid", Integer.parseInt(roomId));
            packet.put("uid", uid);
            Log.i(TAG, "JoinChannel:" + packet.toString());
            sendSocketData(7, packet.toString());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public void sendMessage(int type, String msg) {
        Message message = new Message();
        message.what = type;
        message.obj = msg;

        handler.sendMessage(message);
    }
}
