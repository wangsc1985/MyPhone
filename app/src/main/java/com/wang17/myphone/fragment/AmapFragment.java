package com.wang17.myphone.fragment;


import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapOptions;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapsInitializer;
import com.amap.api.maps.Projection;
import com.amap.api.maps.TextureMapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.Gradient;
import com.amap.api.maps.model.HeatmapTileProvider;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.MultiPointItem;
import com.amap.api.maps.model.MultiPointOverlay;
import com.amap.api.maps.model.MultiPointOverlayOptions;
import com.amap.api.maps.model.NaviPara;
import com.amap.api.maps.model.TileOverlay;
import com.amap.api.maps.model.TileOverlayOptions;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.core.SuggestionCity;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.wang17.myphone.R;
import com.wang17.myphone.event.AmapGearEvent;
import com.wang17.myphone.util.AMapUtil;
import com.wang17.myphone.database.DataContext;
import com.wang17.myphone.util.OffLineMapUtils;
import com.wang17.myphone.util._Utils;
import com.wang17.myphone.util._Session;
import com.wang17.myphone.database.Location;
import com.wang17.myphone.database.Setting;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.SENSOR_SERVICE;
import static com.wang17.myphone.util.AMapSession.ALT_HEATMAP_GRADIENT_COLORS;
import static com.wang17.myphone.util.AMapSession.ALT_HEATMAP_GRADIENT_START_POINTS;

/**
 * A simple {@link Fragment} subclass.
 */
public class AmapFragment extends Fragment implements AMap.OnMarkerClickListener, AMap.InfoWindowAdapter, SensorEventListener {

    DataContext dataContext;
    private boolean isFirst = true;


    private ImageView imageToilet, imageGas, imageHeat, imageZoomIn, imageZoomOut, imageNavi;
    private TextView textViewGear;
    private ImageButton imageLocation;


    private AMap aMap;
    private TextureMapView textureMapView;
    private UiSettings mUiSettings;

    private TileOverlay tileOverlay;
    private MultiPointOverlay multiPointOverlay;
    private Circle searchRangeCircle, accuracyCircle;
    private long animateLong;
    private boolean isMapRotate = false, isLocationNotCenter = false;
    float zoom = 16;
    // 处理静止后跟随的timer
    private Timer needFollowTimer;
    // 屏幕静止DELAY_TIME之后，再次跟随
    private long DELAY_TIME = 5000;

    private AMapLocationClient locationClient;
    private AMapLocationClientOption locationOption;
    private LatLng lastLocation;

    public AmapFragment() {
    }

    //    MyLocationStyle myLocationStyle;
//    Marker northMarker;
    Marker locationMarker;
    Handler uiHandler;
    private boolean isNeedFollow = true;
    private float mapBearing = 0f;
    Point carPoint, centerPoint;


    private void cameriaCenter(LatLng center) {
//        aMap.animateCamera(CameraUpdateFactory.changeBearing(mapBearing));
        aMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(center, zoom, 0, mapBearing)), animateLong, null);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dataContext = new DataContext(getContext());
        animateLong = dataContext.getSetting(Setting.KEYS.map_animateLong, 100).getInt();

        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        carPoint = new Point(displayMetrics.widthPixels / 2, displayMetrics.heightPixels / 5 * 4);
        centerPoint = new Point(displayMetrics.widthPixels / 2, displayMetrics.heightPixels / 2);


        // 传感器管理器
        SensorManager sm = (SensorManager) getContext().getSystemService(SENSOR_SERVICE);
        // 注册传感器(Sensor.TYPE_ORIENTATION(方向传感器);SENSOR_DELAY_FASTEST(0毫秒延迟);
        // SENSOR_DELAY_GAME(20,000毫秒延迟)、SENSOR_DELAY_UI(60,000毫秒延迟))
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_FASTEST);

