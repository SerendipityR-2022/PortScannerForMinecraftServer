package cn.serendipityr.PSFMS;

import cn.serendipityr.PSFMS.Utils.ConfigUtil;
import cn.serendipityr.PSFMS.Utils.SetTitle;
import cn.serendipityr.PSFMS.Utils.TCPChecker;

public class PortScannerForMinecraftServer {
    public static String ver = "1.2.1";
    public static Integer CfgVer = 2;
    public static Boolean isLinux = false;

    public static void main(String[] args) {
        System.out.println("=========================-Made by SerendipityR-=========================");
        System.out.println(" 针对大型群组服务器的子服端口扫描工具~");
        System.out.println(" PortScannerForMS (Ver: " + ver + ")" + " is loading......");
        System.out.println("========================================================================");
        try {
            SetTitle.INSTANCE.SetConsoleTitleA("PortScannerForMS - Made by SerendipityR");
        } catch (Throwable e) {
            isLinux = true;
        }

        start();
    }

    public static void start() {
        new ConfigUtil().loadConfig();
        new TCPChecker().doPortScan(ConfigUtil.ScanAddress);
    }

    public static void exit() {
        System.exit(0);
    }
 }
