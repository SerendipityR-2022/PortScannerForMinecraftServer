package cn.serendipityr.PSFMS.Utils;

import cc.summermc.bukkitYaml.InvalidConfigurationException;
import cc.summermc.bukkitYaml.file.YamlConfiguration;
import cn.serendipityr.PSFMS.PortScannerForMinecraftServer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

public class ConfigUtil {
    public static File configFile;
    public static YamlConfiguration config;
    public static Integer CfgVer;
    public static String ScanHostAddress;
    public static String ScanAddress;
    public static Long ScanDelay;
    public static Integer ScanThreads;
    public static File OutputFile;
    public static Integer ConnectTimeout;
    public static Integer ReadTimeout;
    public static Integer MinPort;
    public static Integer MaxPort;
    public static Boolean ShowFails;
    public static Boolean CheckMinecraftServer;
    public static Boolean CheckOtherServices;

    public void loadConfig() {
        try {
            configFile = new File("config.yml");

            if (!configFile.exists()) {
                LogUtil.doLog(1, "载入配置文件失败! 文件不存在。", null);
                PortScannerForMinecraftServer.exit();
            }

            config = YamlConfiguration.loadConfiguration(configFile);

            if (config.getKeys(true).size() == 0) {
                throw new InvalidConfigurationException();
            }

            CfgVer = config.getInt("CfgVer");
            ScanDelay = config.getLong("PortScannerSettings.ScanDelay");
            ScanThreads = config.getInt("PortScannerSettings.ScanThreads");
            ConnectTimeout = config.getInt("PortScannerSettings.ConnectTimeout");
            ReadTimeout = config.getInt("PortScannerSettings.ReadTimeout");
            MinPort = config.getInt("PortScannerSettings.MinPort");
            MaxPort = config.getInt("PortScannerSettings.MaxPort");
            ShowFails = config.getBoolean("PortScannerSettings.ShowFails");
            CheckMinecraftServer = config.getBoolean("PortScannerSettings.CheckMinecraftServer");
            CheckOtherServices = config.getBoolean("PortScannerSettings.CheckOtherServices");

            // 用户交互式输入
            Scanner scanner = new Scanner(System.in);
            LogUtil.doLog(-1, "请输入扫描地址: ", "CFGUtil");
            try {
                ScanHostAddress = scanner.nextLine();
                ScanAddress = toIPAddress(ScanHostAddress);
            } catch (Exception ignored) {}
            if (ScanAddress == null) {
                LogUtil.emptyLog();
                LogUtil.doLog(1, "输入的地址有误，请重试。", "CFGUtil");
                loadConfig();
            }

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String fileName = config.getString("PortScannerSettings.OutputFile");
            OutputFile = new File(fileName.replaceAll("%address%", ScanHostAddress).replaceAll("%time%", simpleDateFormat.format(new Date())));

            LogUtil.emptyLog();
            LogUtil.doLog(0, "==============================================================", "CFGUtil");
            LogUtil.doLog(0, "扫描地址: " + ScanHostAddress + " (" + ScanAddress + ")", "CFGUtil");
            LogUtil.doLog(0, "扫描范围: " + MinPort + "-" + MaxPort, "CFGUtil");
            LogUtil.doLog(0, "扫描线程: " + ScanThreads, "CFGUtil");
            LogUtil.doLog(0, "扫描间隔: " + ScanDelay + " 毫秒", "CFGUtil");
            LogUtil.doLog(0, "超时时间: " + ConnectTimeout + " | " + ReadTimeout + " 毫秒", "CFGUtil");
            LogUtil.doLog(0, "输出结果: " + OutputFile.getName(), "CFGUtil");
            LogUtil.doLog(0, "==============================================================", "CFGUtil");
            LogUtil.emptyLog();
        } catch (Exception e) {
            LogUtil.emptyLog();
            LogUtil.doLog(1, "载入配置文件失败! 详细信息: " + e, null);
            LogUtil.doLog(-1, "配置可能存在编码问题，是否尝试转换编码以解决问题？ [y/n]:", "CFGUtil");
            Scanner scanner = new Scanner(System.in);
            if (scanner.nextLine().contains("y")) {
                String currentCharset = getFileCharset(configFile);

                File tempConfigFile = new File("config_temp.yml");

                switch (currentCharset) {
                    case "GBK":
                        convertFileCharset(configFile, tempConfigFile, currentCharset, "UTF-8");
                        break;
                    case "UTF-8":
                    default:
                        convertFileCharset(configFile, tempConfigFile, currentCharset, "GBK");
                        break;
                }

                if (configFile.delete()) {
                    tempConfigFile.renameTo(configFile);
                }

                LogUtil.doLog(0, "任务完成。转换前编码: " + currentCharset + " | 转换后编码: " + getFileCharset(configFile) , "CFGUtil");
                LogUtil.emptyLog();
            }

            loadConfig();
        }

        if (!PortScannerForMinecraftServer.CfgVer.equals(CfgVer)) {
            LogUtil.doLog(1, "载入配置文件失败! 配置文件版本不匹配，请前往发布页更新配置文件。", null);
            PortScannerForMinecraftServer.exit();
        }
    }