//        //初始化定位
        locationClient = new AMapLocationClient(getContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_amap, container, false);
        try {
            uiHandler = new Handler();


            // 初始化view
            initView(savedInstanceState, view);

            // 初始化地图

            initMap();

        } catch (Exception e) {
            _Utils.printException(getContext(), e);
        }
        return view;
    }

    public void getLastLocation(final WhenGetLastLocationListener whenGetLastLocationListener) {
        if (lastLocation == null) {
            //初始化client
            final AMapLocationClient locationClient = new AMapLocationClient(getContext());
            AMapLocationClientOption locationOption = new AMapLocationClientOption();
            locationOption.setOnceLocation(true);//可选，设置是否单次定位。默认是false
            locationOption.setOnceLocationLatest(true);//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
            locationClient.setLocationOption(locationOption);

            // 设置定位监听
            locationClient.setLocationListener(new AMapLocationListener() {
                @Override
                public void onLocationChanged(AMapLocation location) {
                    if (location == null || (location != null && location.getErrorCode() == 0)) {
                        location = locationClient.getLastKnownLocation();
                    }
                    if (location != null) {
                        whenGetLastLocationListener.whenGetLastLocation(new LatLng(location.getLatitude(), location.getLongitude()));
                    }
                }
            });
            locationClient.startLocation();
        } else {
            whenGetLastLocationListener.whenGetLastLocation(lastLocation);
        }
    }


    private void hideOptionButton() {
        imageToilet.setVisibility(View.GONE);
        imageHeat.setVisibility(View.GONE);
        imageGas.setVisibility(View.GONE);
    }

    private void showOptionButton() {
        imageToilet.setVisibility(View.VISIBLE);
        imageHeat.setVisibility(View.VISIBLE);
        imageGas.setVisibility(View.VISIBLE);
    }

    /**
     * 初始化view
     *
     * @param savedInstanceState
     * @param view
     */
    private void initView(Bundle savedInstanceState, View view) {

        // Demo中为了其他界面可以使用下载的离线地图，使用默认位置存储，屏蔽了自定义设置
        MapsInitializer.sdcardDir = OffLineMapUtils.getSdCacheDir(getContext());

        textureMapView = view.findViewById(R.id.map);
        textureMapView.onCreate(savedInstanceState);// 此方法必须重写


        textViewGear = (TextView) view.findViewById(R.id.textView_gear);
        if (dataContext.getSetting(Setting.KEYS.map_location_is_opened, false).getBoolean() == true) {
            textViewGear.setVisibility(View.VISIBLE);
            int gear = dataContext.getSetting(Setting.KEYS.map_location_gear, 1).getInt();
            textViewGear.setText(_Session.GEAR_NAMES[gear]);
        } else {
            textViewGear.setVisibility(View.GONE);
        }

        imageNavi = (ImageView) view.findViewById(R.id.image_navi);
        imageNavi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                startActivity(new Intent(getContext(), IntelligentBroadcastActivity.class));
            }
        });

        imageZoomIn = (ImageView) view.findViewById(R.id.image_zoomIn);
        imageZoomIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aMap.animateCamera(CameraUpdateFactory.zoomIn());
            }
        });
        imageZoomOut = (ImageView) view.findViewById(R.id.image_zoomOut);
        imageZoomOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aMap.animateCamera(CameraUpdateFactory.zoomOut());
            }
        });


        imageLocation = (ImageButton) view.findViewById(R.id.image_location);
        imageLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearMultiPoint();
                aMap.clear();
                addLocationMarkers(lastLocation);

                // 地图已被拖动过
                if (isLocationNotCenter) {
                    isLocationNotCenter = false;

                    if (dataContext.getSetting(Setting.KEYS.地图位置显示模式, 0).getInt() == 1) {
                        /**
                         * 行车模式
                         */
                        isMapRotate = true;
                        imageLocation.setImageResource(R.drawable.src_navi);
                        cameriaCenter(lastLocation);
                    } else {
                        /**
                         * 定位模式
                         */
                        isMapRotate = false;
                        imageLocation.setImageResource(R.drawable.src_my_location);
                        aMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(lastLocation, zoom, 0, mapBearing)), animateLong, null);
//                        aMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(lastLocation, zoom, 0, 0)), animateLong, null);
                    }
                }
                // 地图处于跟踪位置
                else {
                    if (dataContext.getSetting(Setting.KEYS.地图位置显示模式, 0).getInt() == 0) {
                        /**
                         * 改行车模式
                         */
                        isMapRotate = true;
                        isNeedFollow = true;

                        imageLocation.setImageResource(R.drawable.src_navi);
                        locationMarker.setIcon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.location_navi_marker)));
                        cameriaCenter(lastLocation);
                        dataContext.editSetting(Setting.KEYS.地图位置显示模式, 1);
                        aMap.setPointToCenter(carPoint.x, carPoint.y);
                        hideOptionButton();

                    } else {

                        /**
                         * 改定位模式
                         */
                        isMapRotate = false;
                        isNeedFollow = false;

                        aMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(lastLocation, zoom, 0, mapBearing)), animateLong, null);
