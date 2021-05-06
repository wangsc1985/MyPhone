package com.wang17.myphone.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.CoordinateConverter;
import com.amap.api.location.DPoint;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.TextureMapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonSharePoint;
import com.amap.api.services.share.ShareSearch;
import com.wang17.myphone.R;
import com.wang17.myphone.model.DateTime;
import com.wang17.myphone.database.Location;
import com.wang17.myphone.database.Setting;
import com.wang17.myphone.database.DataContext;
import com.wang17.myphone.util._Session;
import com.wang17.myphone.util._Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class LocationListActivity extends AppCompatActivity {

    private static final String _TAG = "wangsc";
    private ExpandableListView listViewLocations;


    private LocationListdAdapter locationListdAdapter;

    private DataContext dataContext;
    private List<GroupInfo> groupInfoList;
    private List<List<Location>> childListList;
    private List<Location> locationList;

    private boolean isTimer = false;
    private int curPosition;
    private static PowerManager.WakeLock wakeLock;
    private int preGroupPosition = -1;
    private WebView mUrlView;

    @Override
    protected void onPause() {
        super.onPause();
        stopTimer();
        isTimer = false;
        _Utils.releaseWakeLock(LocationListActivity.this, wakeLock);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_list);

        initData();
        initView();


        mUrlView = (WebView) findViewById(R.id.url_view);

        textureMapView = findViewById(R.id.map);
        textureMapView.onCreate(savedInstanceState);// 此方法必须重写

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        centerPoint = new Point(displayMetrics.widthPixels / 2, displayMetrics.heightPixels / 2);

        initMap();
        listViewLocations.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                return true;
            }
        });
        listViewLocations.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {
            @Override
            public void onGroupExpand(int groupPosition) {
                try {
                    listViewLocations.collapseGroup(preGroupPosition);
                } catch (Exception e) {

                }
                preGroupPosition = groupPosition;
            }
        });

    }

    private ProgressDialog mProgDialog = null;// 搜索时进度条

    /**
     * 显示进度框
     */
    private void showProgressDialog() {
        if (mProgDialog == null)
            mProgDialog = new ProgressDialog(this);
        mProgDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgDialog.setIndeterminate(false);
        mProgDialog.setCancelable(true);
        mProgDialog.show();
    }

    /**
     * 隐藏进度框
     */
    private void dissmissProgressDialog() {
        if (mProgDialog != null && mProgDialog.isShowing()) {
            mProgDialog.dismiss();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if(keyCode==KeyEvent.KEYCODE_BACK&&listViewLocations.getVisibility()==View.GONE){
////            if(){
//                listViewLocations.setVisibility(View.VISIBLE);
//                return true;
////            }
//        }
        return super.onKeyDown(keyCode, event);
    }

    //region Timer
    private Timer timer;
    private Location start;

    private void startTimer() {
        if (timer != null) {
            return;
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    Location location = locationList.get(curPosition);
                    LatLng center = new LatLng(location.Latitude, location.Longitude);
                    DateTime now = new DateTime(location.Time);
                    moveLocationMarkers(center, location.Accuracy, now.toShortDateString(), now.toTimeString() + " (" + now.getWeekDayStr() + ")");
                    aMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(center, zoom, 0, 0)), 100, null);
                    curPosition--;
                    if (curPosition < 0) {
                        stopTimer();
                    }
                } catch (Exception e) {
                    _Utils.printException(LocationListActivity.this, e);
                }
            }
        }, 0, 3000);
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
    //endregion

    //region 地图
    private TextureMapView textureMapView;
    private AMap aMap;
    private UiSettings mUiSettings;
    Point centerPoint;
    Marker locationMarker;
    float zoom = 16;
    private AMapLocationClient locationClient;
    private Circle accuracyCircle, searchCircle;
    private ShareSearch mShareSearch;

    private void moveLocationMarkers(LatLng latLng, double accuracy, String date, String time) {
//        northMarker.setPosition(latLng);
        locationMarker.setPosition(latLng);
        accuracyCircle.setCenter(latLng);
        accuracyCircle.setRadius(accuracy);
        locationMarker.setTitle(date);
        locationMarker.setSnippet(time);
    }

    private void initMap() {
        /*
         * 设置离线地图存储目录，在下载离线地图或初始化地图设置; 使用过程中可自行设置, 若自行设置了离线地图存储的路径，
         * 则需要在离线地图下载和使用地图页面都进行路径设置
         */
        Log.e("wangsc", MapsInitializer.sdcardDir);

        mShareSearch = new ShareSearch(this);
        mShareSearch.setOnShareSearchListener(new ShareSearch.OnShareSearchListener() {
            @Override
            public void onPoiShareUrlSearched(String url, int errorCode) {
                //
                if (errorCode == AMapException.CODE_AMAP_SUCCESS) {
                    mUrlView.loadUrl(url);
                } else {
                    Toast.makeText(LocationListActivity.this, errorCode, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onLocationShareUrlSearched(String url, int errorCode) {
                //
                if (errorCode == AMapException.CODE_AMAP_SUCCESS) {
                    /**
                     * 本地发送短信
                     * 有发送短信对话框，经过同意之后才会发送信息
                     */
//                    String address = "18509513143",content=url;
//                    SmsManager manager = SmsManager.getDefault();
//                    Intent i = new Intent("com.android.TinySMS.RESULT");
//                    //生成PendingIntent，当消息发送完成，接收到广播
//                    PendingIntent sentIntent = PendingIntent.getBroadcast(LocationListActivity.this, 0, i, PendingIntent.FLAG_ONE_SHOT);
//                    manager.sendTextMessage(address, null, content, sentIntent, null);
                    mUrlView.loadUrl(url);
                } else {
                    Toast.makeText(LocationListActivity.this, errorCode, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onNaviShareUrlSearched(String s, int i) {

            }

            @Override
            public void onBusRouteShareUrlSearched(String s, int i) {

            }

            @Override
            public void onWalkRouteShareUrlSearched(String s, int i) {

            }

            @Override
            public void onDrivingRouteShareUrlSearched(String s, int i) {

            }
        });
        aMap = textureMapView.getMap();
        mUiSettings = aMap.getUiSettings();
        mUiSettings.setScrollGesturesEnabled(true);// 设置地图是否可以手势滑动
        mUiSettings.setZoomGesturesEnabled(true); // 设置地图是否可以手势缩放大小
        mUiSettings.setTiltGesturesEnabled(false);// 设置地图是否可以倾斜
        mUiSettings.setRotateGesturesEnabled(false);// 设置地图是否可以旋转
        mUiSettings.setZoomControlsEnabled(true);// 设置地图是否显示手势缩放
        mUiSettings.setZoomPosition(AMapOptions.ZOOM_POSITION_RIGHT_BUTTOM);// 设置缩放按钮位置
        mUiSettings.setMyLocationButtonEnabled(true);// 设置地图默认的定位按钮是否显示
        mUiSettings.setScaleControlsEnabled(true);// 比例尺
        aMap.setMyLocationEnabled(false);// 是否可触发定位并显示定位层

        aMap.setLoadOfflineData(true);


        locationClient = new AMapLocationClient(this);
        LatLng center = new LatLng(locationClient.getLastKnownLocation().getLatitude(), locationClient.getLastKnownLocation().getLongitude());
        addLocationMarkers(center);


        if (dataContext.getSetting(Setting.KEYS.地图位置显示模式, 0).getInt() == 0) {
            /**
             * 定位模式
             */
            aMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(center, zoom, 0, 0)));

            aMap.setOnCameraChangeListener(new AMap.OnCameraChangeListener() {
                @Override
                public void onCameraChange(CameraPosition cameraPosition) {
                    zoom = cameraPosition.zoom;
                }

                @Override
                public void onCameraChangeFinish(CameraPosition cameraPosition) {
                }
            });

            aMap.setOnMapLongClickListener(new AMap.OnMapLongClickListener() {
                @Override
                public void onMapLongClick(LatLng latLng) {
                    if (searchCircle == null) {
                        searchCircle = aMap.addCircle(new CircleOptions()
                                .center(latLng)
                                .radius(dataContext.getSetting(Setting.KEYS.地图搜索半径_米,200).getDouble())
                                .fillColor(getResources().getColor(R.color.location_accuracy))
                                .strokeWidth(0));
                    } else {
                        searchCircle.setCenter(latLng);
                    }

                    List<Location> searchLocationList = new ArrayList<>();
                    locationList = dataContext.getLocatiosByDayspan(_Session.UUID_NULL,dataContext.getSetting(Setting.KEYS.location_search_days,90).getInt(), true);
                    for (Location location : locationList) {
                        if (searchCircle.contains(new LatLng(location.Latitude, location.Longitude))) {
                            searchLocationList.add(location);
                        }
                    }

                    Log.e("wangsc", searchLocationList.size() + "条记录");
                    initListData(searchLocationList);
                    locationListdAdapter.notifyDataSetChanged();
                }
            });

            aMap.setOnMapTouchListener(new AMap.OnMapTouchListener() {
                @Override
                public void onTouch(MotionEvent motionEvent) {

                }
            });

            aMap.setOnMapLoadedListener(new AMap.OnMapLoadedListener() {
                @Override
                public void onMapLoaded() {

                }
            });
        }
    }


    private void addLocationMarkers(LatLng center) {
//        northMarker = aMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f).icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.north))).position(center));

        locationMarker = aMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f)
                .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.gps_point)))
                .title("")
                .snippet("")
                .position(center));

        locationMarker.showInfoWindow();

        accuracyCircle = aMap.addCircle(new CircleOptions()
                .center(center)
                .radius(0)
                .fillColor(getResources().getColor(R.color.location_accuracy))
                .strokeWidth(0));
    }
    //endregion

    private void initView() {
        listViewLocations = findViewById(R.id.listView_locations);
        listViewLocations.setAdapter(locationListdAdapter);
    }

    private void initData() {
        dataContext = new DataContext(this);

        locationList = dataContext.getLocatiosByYear(_Session.UUID_NULL,dataContext.getSetting(Setting.KEYS.足迹展示年份,2019).getInt(), true);

        initListData(locationList);

        locationListdAdapter = new LocationListdAdapter();
    }

    private void initListData(List<Location> locationList) {

        groupInfoList = new ArrayList<>();
        childListList = new ArrayList<>();
        if (locationList.size() == 0) return;

        Location preLocation = locationList.get(0);
        List<Location> childList = new ArrayList<>();
        GroupInfo groupInfo = new GroupInfo(preLocation.Address, new DateTime(preLocation.Time), new DateTime(preLocation.Time), preLocation.Latitude, preLocation.Longitude);
        for (Location location : locationList) {
            float distance = CoordinateConverter.calculateLineDistance(new DPoint(preLocation.Latitude, preLocation.Longitude), new DPoint(location.Latitude, location.Longitude));
            if (distance > 100) {
                groupInfoList.add(groupInfo);
                childListList.add(childList);
                groupInfo = new GroupInfo(location.Address, new DateTime(location.Time), new DateTime(location.Time), location.Latitude, location.Longitude);
                groupInfo.count = 1;
                childList = new ArrayList<>();
            } else {
                groupInfo.firstTime = new DateTime(location.Time);
                if (groupInfo.address.isEmpty())
                    groupInfo.address = location.Address;
                groupInfo.count++;
            }

            childList.add(location);
            preLocation = location;
        }
        groupInfoList.add(groupInfo);
        childListList.add(childList);


        Log.e("wangsc", "groupInfoList" + groupInfoList.size());
        Log.e("wangsc", "childListList" + childListList.size());
    }

    class LocationListdAdapter extends BaseExpandableListAdapter {
        @Override
        public int getGroupCount() {
            return groupInfoList.size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return groupInfoList.get(groupPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return childListList.get(groupPosition).size();
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return childListList.get(groupPosition).get(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {

            try {
                convertView = View.inflate(LocationListActivity.this, R.layout.inflate_list_item_location_group, null);
                LinearLayout linearContainer = (LinearLayout) convertView.findViewById(R.id.linear_container);
                TextView textView_address = (TextView) convertView.findViewById(R.id.textView_address);
                TextView textView_firstTime = (TextView) convertView.findViewById(R.id.textView_firstTime);
                TextView textView_lastTime = (TextView) convertView.findViewById(R.id.textView_lastTime);
                TextView textView_count = (TextView) convertView.findViewById(R.id.textView_count);

                linearContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        final Location location = childListList.get(groupPosition).get(0);
                        LatLng center = new LatLng(location.Latitude, location.Longitude);
                        DateTime now = new DateTime(location.Time);
                        moveLocationMarkers(center, location.Accuracy, now.toShortDateString(), now.toTimeString() + " (" + now.getWeekDayStr() + ")");
                        aMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(center, zoom, 0, 0)), 100, null);
                    }
                });
                linearContainer.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (!listViewLocations.isGroupExpanded(groupPosition))
                            listViewLocations.expandGroup(groupPosition);
                        else
                            listViewLocations.collapseGroup(groupPosition);
                        return true;
                    }
                });
                GroupInfo info = groupInfoList.get(groupPosition);
                textView_address.setText(info.address.isEmpty() ? "---------------------" : info.address);
                textView_firstTime.setText(info.firstTime.toLongDateString4());
                if (info.lastTime.getDay() == info.firstTime.getDay() && info.lastTime.getMonth() == info.firstTime.getMonth()) {
                    textView_lastTime.setText(info.lastTime.toShortTimeString());
                } else {
                    textView_lastTime.setText(info.lastTime.toLongDateString4());
                }
                textView_count.setText(info.count + "次");

            } catch (Exception e) {
                _Utils.printException(LocationListActivity.this, e);
            }
            return convertView;
        }

        @Override
        public View getChildView(final int groupPosition, final int childPosition,  boolean isLastChild, View convertView, ViewGroup parent) {
            try {
                convertView = View.inflate(LocationListActivity.this, R.layout.inflate_list_item_location, null);
                LinearLayout container = convertView.findViewById(R.id.linearLayout_container);
                TextView textViewProvider = convertView.findViewById(R.id.textView_provider);
                TextView textViewSpeed = convertView.findViewById(R.id.textView_speed);
                TextView textViewAccuracy = convertView.findViewById(R.id.textView_accuracy);
                TextView textViewAddress = convertView.findViewById(R.id.textView_address);
                TextView textViewTime = convertView.findViewById(R.id.textView_time);
                TextView textViewSummary = convertView.findViewById(R.id.textView_summary);
                final Location location = childListList.get(groupPosition).get(childPosition);

                textViewAccuracy.setText(location.Accuracy + "米");
                textViewAddress.setText(location.Address.isEmpty() ? "---------------------" : location.Address);
                textViewProvider.setText(location.Provider);
                textViewSpeed.setText(location.Speed + "");
                textViewTime.setText(new DateTime(location.Time).toLongDateTimeString());
                textViewSummary.setText(location.Summary);
                container.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final Location location = childListList.get(groupPosition).get(childPosition);
                        LatLng center = new LatLng(location.Latitude, location.Longitude);
                        DateTime now = new DateTime(location.Time);
                        moveLocationMarkers(center, location.Accuracy, now.toShortDateString(), now.toTimeString() + " (" + now.getWeekDayStr() + ")");
                        aMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(center, zoom, 0, 0)), 100, null);
                    }
                });
                container.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {

                        new AlertDialog.Builder(LocationListActivity.this).setItems(new String[]{!isTimer ? "开始播放" : "停止播放", "分享位置", "删除位置"}, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case 0:

                                        if (!isTimer) {
                                            Location loc = childListList.get(groupPosition).get(childPosition);
                                            int position = locationList.indexOf(loc);
                                            Log.e(_TAG, "position : " + position + " , Address : " + loc.Address + " , Time : " + new DateTime(loc.Time).toLongDateTimeString());
                                            curPosition = position;
                                            startTimer();
                                            isTimer = true;
                                            wakeLock = _Utils.acquireWakeLock(LocationListActivity.this, PowerManager.SCREEN_BRIGHT_WAKE_LOCK);

                                        } else {
                                            stopTimer();
                                            isTimer = false;
                                            _Utils.releaseWakeLock(LocationListActivity.this, wakeLock);


                                        }

                                        break;
                                    case 1:
                                        Location location = childListList.get(groupPosition).get(childPosition);
                                        LatLonSharePoint point = new LatLonSharePoint(location.Latitude, location.Longitude, location.Address);
                                        mShareSearch.searchLocationShareUrlAsyn(point);
                                        break;
                                    case 2:
                                        dataContext.deleteLocation(childListList.get(groupPosition).get(childPosition).Id);
                                        locationList = dataContext.getLocatiosByDayspan(_Session.UUID_NULL,90, true);
                                        locationListdAdapter.notifyDataSetChanged();
                                        break;
                                }
                            }
                        }).show();
                        return true;
                    }
                });

            } catch (Exception e) {
                _Utils.printException(LocationListActivity.this, e);
            }
            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }

    }

//    protected class LocationListdAdapter extends BaseAdapter {
//        @Override
//        public int getCount() {
//            return locationList.size();
//        }
//
//        @Override
//        public Object getItem(int position) {
//            return null;
//        }
//
//        @Override
//        public long getItemId(int position) {
//            return 0;
//        }
//
//        @Override
//        public View getView(int position, View convertView, ViewGroup parent) {
//        }
//    }

    private class GroupInfo {
        /**
         * 当前地址
         */
        String address;
        /**
         * 第一次记录
         */
        DateTime firstTime;
        /**
         * 最后一次记录
         */
        DateTime lastTime;
        /**
         * 纬度
         */
        double latitude;
        /**
         * 经度
         */
        double longitude;
        /**
         * 在当前位置记录次数
         */
        int count = 0;


        public GroupInfo(String address, DateTime firstTime, DateTime lastTime, double latitude, double longitude) {
            this.address = address;
            this.firstTime = firstTime;
            this.lastTime = lastTime;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    private class ChildInfo {
    }
}
