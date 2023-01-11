package cn.serendipityr.PSFMS.Utils;

import com.alibaba.fastjson.JSONObject;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MSChecker {
    public static JSONObject checkMinecraftServer(InetSocketAddress inetSocketAddress) {
        if (!ConfigUtil.CheckMinecraftServer) {
            return null;
        }

        try (Socket socket = new Socket()) {
            socket.connect(inetSocketAddress, ConfigUtil.ConnectTimeout);
            socket.setSoTimeout(ConfigUtil.ReadTimeout);

            // 创建输入流
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

            // 创建输出流
            DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            DataOutputStream handshakeOutputStream = new DataOutputStream(byteArrayOutputStream);

            // 握手开始
            handshakeOutputStream.writeByte(0x00);
            writeVarInt(handshakeOutputStream, 47);
            writeVarInt(handshakeOutputStream, ConfigUtil.ScanAddress.length());
            handshakeOutputStream.writeBytes(ConfigUtil.ScanAddress);
            handshakeOutputStream.writeShort(inetSocketAddress.getPort());
            writeVarInt(handshakeOutputStream, 1);

            // 写入数据
            writeVarInt(dataOutputStream, byteArrayOutputStream.size());
            dataOutputStream.write(byteArrayOutputStream.toByteArray());
            dataOutputStream.writeByte(0x01);
            dataOutputStream.writeByte(0x00);

            String result = readFromInputStream(dataInputStream);
            dataOutputStream.flush();
            return JSONObject.parseObject(result);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getMinecraftServerInfo(JSONObject minecraftServerInfo) {
        StringBuilder info = new StringBuilder();

        Integer onlinePlayers = minecraftServerInfo.getJSONObject("players").getInteger("online");
        Integer maxPlayers = minecraftServerInfo.getJSONObject("players").getInteger("max");
        String version = minecraftServerInfo.getJSONObject("version").getString("name");
        Integer protocolVersion = minecraftServerInfo.getJSONObject("version").getInteger("protocol");
        String motd = "";
        if (minecraftServerInfo.get("description") instanceof JSONObject) {
            motd = minecraftServerInfo.getJSONObject("description").toString();
        } else {
            motd = minecraftServerInfo.getString("description");
        }
        info.append("玩家: ").append(onlinePlayers).append("/").append(maxPlayers).append(" | ");
        if (minecraftServerInfo.getJSONObject("players").containsKey("sample")) {
            info.append("样例: ").append(minecraftServerInfo.getJSONObject("players").getJSONArray("sample").toString()).append(" | ");
        }
        info.append("版本: ").append(version).append(" (").append(protocolVersion).append(")").append(" | ");
        info.append("MOTD: ").append(motd);

        return info.toString();
    }

    public static String readFromInputStream(DataInputStream dataInputStream) throws IOException {
        readVarInt(dataInputStream);
        readVarInt(dataInputStream);

        int len = readVarInt(dataInputStream);
        byte[] bytes = new byte[len];
        dataInputStream.readFully(bytes);

        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static int readVarInt(DataInputStream in) throws IOException {
        int a = 0;
        int b = 0;
        while (true) {
            int c = in.readByte();

            a |= (c & 0x7F) << b++ * 7;

            if (b > 5)
                throw new RuntimeException("VarInt too big");

            if ((c & 0x80) != 128)
                break;
        }
        return a;
    }

    public static void writeVarInt(DataOutputStream out, int paramInt) throws IOException {
        while (true) {
            if ((paramInt & 0xFFFFFF80) == 0) {
                out.writeByte(paramInt);
                return;
            }

            out.writeByte(paramInt & 0x7F | 0x80);
            paramInt >>>= 7;
        }
    }
}
