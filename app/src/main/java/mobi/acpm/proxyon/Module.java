package mobi.acpm.proxyon;

import java.net.InetAddress;
import java.net.URI;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class Module implements IXposedHookLoadPackage {

    public static final String PREFS = "ProxyOnSettings";
    private static XSharedPreferences sPrefs;

    public static void loadPrefs() {
        sPrefs = new XSharedPreferences(Module.class.getPackage().getName(), PREFS);
        sPrefs.makeWorldReadable();
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam loadPackageParam) throws Throwable {

        loadPrefs();

        if (!loadPackageParam.packageName.equals(sPrefs.getString("package", "")))
            return;

        findAndHookMethod("java.net.ProxySelectorImpl", loadPackageParam.classLoader, "select", URI.class, new XC_MethodHook() {

            @SuppressWarnings("ConstantConditions")
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                if(sPrefs.getBoolean("switch", false)) {

                    String overrideHost = sPrefs.getString("host", null);
                    String overridePort = sPrefs.getString("port", null);

                    if (sPrefs.getBoolean("useDNS", false)) {
                        //resolve Host and override overrideHost

                        try {
                            overrideHost = InetAddress.getByName(overrideHost).getHostAddress();
                        } catch (Exception ex) {
                            //no fucks given
                        }
                    }

                    System.setProperty("proxyHost", overrideHost);
                    System.setProperty("proxyPort", overridePort);

                    System.setProperty("http.proxyHost", overrideHost);
                    System.setProperty("http.proxyPort", overridePort);

                    System.setProperty("https.proxyHost", overrideHost);
                    System.setProperty("https.proxyPort", overridePort);

                    System.setProperty("socksProxyHost", overrideHost);
                    System.setProperty("socksProxyPort", overridePort);
                }
            }
        });

        findAndHookMethod("java.net.ProxySelectorImpl", loadPackageParam.classLoader, "isNonProxyHost", String.class, String.class, new XC_MethodHook() {

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {

                if(sPrefs.getBoolean("switch", false)) {
                    param.args[1] = "";
                }
            }
        });
    }
}