//                        aMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(lastLocation, zoom, 0, 0)), animateLong, null);
                        imageLocation.setImageResource(R.drawable.src_my_location);
                        locationMarker.setIcon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.location_marker)));
                        dataContext.editSetting(Setting.KEYS.地图位置显示模式, 0);
                        aMap.setPointToCenter(centerPoint.x, centerPoint.y);
                        showOptionButton();
                    }
                }

                // 是否可触发定位并显示定位层
                getLastLocation(new WhenGetLastLocationListener() {
                    @Override
                    public void whenGetLastLocation(LatLng lastLatLng) {
                    }
                });
            }
        });
        imageLocation.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return true;
            }
        });

        imageToilet = (ImageView) view.findViewById(R.id.image_toilet);
        imageToilet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSearchQuery("厕所", dataContext.getSetting(Setting.KEYS.map_search_radius_toilet, 1000).getInt());
            }
        });

        imageHeat = (ImageView) view.findViewById(R.id.image_heat);
        imageHeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 插入海量点
                List<Location> locations = dataContext.getLocatiosByDayspan(_Session.UUID_NULL, dataContext.getSetting(Setting.KEYS.location_search_days, 1000).getInt(), true);
                List<MultiPointItem> latLngs = new ArrayList<>();
                Log.e("wangsc", "location size: " + locations.size());
                for (Location location : locations) {
                    latLngs.add(new MultiPointItem(new LatLng(location.Latitude, location.Longitude)));
                }
                insertMultiPoint(latLngs);
            }
        });
        imageHeat.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // 插入热力点
                List<Location> locations = dataContext.getLocatiosByDayspan(_Session.UUID_NULL, dataContext.getSetting(Setting.KEYS.location_search_days, 1000).getInt(), true);
                List<LatLng> latLngs = new ArrayList<>();
                Log.e("wangsc", "location size: " + locations.size());
                for (Location location : locations) {
                    latLngs.add(new LatLng(location.Latitude, location.Longitude));
                }
                insertMapHeat(latLngs);
                return true;
            }
        });

        imageGas = (ImageView) view.findViewById(R.id.image_gas);
        imageGas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSearchQuery("加气站", dataContext.getSetting(Setting.KEYS.map_search_radius_gas, 3000).getInt());
            }
        });

    }

    private void initMap() {
        /*
         * 设置离线地图存储目录，在下载离线地图或初始化地图设置; 使用过程中可自行设置, 若自行设置了离线地图存储的路径，
         * 则需要在离线地图下载和使用地图页面都进行路径设置
         */
        Log.e("wangsc", MapsInitializer.sdcardDir);

        aMap = textureMapView.getMap();
        mUiSettings = aMap.getUiSettings();
        mUiSettings.setScrollGesturesEnabled(true);// 设置地图是否可以手势滑动
        mUiSettings.setZoomGesturesEnabled(true); // 设置地图是否可以手势缩放大小
        mUiSettings.setTiltGesturesEnabled(false);// 设置地图是否可以倾斜
        mUiSettings.setRotateGesturesEnabled(false);// 设置地图是否可以旋转
        mUiSettings.setZoomControlsEnabled(false);// 设置地图是否显示手势缩放
        mUiSettings.setZoomPosition(AMapOptions.ZOOM_POSITION_RIGHT_BUTTOM);// 设置缩放按钮位置
        mUiSettings.setMyLocationButtonEnabled(false);// 设置地图默认的定位按钮是否显示
        mUiSettings.setScaleControlsEnabled(true);// 比例尺
        aMap.setMyLocationEnabled(false);// 是否可触发定位并显示定位层

        aMap.setLoadOfflineData(true);

        LatLng center  = new LatLng(locationClient.getLastKnownLocation().getLatitude(), locationClient.getLastKnownLocation().getLongitude());

        addLocationMarkers(center);

        if (dataContext.getSetting(Setting.KEYS.地图位置显示模式, 0).getInt() == 0) {
            /**
             * 定位模式
             */
            isMapRotate = false;
            imageLocation.setImageResource(R.drawable.src_my_location);
            aMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(center, zoom, 0, 0)));
            aMap.setPointToCenter(centerPoint.x, centerPoint.y);
            showOptionButton();
        } else {
            /**
             * 行车模式
             */
            isMapRotate = true;
            imageLocation.setImageResource(R.drawable.src_navi);
            cameriaCenter(center);
            aMap.setPointToCenter(carPoint.x, carPoint.y);
            hideOptionButton();
        }
//        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE);//连续定位、蓝点不会移动到地图中心点，定位点依照设备方向旋转，并且蓝点会跟随设备移动。
//        myLocationStyle.strokeWidth(0f);
//        myLocationStyle.radiusFillColor(getResources().getColor(R.color.location_accuracy, null));
//        aMap.setMyLocationStyle(myLocationStyle);


//         移动视图中心点到最后一次位置。
        /**
         * 地图事件处理
         */
