package com.wang17.myphone.fragment;


import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.method.KeyListener;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.wang17.myphone.R;
import com.wang17.myphone.database.Setting;
import com.wang17.myphone.util.DataContext;
import com.wang17.myphone.util._Utils;

import java.util.ArrayList;
import java.util.List;

public class WebDialogFragment extends DialogFragment implements KeyListener {

    private WebView webView_lofter;
    private Spinner spinner_selector;
    private Button button7,button6,button5,button4,button3, button2, button1;

    private DataContext dataContext;
    private String hphm, fdjh, carName, province;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        dataContext = new DataContext(getContext());
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = View.inflate(getContext(), R.layout.fragment_web, null);
        final AlertDialog dialog = new AlertDialog.Builder(getActivity()).create();
        dialog.setView(view);

        try {

            button1 = (Button) view.findViewById(R.id.button_1);
            button2 = (Button) view.findViewById(R.id.button_2);
            button3 = (Button) view.findViewById(R.id.button_3);
            button4 = (Button) view.findViewById(R.id.button_4);
            button5 = (Button) view.findViewById(R.id.button_5);
            button6 = (Button) view.findViewById(R.id.button_6);
            button7 = (Button) view.findViewById(R.id.button_7);

            webView_lofter = (WebView) view.findViewById(R.id.webView_lofter);
            webView_lofter.setScrollContainer(true);
            webView_lofter.setScrollbarFadingEnabled(true);
            webView_lofter.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);

            WebSettings settings = webView_lofter.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDefaultTextEncodingName("UTF-8");
            settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
            settings.setLoadWithOverviewMode(true);
            settings.setSupportZoom(true); // 支持缩放
            settings.setUseWideViewPort(true);


