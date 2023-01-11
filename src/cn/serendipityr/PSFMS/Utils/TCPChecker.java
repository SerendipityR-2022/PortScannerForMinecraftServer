package cn.serendipityr.PSFMS.Utils;

import cn.serendipityr.PSFMS.PortScannerForMinecraftServer;
import com.alibaba.fastjson.JSONObject;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TCPChecker {
    private static final ExecutorService executor = Executors.newFixedThreadPool(ConfigUtil.ScanThreads);

    public void doPortScan(String address) {
        // 创建新的线程
        new Thread(() -> {
            for (int i = ConfigUtil.MinPort; i < ConfigUtil.MaxPort + 1; i++) {
                int port = i;
                CompletableFuture<Boolean> future = checkPortOpenAsync(new InetSocketAddress(address, port), executor);
                future.thenCompose(isOpen -> {
                    if (isOpen) {
                        JSONObject minecraftServerInfo = null;
                        Integer responseCode = null;

                        try {
                            TimeUnit.MILLISECONDS.sleep(ConfigUtil.ScanDelay);
                            minecraftServerInfo = MSChecker.checkMinecraftServer(new InetSocketAddress(address, port));
                            TimeUnit.MILLISECONDS.sleep(ConfigUtil.ScanDelay);
                            responseCode = ServicesChecker.checkWebsite(new InetSocketAddress(address, port));
                        } catch (InterruptedException ignored) {}

                        String msg;
                        if (minecraftServerInfo != null) {
                            msg = "MC | " + address + ":" + port + " | " + MSChecker.getMinecraftServerInfo(minecraftServerInfo);
                        } else if (responseCode != null) {
                            msg = "HTTP | " + address + ":" + port + " | 状态码: " + responseCode;
                        } else {
                            msg = "TCP | " + address + ":" + port;
                        }
                        LogUtil.saveOpeningPort(msg);
                        LogUtil.doLog(-1, msg + "\n", "PortScan");
                    }
                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            TimeUnit.MILLISECONDS.sleep(ConfigUtil.ScanDelay);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        return true;
                    }, executor);
                });
            }
        }).start();
    }

    public static CompletableFuture<Boolean> checkPortOpenAsync(InetSocketAddress inetSocketAddress, ExecutorService executor) {
        return CompletableFuture.supplyAsync(() -> checkPortOpen(inetSocketAddress), executor);
    }

    public static Boolean checkPortOpen(InetSocketAddress inetSocketAddress) {
        boolean isOpen;

        try (Socket socket = new Socket()) {
            socket.connect(inetSocketAddress, ConfigUtil.ConnectTimeout);
            socket.setSoTimeout(ConfigUtil.ReadTimeout);
            isOpen = true;
        } catch (Exception e) {
            if (ConfigUtil.ShowFails) {
                String msg = "Fails | " + ConfigUtil.ScanAddress + ":" + inetSocketAddress.getPort() + " | " + e;
                LogUtil.saveOpeningPort(msg);
                LogUtil.doLog(-1, msg + "\n", "PortScan");
            }
            isOpen = false;
        }

        if (!PortScannerForMinecraftServer.isLinux) {
            SetTitle.INSTANCE.SetConsoleTitleA("PortScannerForMS - Made by SerendipityR | Scanning: " + inetSocketAddress.getPort() + "/" + ConfigUtil.MaxPort);
        }

        return isOpen;
    }
}
