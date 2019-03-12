package someday.com.xposedweixin;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findField;

public class XposedLuckyMoney implements IXposedHookLoadPackage
{

    private String lastTalkerName = "";
    private String lastNativeUrl = "";
    private Activity launcherUiActivity;


    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable
    {
        log("Loaded app: " + loadPackageParam.packageName);
        Log.d("YOUR_TAG", "Loaded app: " + loadPackageParam.packageName);
        // Xposed模块自检测
        if (loadPackageParam.packageName.equals("someday.com.xposedweixin"))
        {
            XposedHelpers.findAndHookMethod("someday.com.xposedweixin.MainActivity", loadPackageParam.classLoader, "isModuleActive", XC_MethodReplacement.returnConstant(true));
        }
        if (loadPackageParam.packageName.equals("com.tencent.mm"))
        {
            findAndHookMethod("com.tencent.wcdb.database.SQLiteDatabase", loadPackageParam.classLoader, "insertWithOnConflict", String.class, String.class, ContentValues.class, int.class,
                    new XC_MethodHook()
            {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable
                {
                    log("table=" + param.args[0] + "\n");
                    ContentValues contentValues = (ContentValues) param.args[2];

                    if ("WalletLuckyMoney".equals(param.args[0]))
                    {
                        log("------------------------红包参数---------------------" + "\n");
                        log("mNativeUrl:" + contentValues.getAsString("mNativeUrl"));
                        lastNativeUrl = contentValues.getAsString("mNativeUrl");
                        log("------------------------红包参数 END---------------------" + "\n");


                    } else if ("message".equals(param.args[0]) && null != contentValues.getAsString("reserved") && contentValues.getAsString("reserved").indexOf("微信红包") != -1)
                    {
                        log("------------------------红包消息---------------------" + "\n");
                        log("talker:" + contentValues.getAsString("talker"));
                        lastTalkerName = contentValues.getAsString("talker");
//                        log("content:"+contentValues.getAsString("content"));
                        log("------------------------红包消息 END---------------------" + "\n");
                        // 启动红包页面
                        if (launcherUiActivity != null)
                        {
                            log("触发一个开红包的窗口" + "\n");
                            Intent paramau = new Intent();
                            paramau.putExtra("key_way", 0);
                            paramau.putExtra("key_native_url", lastNativeUrl);
                            paramau.putExtra("key_username", lastTalkerName);
                            paramau.putExtra("key_cropname", (String) null);
                            callStaticMethod(findClass("com.tencent.mm.br.d", loadPackageParam.classLoader), "b", launcherUiActivity, "luckymoney", ".ui.LuckyMoneyNotHookReceiveUI", paramau);
                        } else
                        {
                            log("launcherUiActivity == null" + "\n");
                        }
                    }


//
//                    log("------------------------insert over---------------------" + "\n\n");
//                    log("------------------------insertWithOnConflict called---------------------" + "\n");
//                    log("------------------------insert start---------------------" + "\n\n");
//                    log("param args1:" + (String)param.args[0]);
//                    log("param args1:" + (String)param.args[1]);
//                    ContentValues contentValues = (ContentValues) param.args[2];
//                    log("param args3 contentValues:");
//                    for (Map.Entry<String, Object> item : contentValues.valueSet())
//                    {
//                        if (item.getValue() != null)
//                        {
//                            log(item.getKey() + "---------" + item.getValue().toString());
//                        }
//                        else
//                        {
//                            log(item.getKey() + "---------" + "null");
//                        }
//                    }
//
//                    log("------------------------insert over---------------------" + "\n\n");


                }
            });

//            findAndHookMethod("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNotHookReceiveUI", loadPackageParam.classLoader, "onCreate", Bundle.class, new XC_MethodHook() {
//                @Override
//                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                    log("LuckyMoneyNotHookReceiveUI");
//                    Activity activity = (Activity) param.thisObject;
//                    String key_username = activity.getIntent().getStringExtra("key_username");
//                    String key_native_url = activity.getIntent().getStringExtra("key_native_url");
//                    String key_cropname = activity.getIntent().getStringExtra("key_cropname");
//                    int key_way = activity.getIntent().getIntExtra("key_way", 0);
//                    log("key_username: " + key_username + "\n");
//                    log("key_native_url: " + key_native_url + "\n");
//                    log("key_cropname: " + key_cropname + "\n");
//                    log("key_way: " + key_way + "\n");
//
////                    callMethod(param.thisObject,"iA",true);
//                    Class clasz = param.thisObject.getClass();
//                    Method[] methods = clasz.getDeclaredMethods();
//                    for(Method method : methods)
//                    {
//                        log("methods name: " + method.getName());
//                        if("iA".equals(method.getName()))
//                        {
//                            method.invoke(param.thisObject,true);
//                        }
//                    }
//                }
//            });

            findAndHookMethod("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNotHookReceiveUI", loadPackageParam.classLoader, "c", int.class, int.class, String.class,
                    loadPackageParam.classLoader.loadClass("com.tencent.mm.ah.m"), new XC_MethodHook()
            {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable
                {
                    log("LuckyMoneyNotHookReceiveUI");
                    Field buttonField = XposedHelpers.findField(param.thisObject.getClass(), "nai");
                    final Button kaiButton = (Button) buttonField.get(param.thisObject);
                    kaiButton.performClick();

                }
            });
            findAndHookMethod("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI", loadPackageParam.classLoader, "onCreate", Bundle.class,new XC_MethodHook()
            {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable
                {
                    ((Activity)param.thisObject).finish();
                }
            });
//            findAndHookMethod("com.tencent.mm.ui.chatting.d.a", loadPackageParam.classLoader, "getTalkerUserName",  new XC_MethodHook() {
//                @Override
//                protected void afterHookedMethod(MethodHookParam param) throws Throwable
//                {
//                    super.afterHookedMethod(param);
//                    log("getTalkerUserName:"+param.getResult());
//
//                }
//
//                @Override
//                protected void beforeHookedMethod(MethodHookParam param) throws Throwable
//                {
//                    super.beforeHookedMethod(param);
//                    log("getTalkerUserName:"+param.getResult());
//                }
//            });
            findAndHookMethod("com.tencent.mm.ui.LauncherUI", loadPackageParam.classLoader, "onCreate", Bundle.class, new XC_MethodHook()
            {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable
                {
                    launcherUiActivity = (Activity) param.thisObject;
                }
            });
        }

    }
}
