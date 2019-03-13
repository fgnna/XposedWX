package someday.com.xposedweixin;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    private Activity launcherUiActivity;//微信主界面

    private boolean isRun = false;

    private ConcurrentLinkedQueue mConcurrentLinkedQueue = new ConcurrentLinkedQueue();

    private Thread mTaskRun = new Thread(){
        @Override
        public void run()
        {
            while (true)
            {
                Object o = mConcurrentLinkedQueue.poll();
                synchronized(this)
                {
                    if(null == o)
                    {
                        try
                        {

                            this.wait();
                        } catch (InterruptedException e)
                        {
                            e.printStackTrace();
                        }
                    }
                    else
                    {

                        log("获取一个任务 ");

                    }
                }

            }
        }
    };


    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable
    {
//        log("packageName:"+loadPackageParam.processName +":"+this);

        // Xposed模块自检测
        if (loadPackageParam.packageName.equals("someday.com.xposedweixin"))
        {
            XposedHelpers.findAndHookMethod("someday.com.xposedweixin.MainActivity", loadPackageParam.classLoader, "isModuleActive", XC_MethodReplacement.returnConstant(true));
        }



        if (loadPackageParam.processName.equals("com.tencent.mm"))
        {
            if(!isRun)
            {
                isRun = true;
                mTaskRun.setPriority(1);
                mTaskRun.start();
            }
            initHook(loadPackageParam);//初始化动作
            hookNewMessage(loadPackageParam);//追踪红包消息
            hookOpenLuckyMoney(loadPackageParam);//自动点开红包
            hookCloseLuckyMoneyDetailUI(loadPackageParam);//自动关闭红包详情页
        }

    }

    /**
     * 初始化微信红包勾子
     * @param loadPackageParam
     */
    public void initHook(XC_LoadPackage.LoadPackageParam loadPackageParam)
    {
        findAndHookMethod("com.tencent.mm.ui.LauncherUI", loadPackageParam.classLoader, "onCreate", Bundle.class, new XC_MethodHook()
        {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable
            {
                launcherUiActivity = (Activity) param.thisObject;
            }
        });
    }


    /**
     * 过滤红包消息
     *
     * @param loadPackageParam
     */
    public void hookNewMessage(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable
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
                                mConcurrentLinkedQueue.add(new Object());
//                                mTaskRun.notifyAll();
                            } else
                            {
                                log("launcherUiActivity == null" + "\n");
                            }
                        }


                    }
                });
    }

    /**
     * 触发开红包动作
     */
    public void hookOpenLuckyMoney(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable
    {
        findAndHookMethod("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNotHookReceiveUI", loadPackageParam.classLoader, "onResume", new XC_MethodHook()
                {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable
                    {
                        log("LuckyMoneyNotHookReceiveUI");
                        Field buttonField = XposedHelpers.findField(param.thisObject.getClass(), "nai");
                        final Button kaiButton = (Button) buttonField.get(param.thisObject);
                        if(kaiButton.getVisibility() != View.VISIBLE)
                        {
                            ((Activity)param.thisObject).finish();
                        }
                        else
                        {
                            //这里必需要延迟0.5秒点击，不然有时不生效
                            kaiButton.postDelayed(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    kaiButton.performClick();

                                }
                            },500);
                        }

                    }
                });
    }

    /**
     * 关闭红包详情页
     */
    public void hookCloseLuckyMoneyDetailUI(final XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable
    {
        findAndHookMethod("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI", loadPackageParam.classLoader, "onCreate", Bundle.class, new XC_MethodHook()
        {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable
            {
                ((Activity) param.thisObject).finish();
            }
        });
    }

}
