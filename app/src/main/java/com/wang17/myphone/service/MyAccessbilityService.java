package com.wang17.myphone.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._Utils;
import com.wang17.myphone.util._Session;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.model.database.Setting;
import com.wang17.myphone.model.database.TallyRecord;
import com.wang17.myphone.receiver.HeadsetPlugReceiver;

import java.util.ArrayList;
import java.util.List;

import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
import static com.wang17.myphone.receiver.AlarmReceiver.ALARM_NIANFO_BEFORE_OVER;
import static com.wang17.myphone.receiver.AlarmReceiver.ALARM_NIANFO_OVER;

public class MyAccessbilityService extends AccessibilityService {

    private int clickTag = 0;
    private DataContext dataContext;
    private int eventType;
    private String className, packageName;

    public boolean isFromAutoDial = false;
    private boolean isFromBack2Didi = false;
    private String prevAccount;

    private boolean isToLock = true, isFinished = false, isLockScreenOprating = false,isFirst=true;

    private void e(Object log){
        Log.e("wangsc",log.toString());
    }
    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        dataContext = new DataContext(this);

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        List<String> apps = _Utils.getAppInfos(getApplication());
        info.packageNames = apps.toArray(new String[apps.size()]); //监听过滤的包名
        for(String packageName : apps){
            Log.e("wangsc","包名："+packageName);
        }
//        info.packageNames = new String[]{"com.alibaba.android.rimet"}; //监听过滤的包名
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK; //监听哪些行为
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN; //反馈
        info.notificationTimeout = 100; //通知的时间
        setServiceInfo(info);

