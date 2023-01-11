package cn.serendipityr.PSFMS.Utils;

import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;

public class ServicesChecker {
    public static Integer checkWebsite(InetSocketAddress inetSocketAddress) {
        if (!ConfigUtil.CheckOtherServices) {
            return null;
        }

        try {
            URL url = new URL("http://" + ConfigUtil.ScanAddress + ":" + inetSocketAddress.getPort());
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(ConfigUtil.ConnectTimeout);
            connection.setReadTimeout(ConfigUtil.ReadTimeout);
            return connection.getResponseCode();
        } catch (Exception e) {
            return null;
        }
    }
}