    public static String getFileCharset(File file) {
        String charset = "GBK";

        byte[] first3Bytes = new byte[3];

        try {
            boolean checked = false;
            BufferedInputStream bis = new BufferedInputStream(Files.newInputStream(file.toPath()));
            bis.mark(100);

            int read = bis.read(first3Bytes, 0, 3);

            if (read == -1) {
                bis.close();
                return charset; // 文件编码为 ANSI
            } else if (first3Bytes[0] == (byte) 0xFF && first3Bytes[1] == (byte) 0xFE) {
                charset = "UTF-16LE"; // 文件编码为 Unicode
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xFE && first3Bytes[1] == (byte) 0xFF) {
                charset = "UTF-16BE"; // 文件编码为 Unicode big endian
                checked = true;
            } else if (first3Bytes[0] == (byte) 0xEF && first3Bytes[1] == (byte) 0xBB
                    && first3Bytes[2] == (byte) 0xBF) {
                charset = "UTF-8"; // 文件编码为 UTF-8
                checked = true;
            }

            bis.reset();

            if (!checked) {
                while ((read = bis.read()) != -1) {
                    if (read >= 0xF0)
                        break;
                    if (0x80 <= read && read <= 0xBF)
                        break;
                    if (0xC0 <= read && read <= 0xDF) {
                        read = bis.read();
                        if (!(0x80 <= read && read <= 0xBF)) {
                            break;
                        }
                    } else if (0xE0 <= read) {
                        read = bis.read();
                        if (0x80 <= read && read <= 0xBF) {
                            read = bis.read();
                            if (0x80 <= read && read <= 0xBF) {
                                charset = "UTF-8";
                            }
                        }
                        break;
                    }
                }
            }
            bis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return charset;
    }

    public static void convertFileCharset(File inputFile, File outputFile,String currentCharset ,String targetCharset) {
        try {
            InputStreamReader isr = new InputStreamReader(Files.newInputStream(inputFile.toPath()) ,currentCharset);
            java.io.OutputStreamWriter osw = new OutputStreamWriter(Files.newOutputStream(outputFile.toPath()) ,targetCharset);

            int len;
            while((len = isr.read())!=-1){
                osw.write(len);
            }

            osw.close();
            isr.close();
        } catch (Exception e) {
            LogUtil.doLog(1, "转换文件编码时发生错误! 详细信息: " + e, null);
            PortScannerForMinecraftServer.exit();
        }
    }

    public static String toIPAddress(String input) {
        try {
            // 尝试将输入字符串解析为IP地址
            InetAddress addr = InetAddress.getByName(input);
            return addr.getHostAddress();
        } catch (UnknownHostException e) {
            // 如果输入字符串无法解析为IP地址，则返回null
            return null;
        }
    }
}