//        aMap.setOnMyLocationChangeListener(new AMap.OnMyLocationChangeListener() {
//            @Override
//            public void onMyLocationChange(android.location.Location location) {
//                lastLocation = new LatLng(location.getLatitude(), location.getLongitude());
//                northMarker.setPosition(lastLocation);
//                locationMarker.setPosition(lastLocation);
//                Log.e("wangsc", location.getAccuracy() + "");
//                accuracyCircle.setRadius(location.getAccuracy());
//                if (isMapRotate) {
//                    aMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(lastLocation, zoom, 0, location.getBearing())), animateLong, null);
//                    northMarker.setRotateAngle(location.getBearing());
//                }
//                if (isFirst && location != null && dataContext.getSetting(Setting.KEYS.map_is_default_toilet, false).getBoolean() == true) {
//                    doSearchQueryByDefineLocation("厕所", lastLocation, dataContext.getSetting(Setting.KEYS.map_search_radius_toilet, 1000).getInt());
//                    isFirst = false;
//                }
//            }
//        });

        aMap.setOnCameraChangeListener(new AMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                zoom = cameraPosition.zoom;
            }

            @Override
            public void onCameraChangeFinish(CameraPosition cameraPosition) {
            }
        });
        aMap.setOnMapTouchListener(new AMap.OnMapTouchListener() {
            @Override
            public void onTouch(MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 按下屏幕
                        // 如果timer在执行，关掉它
                        if (isMapRotate == false)
                            clearTimer();
                        // 改变跟随状态
                        break;

                    case MotionEvent.ACTION_UP:

                        // 离开屏幕
                        if (dataContext.getSetting(Setting.KEYS.地图位置显示模式, 0).getInt() == 1) {
                            startTimerSomeTimeLater();
                        }
                        break;

                    default:
                        break;
                }
                isMapRotate = false;
                isLocationNotCenter = true;
                isNeedFollow = false;
//                myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);//连续定位、蓝点不会移动到地图中心点，定位点依照设备方向旋转，并且蓝点会跟随设备移动。
//                aMap.setMyLocationStyle(myLocationStyle);
                imageLocation.setImageResource(R.drawable.src_my_not_location);
            }
        });

        aMap.setOnMapLoadedListener(new AMap.OnMapLoadedListener() {
            @Override
            public void onMapLoaded() {
                // 插入海量点
                List<Location> locations = dataContext.getTodayLocatios(_Session.UUID_NULL);
                List<MultiPointItem> latLngs = new ArrayList<>();
                for (Location location : locations) {
                    latLngs.add(new MultiPointItem(new LatLng(location.Latitude, location.Longitude)));
                }
                insertMultiPoint(latLngs);
            }
        });
    }

    private void addLocationMarkers(LatLng center) {
//        northMarker = aMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f).icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.north))).position(center));

        if (dataContext.getSetting(Setting.KEYS.地图位置显示模式, 0).getInt() == 0) {
            locationMarker = aMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f)
                    .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.location_marker))).position(center));
        } else {
            locationMarker = aMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f)
                    .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.location_navi_marker))).position(center));
        }

        accuracyCircle = aMap.addCircle(new CircleOptions().center(center).radius(0).fillColor(getResources().getColor(R.color.location_accuracy)).strokeWidth(0));
    }

    private void moveLocationMarkers(AMapLocation location) {
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
//        northMarker.setPosition(latLng);
        locationMarker.setPosition(latLng);
        accuracyCircle.setCenter(latLng);
        accuracyCircle.setRadius(location.getAccuracy());
    }

    /**
     * 取消timer任务
     */
    private void clearTimer() {
        if (needFollowTimer != null) {
            needFollowTimer.cancel();
            needFollowTimer = null;
        }
    }

    /**
     * 如果地图在静止的情况下
     */
    private void startTimerSomeTimeLater() {
        // 首先关闭上一个timer
        clearTimer();
        needFollowTimer = new Timer();
        // 开启一个延时任务，改变跟随状态
        needFollowTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        isMapRotate = true;
                        imageLocation.setImageResource(R.drawable.src_navi);
                        isNeedFollow = true;
                        isLocationNotCenter = false;
                        cameriaCenter(lastLocation);
//                        aMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(lastLocation, zoom, 0, mapBearing)), animateLong, null);
                    }
                });
            }
        }, DELAY_TIME);
    }

    //region POI搜索

    private Marker mlastMarker;
    private PoiResult poiResult; // poi返回的结果
    private int currentPage = 0;// 当前页面，从0开始计数
    private PoiSearch.Query query;// Poi查询条件类
    private PoiSearch poiSearch;
    private MyPoiOverlay poiOverlay;// poi图层

    /**
     * 开始进行poi搜索。
     *
     * @param keyWord 搜索关键字。
     * @param radius  搜索范围，单位为米。
     */
    protected void doSearchQuery(final String keyWord, final int radius) {
        clearMultiPoint();
        aMap.clear();

        getLastLocation(new WhenGetLastLocationListener() {
            @Override
            public void whenGetLastLocation(final LatLng lastLatLng) {
                if (lastLatLng != null) {
                    currentPage = 0;
                    query = new PoiSearch.Query(keyWord, "", "");// 第一个参数表示搜索字符串，第二个参数表示poi搜索类型，第三个参数表示poi搜索区域（空字符串代表全国）
                    query.setPageSize(20);// 设置每页最多返回多少条poiitem
                    query.setPageNum(currentPage);// 设置查第一页

                    /**
                     *
                     */
                    poiSearch = new PoiSearch(getContext(), query);
                    poiSearch.setOnPoiSearchListener(new PoiSearch.OnPoiSearchListener() {
                        @Override
                        public void onPoiSearched(PoiResult result, int rcode) {
                            poiSearched(result, rcode, lastLatLng, radius);
                        }

                        @Override
                        public void onPoiItemSearched(PoiItem poiItem, int i) {

                        }
                    });
                    poiSearch.setBound(new PoiSearch.SearchBound(AMapUtil.convertToLatLonPoint(lastLatLng), radius, true));//
                    // 设置搜索区域为以lp点为圆心，其周围5000米范围
                    poiSearch.searchPOIAsyn();// 异步搜索
                }
            }
        });

    }


    /**
     * 开始进行poi搜索。
     *
     * @param keyWord 搜索关键字。
     * @param radius  搜索范围，单位为米。
     */
    protected void doSearchQueryByDefineLocation(final String keyWord, final LatLng center, final int radius) {
        clearMultiPoint();
        aMap.clear();

        if (center != null) {
            currentPage = 0;
            query = new PoiSearch.Query(keyWord, "", "");// 第一个参数表示搜索字符串，第二个参数表示poi搜索类型，第三个参数表示poi搜索区域（空字符串代表全国）
            query.setPageSize(20);// 设置每页最多返回多少条poiitem
            query.setPageNum(currentPage);// 设置查第一页

            /**
             *
             */
            poiSearch = new PoiSearch(getContext(), query);
            poiSearch.setOnPoiSearchListener(new PoiSearch.OnPoiSearchListener() {
                @RequiresApi(api = Build.VERSION_CODES.M)
                @Override
                public void onPoiSearched(PoiResult result, int rcode) {
                    poiSearched(result, rcode, center, radius);
                }

                @Override
                public void onPoiItemSearched(PoiItem poiItem, int i) {

                }
            });
            poiSearch.setBound(new PoiSearch.SearchBound(AMapUtil.convertToLatLonPoint(center), radius, true));//
            // 设置搜索区域为以lp点为圆心，其周围5000米范围
            poiSearch.searchPOIAsyn();// 异步搜索
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void poiSearched(PoiResult result, int rcode, final LatLng center, int radius) {
        //清除POI信息显示
//        whetherToShowDetailInfo(false);
        //并还原点击marker样式
        if (mlastMarker != null) {
            resetlastmarker();
        }
        //清理之前搜索结果的marker
        if (poiOverlay != null) {
            poiOverlay.removeFromMap();
        }
        // 清理旧的范围灰圈
        if (searchRangeCircle != null) {
            searchRangeCircle.remove();
        }
        if (rcode == AMapException.CODE_AMAP_SUCCESS) {
            if (result != null && result.getQuery() != null) {// 搜索poi的结果
                if (result.getQuery().equals(query)) {// 是否是同一条
                    poiResult = result;
                    List<PoiItem> poiItems = poiResult.getPois();// 取得第一页的poiitem数据，页数从数字0开始
                    List<SuggestionCity> suggestionCities = poiResult.getSearchSuggestionCitys();// 当搜索不到poiitem数据时，会返回含有搜索关键字的城市信息
                    if (poiItems != null && poiItems.size() > 0) {
                        poiOverlay = new MyPoiOverlay(aMap, poiItems);
                        poiOverlay.addToMap();
                        poiOverlay.zoomToSpan();


                    } else if (
                            suggestionCities != null && suggestionCities.size() > 0) {
                        String res = "建议查询城市" + suggestionCities.toString();
                    } else {
                        Toast.makeText(getContext(), "查询结果为空", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(getContext(), "查询结果为空", Toast.LENGTH_SHORT).show();
            }
        } else {
            dataContext.addRunLog("err", "查询错误码：" + rcode);

            Toast.makeText(getContext(), "查询错误码：" + rcode, Toast.LENGTH_SHORT).show();
        }
//        myLocationMarker = aMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f).icon(locationIcon).position(center));
//        aMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(center, 15, 0, 0)), animateLong, null);
        adjustCamera(center, radius);
        searchRangeCircle = aMap.addCircle(new CircleOptions().center(center).radius(radius).fillColor(getResources().getColor(R.color.range_overlay, null)).strokeWidth(0));
        addLocationMarkers(center);
    }


    // 将之前被点击的marker置为原来的状态
    private void resetlastmarker() {
        int index = poiOverlay.getPoiIndex(mlastMarker);
        if (index < 10) {
            mlastMarker.setIcon(BitmapDescriptorFactory
                    .fromBitmap(BitmapFactory.decodeResource(
                            getResources(),
                            markers[index])));
        } else {
            mlastMarker.setIcon(BitmapDescriptorFactory.fromBitmap(
                    BitmapFactory.decodeResource(getResources(), R.drawable.marker_other_highlight)));
        }
        mlastMarker = null;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.showInfoWindow();
        return false;
    }

    @Override
    public View getInfoWindow(final Marker marker) {
        View view = View.inflate(getContext(), R.layout.poikeywordsearch_uri, null);
        TextView title = (TextView) view.findViewById(R.id.title);
        title.setText(marker.getTitle());

        TextView snippet = (TextView) view.findViewById(R.id.snippet);
        snippet.setText(marker.getSnippet());
        ImageButton button = (ImageButton) view.findViewById(R.id.start_amap_app);
        // 调起高德地图app
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAMapNavi(marker);
            }
        });
        return view;
    }

    @Override
    public View getInfoContents(Marker marker) {
        return null;
    }

    /**
     * 调起高德地图导航功能，如果没安装高德地图，会进入异常，可以在异常中处理，调起高德地图app的下载页面
     */
    public void startAMapNavi(Marker marker) {
        // 构造导航参数
        NaviPara naviPara = new NaviPara();
        // 设置终点位置
        naviPara.setTargetPoint(marker.getPosition());
        // 设置导航策略，这里是距离最短
        naviPara.setNaviStyle(NaviPara.DRIVING_SHORT_DISTANCE);

        // 调起高德地图导航
        try {

            AMapUtils.openAMapNavi(naviPara, getContext().getApplicationContext());
        } catch (com.amap.api.maps.AMapException e) {

            // 如果没安装会进入异常，调起下载页面
            AMapUtils.getLatestAMapApp(getContext().getApplicationContext());

        }

    }

    /**
     * 自定义PoiOverlay
     */
    private class MyPoiOverlay {
        private AMap mamap;
        private List<PoiItem> mPois;
        private ArrayList<Marker> mPoiMarks = new ArrayList<Marker>();

        public MyPoiOverlay(AMap amap, List<PoiItem> pois) {
            mamap = amap;
            mPois = pois;
        }

        /**
         * 添加Marker到地图中。
         *
         * @since V2.1.0
         */
        public void addToMap() {
            for (int i = 0; i < mPois.size(); i++) {
                Marker marker = mamap.addMarker(getMarkerOptions(i));
                PoiItem item = mPois.get(i);
                marker.setObject(item);
                mPoiMarks.add(marker);
            }
        }

        /**
         * 去掉PoiOverlay上所有的Marker。
         *
         * @since V2.1.0
         */
        public void removeFromMap() {
            for (Marker mark : mPoiMarks) {
                mark.remove();
            }
        }

        /**
         * 移动镜头到当前的视角。
         *
         * @since V2.1.0
         */
        public void zoomToSpan() {
            if (mPois != null && mPois.size() > 0) {
                if (mamap == null)
                    return;
                LatLngBounds bounds = getLatLngBounds();
                mamap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));
            }
        }

        private LatLngBounds getLatLngBounds() {
            LatLngBounds.Builder b = LatLngBounds.builder();
            for (int i = 0; i < mPois.size(); i++) {
                b.include(new LatLng(mPois.get(i).getLatLonPoint().getLatitude(),
                        mPois.get(i).getLatLonPoint().getLongitude()));
            }
            return b.build();
        }

        private MarkerOptions getMarkerOptions(int index) {
            return new MarkerOptions()
                    .position(new LatLng(mPois.get(index).getLatLonPoint()
                            .getLatitude(), mPois.get(index)
                            .getLatLonPoint().getLongitude()))
                    .title(getTitle(index)).snippet(getSnippet(index))
                    .icon(getBitmapDescriptor(index));
        }

        protected String getTitle(int index) {
            return mPois.get(index).getTitle();
        }

        protected String getSnippet(int index) {
            return mPois.get(index).getSnippet();
        }

        /**
         * 从marker中得到poi在list的位置。
         *
         * @param marker 一个标记的对象。
         * @return 返回该marker对应的poi在list的位置。
         * @since V2.1.0
         */
        public int getPoiIndex(Marker marker) {
            for (int i = 0; i < mPoiMarks.size(); i++) {
                if (mPoiMarks.get(i).equals(marker)) {
                    return i;
                }
            }
            return -1;
        }

        /**
         * 返回第index的poi的信息。
         *
         * @param index 第几个poi。
         * @return poi的信息。poi对象详见搜索服务模块的基础核心包（com.amap.api.services.core）中的类 <strong><a href="../../../../../../Search/com/amap/api/services/core/PoiItem.html" title="com.amap.api.services.core中的类">PoiItem</a></strong>。
         * @since V2.1.0
         */
        public PoiItem getPoiItem(int index) {
            if (index < 0 || index >= mPois.size()) {
                return null;
            }
            return mPois.get(index);
        }

        /**
         * 给第几个Marker设置图标，并返回更换图标的图片。如不用默认图片，需要重写此方法。
         *
         * @param arg0 第几个Marker。
         * @return 更换的Marker图片。
         * @since V2.1.0
         */
        protected BitmapDescriptor getBitmapDescriptor(int arg0) {
            if (arg0 < 10) {
                BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(
                        BitmapFactory.decodeResource(getResources(), markers[arg0]));
                return icon;
            } else {
                BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.marker_other_highlight));
                return icon;
            }
        }
    }

    private int[] markers = {
            R.drawable.poi_marker_1,
            R.drawable.poi_marker_2,
            R.drawable.poi_marker_3,
            R.drawable.poi_marker_4,
            R.drawable.poi_marker_5,
            R.drawable.poi_marker_6,
            R.drawable.poi_marker_7,
            R.drawable.poi_marker_8,
            R.drawable.poi_marker_9,
            R.drawable.poi_marker_10
    };

    /**
     * 调节地图到正好放置查询范围的所有点 * @param centerLatLng 中心点 * @param radius 查询范围（米）
     */
    private void adjustCamera(LatLng centerLatLng, int radius) {
        //当前缩放级别下的比例尺
        //"每像素代表" + scale + "米"
        float scale = aMap.getScalePerPixel();
        //radius（米）的像素数量
        int pixel = Math.round(radius / scale);
        //小范围，小缩放级别（比例尺较大），有精度损失
        Projection projection = aMap.getProjection();
        //将地图的中心点，转换为屏幕上的点
        Point center = projection.toScreenLocation(centerLatLng);
        //获取距离中心点为pixel像素的左、右两点（屏幕上的点
        Point right = new Point(center.x + pixel, center.y);
        Point left = new Point(center.x - pixel, center.y);

        //将屏幕上的点转换为地图上的点
        LatLng rightLatlng = projection.fromScreenLocation(right);
        LatLng LeftLatlng = projection.fromScreenLocation(left);

        //调整可视范围
        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.builder().include(rightLatlng).include(LeftLatlng).build(), 10), animateLong, null);
    }

    /**
     * 调节地图到正好放置查询范围的所有点 * @param centerLatLng 中心点 * @param radius 查询范围（米）
     */
    private void adjustAllMarker1(List<MultiPointItem> latLngs) {

        if (latLngs.size() > 0) {
            LatLng topLatlng = latLngs.get(0).getLatLng(),
                    bottomLatlng = latLngs.get(0).getLatLng(),
                    leftLatlng = latLngs.get(0).getLatLng(),
                    rightLatlng = latLngs.get(0).getLatLng();

            for (MultiPointItem mpi : latLngs) {
                LatLng latLng = mpi.getLatLng();
                if (latLng.latitude > topLatlng.latitude) {
                    topLatlng = latLng;
                }
                if (latLng.latitude < bottomLatlng.latitude) {
                    bottomLatlng = latLng;
                }
                if (latLng.longitude > leftLatlng.longitude) {
                    leftLatlng = latLng;
                }
                if (latLng.longitude < rightLatlng.longitude) {
                    rightLatlng = latLng;
                }
            }

            //调整可视范围
            aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.builder().include(rightLatlng).include(leftLatlng).include(topLatlng).include(bottomLatlng).build(), 10), animateLong, null);
        }
    }

    /**
     * 调节地图到正好放置查询范围的所有点 * @param centerLatLng 中心点 * @param radius 查询范围（米）
     */
    private void adjustAllMarker2(List<LatLng> latLngs) {

        if (latLngs.size() > 0) {
            LatLng topLatlng = latLngs.get(0),
                    bottomLatlng = latLngs.get(0),
                    leftLatlng = latLngs.get(0),
                    rightLatlng = latLngs.get(0);

            for (LatLng latLng : latLngs) {
                if (latLng.latitude > topLatlng.latitude) {
                    topLatlng = latLng;
                }
                if (latLng.latitude < bottomLatlng.latitude) {
                    bottomLatlng = latLng;
                }
                if (latLng.longitude > leftLatlng.longitude) {
                    leftLatlng = latLng;
                }
                if (latLng.longitude < rightLatlng.longitude) {
                    rightLatlng = latLng;
                }
            }

            //调整可视范围
            aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds.builder().include(rightLatlng).include(leftLatlng).include(topLatlng).include(bottomLatlng).build(), 10), animateLong, null);
        }
    }

    //endregion

    //region 海量点
    private void insertMultiPoint(final List<MultiPointItem> list) {
        try {
            aMap.clear();
            clearMultiPoint();

            if (list.size() != 0) {
                //添加一个Marker用来展示海量点点击效果
                final Marker marker = aMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

                BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory.fromResource(R.drawable.marker_blue);

                MultiPointOverlayOptions overlayOptions = new MultiPointOverlayOptions();
                overlayOptions.icon(bitmapDescriptor);
                overlayOptions.anchor(0.5f, 0.5f);

                if (multiPointOverlay == null) {
                    multiPointOverlay = aMap.addMultiPointOverlay(overlayOptions);
                }
                aMap.setOnMultiPointClickListener(new AMap.OnMultiPointClickListener() {
                    @Override
                    public boolean onPointClick(MultiPointItem pointItem) {
                        android.util.Log.e("wangsc", "onPointClick");

                        //添加一个Marker用来展示海量点点击效果
                        marker.setPosition(pointItem.getLatLng());
                        marker.setToTop();
                        return false;
                    }
                });

                if (multiPointOverlay != null && list != null) {
                    multiPointOverlay.setItems(list);
                    multiPointOverlay.setEnable(true);
                }
            }
            // 移动视图
            getLastLocation(new WhenGetLastLocationListener() {
                @Override
                public void whenGetLastLocation(LatLng lastLatLng) {
                    addLocationMarkers(lastLatLng);
                    adjustAllMarker1(list);
                }
            });
        } catch (Exception e) {
            _Utils.printException(getContext(), e);
        }
    }

    private void clearMultiPoint() {
        if (multiPointOverlay == null)
            return;
        multiPointOverlay.setEnable(false);
    }
    //endregion

    //region 热力图

    /**
     * 向地图中插入热力点。
     */
    private void insertMapHeat(final List<LatLng> latlngs) {
        aMap.clear();
        clearMultiPoint();
        if (tileOverlay != null) {
            tileOverlay.remove();
        }
        if (latlngs.size() != 0) {

            Gradient ALT_HEATMAP_GRADIENT = new Gradient(
                    ALT_HEATMAP_GRADIENT_COLORS, ALT_HEATMAP_GRADIENT_START_POINTS);
            // 第二步： 构建热力图 构造热力图对象 TileProvider
            HeatmapTileProvider heatmapTileProvider = new HeatmapTileProvider.Builder().
                    data(latlngs)// 设置热力图绘制的数据
                    .gradient(ALT_HEATMAP_GRADIENT)// 设置热力图渐变，有默认值 DEFAULT_GRADIENT，可不设置该接口, Gradient 的设置可见参考手册
                    .build();

            // 第三步： 构建热力图参数对象
            TileOverlayOptions tileOverlayOptions = new TileOverlayOptions();
            tileOverlayOptions.tileProvider(heatmapTileProvider); // 设置瓦片图层的提供者

            // 第四步： 添加热力图
            tileOverlay = aMap.addTileOverlay(tileOverlayOptions);
        }
        // 移动视图
        getLastLocation(new WhenGetLastLocationListener() {
            @Override
            public void whenGetLastLocation(LatLng lastLatLng) {
                addLocationMarkers(lastLatLng);
                adjustAllMarker2(latlngs);
            }
        });
    }

    //endregion

    //region 罗盘定向

    private float currentDegree = 0f;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
            float degree = event.values[0];

            /*
            RotateAnimation类：旋转变化动画类

            参数说明:
            fromDegrees：旋转的开始角度。
            toDegrees：旋转的结束角度。
            pivotXType：X轴的伸缩模式，可以取值为ABSOLUTE、RELATIVE_TO_SELF、RELATIVE_TO_PARENT。
            pivotXValue：X坐标的伸缩值。
            pivotYType：Y轴的伸缩模式，可以取值为ABSOLUTE、RELATIVE_TO_SELF、RELATIVE_TO_PARENT。
            pivotYValue：Y坐标的伸缩值
            */
            if (!isMapRotate) {
                rotateMarker(locationMarker, -degree, animateLong);
//                com.amap.api.maps.model.animation.RotateAnimation ra = new com.amap.api.maps.model.animation.RotateAnimation(locationMarker.getRotateAngle(), -degree);
//                ra.setDuration(50);
//                if (locationMarker != null) {
//                    locationMarker.setAnimation(ra);
//                    locationMarker.startAnimation();
//                }
            }
        }
    }


    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //endregion

    private void rotateMarker(Marker target, float degree, long animateLong) {
        com.amap.api.maps.model.animation.RotateAnimation ra = new com.amap.api.maps.model.animation.RotateAnimation(target.getRotateAngle(), degree);
        ra.setDuration(animateLong);
        if (target != null) {
            target.setAnimation(ra);
            target.startAnimation();
        }
    }

    private void startLocation() {

        Log.e("wangsc", "AmapFragment.startLocation()");
        //初始化client
        locationClient = new AMapLocationClient(getContext());
        if (locationOption == null) {
            locationOption = new AMapLocationClientOption();
        }
        locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
        locationOption.setGpsFirst(false);//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
        locationOption.setHttpTimeOut(30000);//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
        locationOption.setInterval(1000);//可选，设置定位间隔。默认为2秒
        locationOption.setNeedAddress(true);//可选，设置是否返回逆地理地址信息。默认是true
        AMapLocationClientOption.setLocationProtocol(AMapLocationClientOption.AMapLocationProtocol.HTTP);//可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
        locationOption.setSensorEnable(false);//可选，设置是否使用传感器。默认是false
        locationOption.setWifiScan(true); //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
        locationOption.setLocationCacheEnable(true); //可选，设置是否使用缓存定位，默认为true
        locationOption.setOnceLocation(false);//可选，设置是否单次定位。默认是false
        locationOption.setOnceLocationLatest(false);//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
        locationClient.setLocationOption(locationOption);

        // 设置定位监听
        locationClient.setLocationListener(new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation location) {
                if (location != null && location.getErrorCode() == 0) {
                    lastLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    mapBearing = location.getBearing();
                    moveLocationMarkers(location);

                    if (isNeedFollow) {
                        // 旋转地图
                        if (isMapRotate) {
                            if (location.getSpeed() != 0) {
                                cameriaCenter(lastLocation);
//                                rotateMarker(northMarker, mapBearing, 0);
                                if (locationMarker.getRotateAngle() != 0f) {
                                    rotateMarker(locationMarker, 0f, 0);
                                }
                            }
                        } else {
                            aMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition(lastLocation, zoom, 0, mapBearing)), animateLong, null);
                        }
                    }
                }
            }
        });
        locationClient.startLocation();
    }

    private void stopLocation() {
        if (locationClient != null) {
            locationClient.stopLocation();
            locationClient.onDestroy();
            locationClient = null;
        }
    }

    @Subscribe
    public void onEventMainThread(AmapGearEvent event) {
        textViewGear.setText(_Session.GEAR_NAMES[event.gear]);
    }

    /**
     * 方法必须重写
     */
    @Override
    public void onResume() {
        super.onResume();
        textureMapView.onResume();
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        startLocation();
    }

    /**
     * 方法必须重写
     */
    @Override
    public void onPause() {
        super.onPause();
        textureMapView.onPause();
        stopLocation();
    }

    /**
     * 方法必须重写
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        textureMapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        textureMapView.onDestroy();
    }

    public interface WhenGetLastLocationListener {
        void whenGetLastLocation(LatLng lastLatLng);
    }
}
