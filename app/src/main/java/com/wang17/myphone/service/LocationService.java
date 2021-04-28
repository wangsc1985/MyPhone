package com.wang17.myphone.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.IntDef;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.model.LatLng;
import com.wang17.myphone.event.AmapGearEvent;
import com.wang17.myphone.event.LocationGearEvent;
import com.wang17.myphone.event.LocationIsAutomaticEvent;
import com.wang17.myphone.util.AMapUtil;
import com.wang17.myphone.database.DataContext;
import com.wang17.myphone.util._Session;
import com.wang17.myphone.database.Location;
import com.wang17.myphone.database.Setting;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

public class LocationService extends Service {

    private AMapLocationClient locationClient = null;
    private AMapLocationClientOption locationOption = null;
    private DataContext dataContext;
    private boolean isAutomatic;
    private int count;
    private int gear;

    private LatLng lastLatLng;

    //region 注解静态类替换枚举类型

    private static final int EVENT_BUS = 748;
    private static final int AUTO = 619;
    private static final int AUTO_TRANSIENT = 946;

    @IntDef({EVENT_BUS, AUTO, AUTO_TRANSIENT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface EventType {
    }

    //endregion

    class Gear {
        int gear;
        long speed;

        public Gear(int gear, long speed) {
            this.gear = gear;
            this.speed = speed;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        dataContext = new DataContext(getApplicationContext());
        isAutomatic = dataContext.getSetting(Setting.KEYS.map_location_isAutoChangeGear, false).getBoolean();
        gear = dataContext.getSetting(Setting.KEYS.map_location_gear, 0).getInt();
        dataContext.addRunLog("定位服务", "onCreate()");
        EventBus.getDefault().register(this);
        startSaveLocation();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        dataContext.addRunLog("定位服务", "onStartCommand()");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * 初始化定位
     *
     * @author hongming.wang
     * @since 2.8.0
     */
    private void startSaveLocation() {
        /**
         * 1、配置定位参数。
         * 2、设置监听器。
         * 3、开始定位。
         */

        //初始化client
        if (locationClient == null) {
            locationClient = new AMapLocationClient(getApplicationContext());
        }

        // 配置定位参数
        boolean isOnceLocation = false;
        gear = dataContext.getSetting(Setting.KEYS.map_location_gear, 1).getInt();
        if (locationOption == null) {
            locationOption = new AMapLocationClientOption();
        }
        locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
        locationOption.setGpsFirst(true);//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
        locationOption.setHttpTimeOut(30000);//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
        locationOption.setInterval(_Session.GEARS[gear] * 1000);//可选，设置定位间隔。默认为2秒
        locationOption.setNeedAddress(true);//可选，设置是否返回逆地理地址信息。默认是true
        AMapLocationClientOption.setLocationProtocol(AMapLocationClientOption.AMapLocationProtocol.HTTP);//可选， 设置网络请求的协议。可选HTTP或者HTTPS。默认为HTTP
        locationOption.setSensorEnable(false);//可选，设置是否使用传感器。默认是false
        locationOption.setWifiScan(true); //可选，设置是否开启wifi扫描。默认为true，如果设置为false会同时停止主动刷新，停止以后完全依赖于系统刷新，定位位置可能存在误差
        locationOption.setLocationCacheEnable(true); //可选，设置是否使用缓存定位，默认为true
        locationOption.setOnceLocation(isOnceLocation);//可选，设置是否单次定位。默认是false
        locationOption.setOnceLocationLatest(isOnceLocation);//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用

        //设置定位参数
        locationClient.setLocationOption(locationOption);

        // 设置定位监听
        locationClient.setLocationListener(new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation location) {

                if (null != location) {
                    StringBuffer sb = new StringBuffer();
                    //errCode等于0代表定位成功，其他的为定位失败，具体的可以参照官网定位错误码说明
                    if (location.getErrorCode() == 0) {
                        if (location.getAccuracy() > 50) {
                            dataContext.addRunLog("定位 - 精度过低", "当前定位精度：" + location.getAccuracy() + "米。");
                            return;
                        }
                        Location l = new Location();
                        l.Id = UUID.randomUUID();
                        l.UserId = _Session.UUID_NULL;
                        l.Time = location.getTime();
                        l.Accuracy = location.getAccuracy();
                        l.AdCode = location.getAdCode();
                        l.Address = location.getAddress();
                        l.Bearing = location.getBearing();
                        l.City = location.getCity();
                        l.CityCode = location.getCityCode();
                        l.Province = location.getProvince();
                        l.Country = location.getCountry();
                        l.District = location.getDistrict();
                        l.LocationType = location.getLocationType();
                        l.Longitude = location.getLongitude();
                        l.PoiName = location.getPoiName();
                        l.Latitude = location.getLatitude();
                        l.Provider = location.getProvider();
                        l.Speed = location.getSpeed();
                        l.Satellites = location.getSatellites();
                        dataContext.addLocation(l);

                        LatLng currentLatLng = new LatLng(l.Latitude, l.Longitude);


                        if (lastLatLng == null)
                            lastLatLng = currentLatLng;

                        float distance = AMapUtils.calculateLineDistance(lastLatLng, currentLatLng);
                        dataContext.addRunLog("定位 - " + gear + "档，" + _Session.GEAR_NAMES[gear] + "，：" + count, AMapUtil.locationToString(location) + +_Session.GEARS[gear] + "秒/次   移动：" + distance + "米");


                        /**
                         * 换挡操作
                         */
                        if (isAutomatic) {
                            /**
                             * 等待
                             */
                            if (gear != 0 && l.Speed == 0f) {
                                // 速度为零，并且两次定位在同一位置（也就是没有移动）。如果虽然都是速度为零，但是位置已经更改，说明已经移动了，只是恰好这两次中间的时间内没有调用定位，而在两个位置停止状态调用的定位。
                                if (lastLatLng != null && location.getAccuracy() <= 10 && distance > 20) {
                                    count = 0;
                                }
                                if (count > 20) {
                                    changeGear(0, AUTO);
                                } else if (count == 1) {
                                    changeGear(1, AUTO_TRANSIENT);
                                }
                            }
                            /**
                             * 步行
                             */
                            else if (gear != 1 && l.Speed > 0.5f && l.Speed <= 2f) {
                                changeGear(1, AUTO);
                                count = 0;
                            }
                            /**
                             * 单车
                             */
                            else if (gear != 2 && l.Speed > 2f && l.Speed <= 5f) {
                                changeGear(2, AUTO);
                                count = 0;
                            }
                            /**
                             * 汽车
                             */
                            else if (gear != 3 && l.Speed > 10f && l.Speed <= 20f) {
                                changeGear(3, AUTO);
                                count = 0;
                            }
                            /**
                             * 高速
                             */
                            else if (gear != 4 && l.Speed > 20f) {
                                changeGear(4, AUTO);
                                count = 0;
                            }
   /*                            else if (gear != 5 && l.Speed > 20f && l.Speed <= 30f) {
                                changeGear(5, AUTO);
                                count = 0;
                            } else if (gear != 6 && l.Speed > 30f) {
                                changeGear(6, AUTO);
                                count = 0;
                            }*/
                        }
                        /**
                         *
                         */
                        lastLatLng = currentLatLng;
                    } else {
                        //定位失败
                        sb.append("定位失败" + "\n");
                        sb.append("错误码:" + location.getErrorCode() + "\n");
                        sb.append("错误信息:" + location.getErrorInfo() + "\n");
                        sb.append("错误描述:" + location.getLocationDetail() + "\n");
                        dataContext.addRunLog("定位失败", sb.toString());
                    }
                } else {
                    dataContext.addRunLog("定位失败", "location is null");
                }
            }
        });
        locationClient.startLocation();
    }

    private void changeGear(int gear, @EventType int changeType) {
        this.gear = gear;
        locationOption.setInterval(_Session.GEARS[gear] * 1000);
        locationClient.setLocationOption(locationOption);
        dataContext.editSetting(Setting.KEYS.map_location_gear, gear);
        switch (changeType) {
            case EVENT_BUS:
                count = 0;
                dataContext.addRunLog("定位 - EventBus换挡： " + gear, _Session.GEARS[gear] + "秒钟定位一次");
                break;
            case AUTO_TRANSIENT:
                dataContext.addRunLog("定位 - 暂停挡 " + gear, _Session.GEARS[gear] + "秒钟定位一次");
                break;
            case AUTO:
                dataContext.addRunLog("定位 - 自动换挡 " + gear, _Session.GEARS[gear] + "秒钟定位一次");
        }
        EventBus.getDefault().post(new AmapGearEvent(gear));
    }

    @Subscribe
    public void onEventMainThread(LocationGearEvent event) {
        if (gear != event.gear) {
            changeGear(event.gear, EVENT_BUS);
        }

    }

    @Subscribe
    public void onEventMainThread(LocationIsAutomaticEvent event) {
        if (isAutomatic != event.isAutomatic) {
            dataContext.addRunLog("定位EventBus手自切换", "自动挡：" + event.isAutomatic);
            isAutomatic = event.isAutomatic;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        if (locationClient != null)
            locationClient.stopLocation();
    }
}