            webView_lofter.setWebViewClient(new WebViewClient() {

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    if (url != null && url.contains("122.gov.cn/views/inquiry.html")) {
                        String fun = "javascript:" +
                                "   function getClass(parent,sClass) { " +
                                "       var aEle=parent.getElementsByTagName('div'); " +
                                "       var aResult=[]; " +
                                "       var i=0; " +
                                "       for(i<0;i<aEle.length;i++) { " +
                                "           if(aEle[i].className==sClass) { " +
                                "               aResult.push(aEle[i]); " +
                                "           }" +
                                "       }; " +
                                "       return aResult; " +
                                "   } " +
                                "   function getClass2(parent,sClass) { " +
                                "       var aEle=parent.getElementsByTagName('label'); " +
                                "       var aResult=[]; " +
                                "       var i=0; " +
                                "       for(i<0;i<aEle.length;i++) { " +
                                "           if(aEle[i].className==sClass) { " +
                                "               aResult.push(aEle[i]); " +
                                "           }" +
                                "       }; " +
                                "       return aResult; " +
                                "   } " +

                                "   function hideSome() { " +
                                "       getClass(document,'header')[0].style.display='none'; " +
                                "       getClass(document,'footer')[0].style.display='none'; " +
                                "       getClass(document,'control-group')[0].style.display='none'; " +
                                "       getClass(document,'control-group')[1].style.display='none'; " +
                                "       getClass(document,'control-group')[2].style.display='none'; " +
                                "       document.getElementById('infosearchnav').style.display='none'; " +
                                "       document.getElementById('tips').style.display='none'; " +
                                "       document.getElementById('hphm1-b').value='" + hphm + "'; " +
                                "       document.getElementsByName('fdjh')[0].value='" + fdjh + "'; " +
                                "       getClass2(document,'control-label')[3].style.display='none';" +
                                "       document.getElementById('searchResultContainer').innerHTML='<div style=\"font:bold 16px 宋体;color:#F00;\">" + carName + "：" + province + hphm + "</div>'; " +
//                                <div><p><b>查询方法：1、输入验证码。2、点击查询。</p></div>
                                "   }" +
                                "   hideSome();";
//                                "   function hideGotop(){" +
//                                "       alert('aaaaaaaaaaaaaaaaaaa'); " +
//                                "       document.getElementById('feedbak').style.display='none'; " +
//                                "       document.getElementById('gotop').style.display='none'; " +
//                                "   }" +
//                                "   window.onload = hideGotop;";
                        view.loadUrl(fun);
                    }
                }
            });


            spinner_selector = (Spinner) view.findViewById(R.id.spinner_selector);
            // 创建列表序列
            List<String> selector = new ArrayList();
            selector.add("JEEP");
            selector.add("小货");
            selector.add("尼桑");
            selector.add("大货");
            selector.add("吉利");
            selector.add("雪佛兰");
            fillSpinner(spinner_selector, selector);
            int index = dataContext.getSetting(Setting.KEYS.web_index, 0).getInt();
            spinner_selector.setSelection(index);
            spinner_selector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    switch (position) {
                        case 0:
                            province = "宁";
                            hphm = "A339K7";
                            fdjh = "371685";

                            webView_lofter.loadUrl("http://nx.122.gov.cn/views/inquiry.html");
                            break;
                        case 1:
                            province = "宁";
                            hphm = "AAW150";
                            fdjh = "530699";

                            webView_lofter.loadUrl("http://nX.122.gov.cn/views/inquiry.html");
                            break;
                        case 2:
                            province = "蒙";
                            hphm = "M13517";
                            fdjh = "33802D";

                            webView_lofter.loadUrl("http://nm.122.gov.cn/views/inquiry.html");
                            break;
                        case 3:

                            province = "宁";
                            hphm = "A002F3";
                            fdjh = "109976";

                            webView_lofter.loadUrl("http://nx.122.gov.cn/views/inquiry.html");
                            break;
                        case 4:

                            province = "宁";
                            hphm = "EM8160";
                            fdjh = "03252D";

                            webView_lofter.loadUrl("http://nx.122.gov.cn/views/inquiry.html");
                            break;
                        case 5:

                            province = "宁";
                            hphm = "A3L769";
                            fdjh = "310381";

                            webView_lofter.loadUrl("http://nx.122.gov.cn/views/inquiry.html");
                            break;
                    }
                    carName = spinner_selector.getSelectedItem().toString();
                    dataContext.editSetting(Setting.KEYS.web_index, position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });

            button1.setText("JEEP");
            button1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    province = "宁";
                    hphm = "A339K7";
                    fdjh = "371685";

                    webView_lofter.loadUrl("http://nx.122.gov.cn/views/inquiry.html");
                    carName = ((Button)v).getText().toString();
                    initButtonTextColor(v);
                }
            });
            button2.setText("小货");
            button2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    province = "宁";
                    hphm = "AAW150";
                    fdjh = "530699";

                    webView_lofter.loadUrl("http://nX.122.gov.cn/views/inquiry.html");
                    carName = ((Button)v).getText().toString();
                    initButtonTextColor(v);
                }
            });
            button3.setText("大货");
            button3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    province = "宁";
                    hphm = "A3L769";
                    fdjh = "310381";

                    webView_lofter.loadUrl("http://nx.122.gov.cn/views/inquiry.html");
                    carName = ((Button)v).getText().toString();
                    initButtonTextColor(v);
                }
            });
            button4.setText("吉利");
            button4.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    province = "宁";
                    hphm = "A3L769";
                    fdjh = "310381";

                    webView_lofter.loadUrl("http://nx.122.gov.cn/views/inquiry.html");
                    carName = ((Button)v).getText().toString();
                    initButtonTextColor(v);
                }
            });
            button5.setText("退出");
            button5.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
            button6.setText("雪佛兰");
            button6.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    province = "宁";
                    hphm = "A3L769";
                    fdjh = "310381";

                    webView_lofter.loadUrl("http://nx.122.gov.cn/views/inquiry.html");
                    carName = ((Button)v).getText().toString();
                    initButtonTextColor(v);
                }
            });
            button1.performClick();
        } catch (Exception e) {
            _Utils.printException(getContext(), e);
        }

        return dialog;
    }

    private void initButtonTextColor(View btn) {
        button1.setTextColor(Color.WHITE);
        button2.setTextColor(Color.WHITE);
        button3.setTextColor(Color.WHITE);
        button4.setTextColor(Color.WHITE);
        button5.setTextColor(Color.WHITE);
        button6.setTextColor(Color.WHITE);
        button7.setTextColor(Color.WHITE);
        ((Button)btn).setTextColor(Color.RED);
    }

    private void fillSpinner(Spinner spinner, List<String> values) {

        // 创建适配器
        ArrayAdapter<String> aspn = new ArrayAdapter<String>(getContext(), R.layout.inflate_spinner, values);
        aspn.setDropDownViewResource(R.layout.inflate_spinner_dropdown);

        // 设置适配器
        spinner.setAdapter(aspn);
    }

    @Override
    public int getInputType() {
        return 0;
    }

    @Override
    public boolean onKeyDown(View view, Editable text, int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView_lofter.canGoBack()) {
            webView_lofter.goBack();
            return true;
        }
        return true;
    }

    @Override
    public boolean onKeyUp(View view, Editable text, int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public boolean onKeyOther(View view, Editable text, KeyEvent event) {
        return false;
    }

    @Override
    public void clearMetaKeyState(View view, Editable content, int states) {

    }
}