        // 注册耳机状态广播。
        registerHeadsetPlugReceiver();
    }


    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            packageName = event.getPackageName().toString();
            eventType = event.getEventType();
            className = event.getClassName().toString();

            if (eventType == TYPE_WINDOW_STATE_CHANGED) {
            } else if (eventType == TYPE_WINDOW_CONTENT_CHANGED) {
                e(packageName);
                e(className);
                printNodeInfo();
            } else {
                if (dataContext.getSetting(Setting.KEYS.is_print_other_all, true).getBoolean() == true && !className.contains("com.wang17.myphone")) {
                    printNodeInfo();
                }
            }

        } catch (Exception e) {
            _Utils.printException(getApplicationContext(), e);
        }
    }

    private void registerHeadsetPlugReceiver() {
        HeadsetPlugReceiver headsetPlugReceiver = new HeadsetPlugReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.HEADSET_PLUG");
        registerReceiver(headsetPlugReceiver, filter);
    }

    public void pauseNianfoTally() {
        try {
            // 向数据库中保存“已经完成的时间”
            long now = System.currentTimeMillis();
            long sectionStartInMillis = Long.parseLong(dataContext.getSetting(Setting.KEYS.tally_sectionStartMillis).getString());
            long endTimeInMillis = Long.parseLong(dataContext.getSetting(Setting.KEYS.tally_endInMillis).getString());
            dataContext.deleteSetting(Setting.KEYS.tally_sectionStartMillis);
            long sectionIntervalInMillis = now - sectionStartInMillis;
            if (sectionIntervalInMillis >= 60000) {
                this.saveSection(sectionStartInMillis, sectionIntervalInMillis);
            }

            // 向数据库中保存“剩余时间”
            dataContext.deleteSetting(Setting.KEYS.tally_endInMillis);
            long remainIntervalInMillis = endTimeInMillis - now;
            if (sectionIntervalInMillis < 60000) {
                remainIntervalInMillis = endTimeInMillis - sectionStartInMillis;
            }
            dataContext.addSetting(Setting.KEYS.tally_intervalInMillis, remainIntervalInMillis);

        } catch (Exception e) {
            _Utils.printException(getApplicationContext(), e);
        }

    }

    private void saveSection(long partStartMillis, long partSpanMillis) {
        try {
            TallyRecord tallyRecord = new TallyRecord(new DateTime(partStartMillis), (int) partSpanMillis,dataContext.getSetting(Setting.KEYS.tally_record_item_text,"").getString());
            if (dataContext.getRecord(partStartMillis) == null)
                dataContext.addRecord(tallyRecord);
        } catch (Exception e) {
            _Utils.printException(getApplicationContext(), e);
        }
    }

    public void stopAlarm() {
        try {
            Intent intent = new Intent(_Session.NIAN_FO_TIMER);
            PendingIntent pi = PendingIntent.getBroadcast(getApplicationContext(), ALARM_NIANFO_OVER, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            AlarmManager am = (AlarmManager) getApplicationContext().getSystemService(ALARM_SERVICE);
            am.cancel(pi);
            pi = PendingIntent.getBroadcast(getApplicationContext(), ALARM_NIANFO_BEFORE_OVER, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            am.cancel(pi);

        } catch (Exception e) {
            _Utils.printException(getApplicationContext(), e);
        }
    }

    private void switchClickTag() {
        switch (clickTag) {
            case 0:
                clickTag += clickViewByEqualText("退出当前帐号") ? 1 : 0;
                break;
            case 1:
                clickTag += clickViewByEqualText("退出") ? 1 : 0;
                break;
            case 2:
                clickTag += clickViewByEqualsDescription("更多") ? 1 : 0;
                break;
            case 3:
                clickTag += clickViewByEqualText("切换帐号") ? 1 : 0;
                break;
            case 4:
                clickTag += clickViewByEqualText("用微信号/QQ号/邮箱登录") ? 1 : 0;
                break;
            case 5:
                try {
                    if ("645708679".equals(prevAccount)) {
                        clickTag = clipUserNameAndPassword("850337424", "qq351489") ? 0 : 5;
                    } else {
                        clickTag = clipUserNameAndPassword("645708679", "qq351489") ? 0 : 5;
                    }
//                    int user = Integer.parseInt(dataContext.getSetting(Setting.KEYS.weixin_curr_account_id, 1).getString());
//                    if (user == 1) {
//                    } else {
//                    }
//                    if (clickTag == 0) {
//                        dataContext.editSetting(Setting.KEYS.weixin_curr_account_id, user == 1 ? 2 : 1);
//                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    private boolean clipUserNameAndPassword(String userName, String password) {
        try {
            AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
            if (nodeInfo != null) {
                allNodesInActiveWindow.clear();
                getAllNodesToList(nodeInfo);
                AccessibilityNodeInfo passwordNode = null, userNameNode = null, loginNode = null;
                for (AccessibilityNodeInfo node : allNodesInActiveWindow) {
                    if (node.getClassName().toString().equals("android.widget.EditText") && userNameNode == null) {
                        userNameNode = node;
                    } else if (node.getClassName().toString().equals("android.widget.EditText")) {
                        passwordNode = node;
                    } else if (node.getText() != null && "登录".equals(node.getText().toString())) {
                        loginNode = node;
                    }
                }
                if (userNameNode != null && passwordNode != null && loginNode != null) {

                    //android>21 = 5.0时可以用ACTION_SET_TEXT
                    // android>18 3.0.1可以通过复制的手段,先确定焦点，再粘贴ACTION_PASTE

                    //region 使用Bundle填入内容
                    Bundle arguments = new Bundle();
                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, userName);
                    userNameNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);

                    arguments = new Bundle();
                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, password);
                    passwordNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                    //endregion


                    // 点击登录
                    return clickView(loginNode);
                }
            }
        } catch (Exception e) {
            _Utils.printException(getApplicationContext(), e);
        }
        return false;
    }

    private boolean clipPassword(String password) {
        try {
            AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
            if (nodeInfo != null) {
                allNodesInActiveWindow.clear();
                getAllNodesToList(nodeInfo);
                AccessibilityNodeInfo passwordNode = null;
                for (AccessibilityNodeInfo node : allNodesInActiveWindow) {
                    if (node.getClassName().toString().equals("android.widget.EditText")) {
                        passwordNode = node;
                        break;
                    }
                }
                if (passwordNode != null) {

                    //android>21 = 5.0时可以用ACTION_SET_TEXT
                    // android>18 3.0.1可以通过复制的手段,先确定焦点，再粘贴ACTION_PASTE

                    //region 使用Bundle填入内容
                    Bundle arguments = new Bundle();
                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, password);
                    passwordNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                    //endregion
                }
            }
        } catch (Exception e) {
            _Utils.printException(getApplicationContext(), e);
        }
        return false;
    }

    private boolean clickView(AccessibilityNodeInfo node) {
        if (node.isClickable()) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return true;
        } else {
            AccessibilityNodeInfo parent = node.getParent();
            while (parent != null) {
                if (parent.isClickable()) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    return true;
                }
                parent = parent.getParent();
            }
        }
        return false;
    }

    /**
     * 查找到
     */
    List<AccessibilityNodeInfo> allNodesInActiveWindow = new ArrayList<AccessibilityNodeInfo>();

    /**
     * 得到当前屏幕中所有节点数量。
     *
     * @return
     */
    private int nodesNumInActiveWindow;

    private int getNodesNum() {
        nodesNumInActiveWindow = 0;
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            recursionNodeForCount(rootNode);
            return nodesNumInActiveWindow;
        } else {
            return 0;
        }
    }

    private boolean recursionNodeForCount(AccessibilityNodeInfo info) {
        if (info.getChildCount() == 0) {
            nodesNumInActiveWindow++;
        } else {
            for (int i = 0; i < info.getChildCount(); i++) {
                if (info.getChild(i) != null) {
                    recursionNodeForCount(info.getChild(i));
                }
            }
        }
        return false;
    }


    /**
     * 点击Text = btnText的按钮，只点击一个view即返回。
     *
     * @param viewText
     * @return
     */
    private boolean clickViewByEqualText(String viewText) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            AccessibilityNodeInfo node = getNodeByEqualsText(nodeInfo, viewText);
            if (node != null) {
                return clickView(node);
            }
        }
        return false;
    }

    /**
     * 点击Text = btnText的按钮，只点击一个view即返回。
     *
     * @param viewText
     * @return
     */
    private boolean clickViewByContainsText(String viewText) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            AccessibilityNodeInfo node = getNodeByContains(nodeInfo, viewText);
            if (node != null) {
                return clickView(node);
            }
        }
        return false;
    }


    /**
     * 循环点击指定view之后的第after个view;用于指定的viewText存在多个，例如滴滴的“系统消息”。
     *
     * @param viewText
     * @param after
     * @return
     */
    private void clickViewListByText(String viewText, int after) {
        String text = "";
        int targetIndex = 0;
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        AccessibilityNodeInfo node = null;
        if (nodeInfo != null) {
            allNodesInActiveWindow.clear();
            getAllNodesToList(nodeInfo);
            for (int i = 0; i < allNodesInActiveWindow.size(); i++) {
                node = allNodesInActiveWindow.get(i);
                if (node.getText() != null) {
                    text = node.getText().toString();
                }
                if (text.equals(viewText)) {
                    targetIndex = i + after;
                    clickView(allNodesInActiveWindow.get(targetIndex));
                }
            }
        }
    }


    /**
     * 点击ContentDescription = viewDescription
     *
     * @param viewDescription
     * @return
     */
    private boolean clickViewByEqualsDescription(String viewDescription) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            AccessibilityNodeInfo node = getNodeByEqualsDescription(nodeInfo, viewDescription);
            if (node != null) {
                return clickView(node);
            }
        }
        return false;
    }


    /**
     * 点击“通话”按钮
     *
     * @return
     */
    private boolean clickLeftDialButton() {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            List<AccessibilityNodeInfo> nodes = new ArrayList<>();
            getAllNodesToListByEqualsDescription(nodeInfo, "通话", nodes);

            // 执行点击
            if (nodes.size() == 1) {
                return clickView(nodes.get(0));
            } else if (nodes.size() == 2) {
                Rect rect0 = new Rect();
                Rect rect1 = new Rect();
                nodes.get(0).getBoundsInScreen(rect0);
                nodes.get(1).getBoundsInScreen(rect1);
                if (rect0.left < rect1.left) {
                    return clickView(nodes.get(0));
                } else {
                    return clickView(nodes.get(1));
                }
            }

        }
        return false;
    }


    /**
     * 检查当前窗口中是否有某按钮。
     *
     * @param viewText
     * @return
     */
    private boolean isExsitNodeByContainsText(String viewText) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            if (recursionNodeForExsitNodeByContainsText(nodeInfo, viewText)) {
                return true;
            }
        }
        return false;
    }

    public boolean recursionNodeForExsitNodeByContainsText(AccessibilityNodeInfo node, String viewText) {
        if (node.getChildCount() == 0) {
            String text = "";
            if (node.getText() != null) {
                text = node.getText().toString();
            }
            if (text.contains(viewText)) {
                return true;
            }
        } else {
            for (int i = 0; i < node.getChildCount(); i++) {
                if (node.getChild(i) != null) {
                    if (recursionNodeForExsitNodeByContainsText(node.getChild(i), viewText)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 检查当前窗口中是否有某按钮。
     *
     * @param viewText
     * @return
     */
    private boolean isExsitNodeByEqualText(String viewText) {
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        if (nodeInfo != null) {
            if (recursionNodeForExsitNodeByEqualText(nodeInfo, viewText)) {
                return true;
            }
        }
        return false;
    }

    public boolean recursionNodeForExsitNodeByEqualText(AccessibilityNodeInfo node, String viewText) {
        if (node.getChildCount() == 0) {
            String text = "";
            if (node.getText() != null) {
                text = node.getText().toString();
            }
            if (text.equals(viewText)) {
                return true;
            }
        } else {
            for (int i = 0; i < node.getChildCount(); i++) {
                if (node.getChild(i) != null) {
                    if (recursionNodeForExsitNodeByEqualText(node.getChild(i), viewText)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void printNodeInfo() {
//        packageName.contains("com.android.systemui") ||
        if (packageName.contains("com.sec.android.app.launcher") || packageName.contains("com.wang17.myphone")) {
            return;
        }

//        if (dataContext.getSetting(Setting.KEYS.is_print_ifClassName, true).getBoolean() == false || packageName.equals("com.wang17.myphone")) {
//            return;
//        }

        //
        StringBuilder texts = new StringBuilder();
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();
        getAllNodesPrintInfo(nodeInfo, texts);
        Log.e("wangsc",AccessibilityEvent.eventTypeToString(eventType)+"\n"+className + " ：" + packageName + "\n" + texts.toString());
//        dataContext.addRunLog2File(AccessibilityEvent.eventTypeToString(eventType), className + " ：" + packageName + "\n" + texts.toString());
    }

    public void getAllNodesPrintInfo(AccessibilityNodeInfo info, StringBuilder texts) {
        if (info != null) {
            if (info.getChildCount() == 0) {
                texts.append("【");
                if (info.getText() != null) {
                    texts.append(info.getText().toString());
                }
                if (info.getContentDescription() != null) {
                    texts.append("*" + info.getContentDescription().toString());
                }
                texts.append("】");
            } else {
                for (int i = 0; i < info.getChildCount(); i++) {
                    if (info.getChild(i) != null) {
                        getAllNodesPrintInfo(info.getChild(i), texts);
                    }
                }
            }
        }
    }

    public void getAllNodesToList(AccessibilityNodeInfo info) {
        if (info != null) {
            if (info.getChildCount() == 0) {
                allNodesInActiveWindow.add(info);
            } else {
                for (int i = 0; i < info.getChildCount(); i++) {
                    if (info.getChild(i) != null) {
                        getAllNodesToList(info.getChild(i));
                    }
                }
            }
        }
    }

    private void getAllNodesToListByEqualsDescription(AccessibilityNodeInfo nodeInfo, String viewDescription, List<AccessibilityNodeInfo> nodeInfoList) {
        if (nodeInfo.getChildCount() == 0) {
            if (nodeInfo.getContentDescription() != null) {
                String description = nodeInfo.getContentDescription().toString();
                if (description.equals(viewDescription)) {
                    nodeInfoList.add(nodeInfo);
                }
            }
        } else {
            for (int i = 0; i < nodeInfo.getChildCount(); i++) {
                if (nodeInfo.getChild(i) != null) {
                    getAllNodesToListByEqualsDescription(nodeInfo.getChild(i), viewDescription, nodeInfoList);
                }
            }
        }
    }

    /**
     * 找到与viewText的view就返回。
     *
     * @param info
     * @param viewText
     * @return
     */
    public AccessibilityNodeInfo getNodeByEqualsText(AccessibilityNodeInfo info, String viewText) {
        if (info != null) {
            if (info.getChildCount() == 0) {
                if (info.getText() != null) {
                    String text = info.getText().toString();
                    if (text.equals(viewText))
                        return info;
                }
            } else {
                for (int i = 0; i < info.getChildCount(); i++) {
                    if (info.getChild(i) != null) {
                        AccessibilityNodeInfo node = getNodeByEqualsText(info.getChild(i), viewText);
                        if (node != null) {
                            return node;
                        }
                    }
                }
            }
        }
        return null;
    }


    /**
     * 找到与viewText的view就返回。
     *
     * @param info
     * @param viewText
     * @return
     */
    public AccessibilityNodeInfo getNodeByContains(AccessibilityNodeInfo info, String viewText) {
        if (info != null) {
            if (info.getChildCount() == 0) {
                if (info.getText() != null) {
                    String text = info.getText().toString();
                    if (text.contains(viewText))
                        return info;
                }
            } else {
                for (int i = 0; i < info.getChildCount(); i++) {
                    if (info.getChild(i) != null) {
                        AccessibilityNodeInfo node = getNodeByContains(info.getChild(i), viewText);
                        if (node != null) {
                            return node;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 找到与viewText的view就返回。
     *
     * @param info
     * @param viewDescription
     * @return
     */
    public AccessibilityNodeInfo getNodeByEqualsDescription(AccessibilityNodeInfo info, String viewDescription) {
        if (info != null) {
            if (info.getChildCount() == 0) {
                if (info.getContentDescription() != null) {
                    String description = info.getContentDescription().toString();
                    if (description.equals(viewDescription))
                        return info;
                }
            } else {
                for (int i = 0; i < info.getChildCount(); i++) {
                    if (info.getChild(i) != null) {
                        AccessibilityNodeInfo node = getNodeByEqualsDescription(info.getChild(i), viewDescription);
                        if (node != null) {
                            return node;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void onInterrupt() {
    }


}
