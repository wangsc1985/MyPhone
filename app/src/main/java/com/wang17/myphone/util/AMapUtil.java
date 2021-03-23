package com.wang17.myphone.util;

/**
 *
 */

import android.content.Context;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.model.LatLng;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusPath;
import com.amap.api.services.route.BusStep;
import com.amap.api.services.route.DistanceResult;
import com.amap.api.services.route.DistanceSearch;
import com.amap.api.services.route.RouteBusLineItem;
import com.amap.api.services.route.RouteRailwayItem;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import java.util.Locale;
import java.util.UUID;

import com.wang17.myphone.R;
import com.wang17.myphone.database.Location;

public class AMapUtil {

    public static void getCurrentLocation(final Context context, final String summary) {
        //初始化client
        final AMapLocationClient locationClient = new AMapLocationClient(context);
        AMapLocationClientOption locationOption = new AMapLocationClientOption();
        locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
        locationOption.setGpsFirst(true);//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
        locationOption.setHttpTimeOut(30000);//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
        locationOption.setOnceLocation(true);//可选，设置是否单次定位。默认是false
        locationOption.setOnceLocationLatest(true);//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
        locationClient.setLocationOption(locationOption);

        // 设置定位监听
        locationClient.setLocationListener(new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation location) {
                DataContext dataContext = new DataContext(context);
                if (null != location) {
                    StringBuffer sb = new StringBuffer();
                    //errCode等于0代表定位成功，其他的为定位失败，具体的可以参照官网定位错误码说明
                    if (location.getErrorCode() == 0) {
//                        if (location.getAccuracy() > 50) {
//                            dataContext.addRunLog2File("定位 - 精度过低", "当前定位精度：" + location.getAccuracy() + "米。");
//                            return;
//                        }
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
                        l.Summary = summary;
                        if (l.Accuracy <= 100) {
                            dataContext.addLocation(l);
                        }
                    } else {
//                        dataContext.addRunLog2File("定位 - 错误码：" + location.getErrorCode() + "。","");
                    }
                } else {
//                    dataContext.addRunLog2File("定位 - 没有定位信息。","");
                }
            }
        });
        locationClient.startLocation();
    }

    public static void getCurrentLocation(final Context context, final String summary, final LocationCallBack callBack) {
        //初始化client
        final AMapLocationClient locationClient = new AMapLocationClient(context);
        AMapLocationClientOption locationOption = new AMapLocationClientOption();
        locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy);//可选，设置定位模式，可选的模式有高精度、仅设备、仅网络。默认为高精度模式
        locationOption.setGpsFirst(true);//可选，设置是否gps优先，只在高精度模式下有效。默认关闭
        locationOption.setHttpTimeOut(30000);//可选，设置网络请求超时时间。默认为30秒。在仅设备模式下无效
        locationOption.setOnceLocation(true);//可选，设置是否单次定位。默认是false
        locationOption.setOnceLocationLatest(true);//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
        locationClient.setLocationOption(locationOption);

        // 设置定位监听
        locationClient.setLocationListener(new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation location) {
                DataContext dataContext = new DataContext(context);
                if (null != location) {
                    StringBuffer sb = new StringBuffer();
                    //errCode等于0代表定位成功，其他的为定位失败，具体的可以参照官网定位错误码说明
                    if (location.getErrorCode() == 0) {
//                        if (location.getAccuracy() > 50) {
//                            dataContext.addRunLog2File("定位 - 精度过低", "当前定位精度：" + location.getAccuracy() + "米。");
//                            return;
//                        }
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
                        l.Summary = summary;
                        Log.e("wangsc", "Accuracy: " + l.Accuracy);
                        if (l.Accuracy <= 100) {
                            if (callBack != null) {
                                callBack.OnLocationedListener(l);
                            }
                        }
                    } else {
//                        dataContext.addRunLog2File("定位 - 错误码：" + location.getErrorCode() + "。","");
                    }
                } else {
//                    dataContext.addRunLog2File("定位 - 没有定位信息。","");
                }
            }
        });
        locationClient.startLocation();
    }

    public interface LocationCallBack {
        void OnLocationedListener(Location location);
    }

    public static void getDistants(Context context, final Location start, final Location end, final DistanceSearchCallback callback) {
        try {
            DistanceSearch distanceSearch = new DistanceSearch(context);
            distanceSearch.setDistanceSearchListener(new DistanceSearch.OnDistanceSearchListener() {
                @Override
                public void onDistanceSearched(DistanceResult distanceResult, int i) {
                    if (callback != null)
                        callback.OnDistanceSearchListener(distanceResult);
                }
            });

            //设置起点和终点，其中起点支持多个
            DistanceSearch.DistanceQuery distanceQuery = new DistanceSearch.DistanceQuery();
            List<LatLonPoint> latLonPoints = new ArrayList<LatLonPoint>();
            latLonPoints.add(new LatLonPoint(start.Latitude, start.Longitude));
            distanceQuery.setOrigins(latLonPoints);
            distanceQuery.setDestination(new LatLonPoint(end.Latitude, end.Longitude));
            //设置测量方式，支持直线和驾车
            distanceQuery.setType(DistanceSearch.TYPE_DISTANCE);
            distanceSearch.calculateRouteDistanceAsyn(distanceQuery);
        } catch (Exception ex) {
            _Utils.saveException(context,ex);
        }
    }

    public static void getDistants(Context context, List<LatLonPoint> latLonPoints, final DistanceSearchCallback callback) {
        try {
        DistanceSearch distanceSearch = new DistanceSearch(context);
        distanceSearch.setDistanceSearchListener(new DistanceSearch.OnDistanceSearchListener() {
            @Override
            public void onDistanceSearched(DistanceResult distanceResult, int i) {
                if (callback != null)
                    callback.OnDistanceSearchListener(distanceResult);
            }
        });

        //设置起点和终点，其中起点支持多个
        DistanceSearch.DistanceQuery distanceQuery = new DistanceSearch.DistanceQuery();
        int index = 0;
        int indexL = latLonPoints.size() - 1;
        while (index < indexL) {
            distanceQuery.addOrigins(latLonPoints.get(index));
            index++;
        }
        distanceQuery.setDestination(latLonPoints.get(index));
        //设置测量方式，支持直线和驾车
        distanceQuery.setType(DistanceSearch.TYPE_DRIVING_DISTANCE);
        distanceSearch.calculateRouteDistanceAsyn(distanceQuery);
        } catch (Exception ex) {
            _Utils.saveException(context,ex);
        }
    }

    public interface DistanceSearchCallback {
        void OnDistanceSearchListener(DistanceResult distanceResult);
    }

    /*
     * 判断edittext是否null
     */
    public static String checkEditText(EditText editText) {
        if (editText != null && editText.getText() != null
                && !(editText.getText().toString().trim().equals(""))) {
            return editText.getText().toString().trim();
        } else {
            return "";
        }
    }

    public static Spanned stringToSpan(String src) {
        return src == null ? null : Html.fromHtml(src.replace("\n", "<br />"));
    }

    public static String colorFont(String src, String color) {
        StringBuffer strBuf = new StringBuffer();

        strBuf.append("<font color=").append(color).append(">").append(src)
                .append("</font>");
        return strBuf.toString();
    }

    public static String makeHtmlNewLine() {
        return "<br />";
    }

    public static String makeHtmlSpace(int number) {
        final String space = "&nbsp;";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < number; i++) {
            result.append(space);
        }
        return result.toString();
    }

    public static String getFriendlyLength(int lenMeter) {
        if (lenMeter > 10000) // 10 km
        {
            int dis = lenMeter / 1000;
            return dis + ChString.Kilometer;
        }

        if (lenMeter > 1000) {
            float dis = (float) lenMeter / 1000;
            DecimalFormat fnum = new DecimalFormat("##0.0");
            String dstr = fnum.format(dis);
            return dstr + ChString.Kilometer;
        }

        if (lenMeter > 100) {
            int dis = lenMeter / 50 * 50;
            return dis + ChString.Meter;
        }

        int dis = lenMeter / 10 * 10;
        if (dis == 0) {
            dis = 10;
        }

        return dis + ChString.Meter;
    }

    public static boolean IsEmptyOrNullString(String s) {
        return (s == null) || (s.trim().length() == 0);
    }

    /**
     * 把LatLng对象转化为LatLonPoint对象
     */
    public static LatLonPoint convertToLatLonPoint(LatLng latlon) {
        return new LatLonPoint(latlon.latitude, latlon.longitude);
    }

    /**
     * 把LatLonPoint对象转化为LatLon对象
     */
    public static LatLng convertToLatLng(LatLonPoint latLonPoint) {
        return new LatLng(latLonPoint.getLatitude(), latLonPoint.getLongitude());
    }

    /**
     * 把集合体的LatLonPoint转化为集合体的LatLng
     */
    public static ArrayList<LatLng> convertArrList(List<LatLonPoint> shapes) {
        ArrayList<LatLng> lineShapes = new ArrayList<LatLng>();
        for (LatLonPoint point : shapes) {
            LatLng latLngTemp = AMapUtil.convertToLatLng(point);
            lineShapes.add(latLngTemp);
        }
        return lineShapes;
    }


    public static String locationToString(AMapLocation location) {

        StringBuffer sb = new StringBuffer();
        sb.append("经    度    : " + location.getLongitude() + "\n");
        sb.append("纬    度    : " + location.getLatitude() + "\n");
        sb.append("精度: " + location.getAccuracy() + "米" + "    ");
        sb.append("提供者: " + location.getProvider() + "    ");

        sb.append("速度: " + location.getSpeed() + "米/秒" + "    ");
        sb.append("角度: " + location.getBearing() + "    ");
        // 获取当前提供定位服务的卫星个数
        sb.append("星数: " + location.getSatellites() + "\n");
        sb.append("" + location.getAddress() + "\n");
        sb.append("兴趣点: " + location.getPoiName() + "   ");
        sb.append("定位类型: " + location.getLocationType() + "\n");

        return sb.toString();
    }

    /**
     * long类型时间格式化
     */
    public static String convertToTime(long time) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(time);
        return df.format(date);
    }

    public static final String HtmlBlack = "#000000";
    public static final String HtmlGray = "#808080";

    public static String getFriendlyTime(int second) {
        if (second > 3600) {
            int hour = second / 3600;
            int miniate = (second % 3600) / 60;
            return hour + "小时" + miniate + "分钟";
        }
        if (second >= 60) {
            int miniate = second / 60;
            return miniate + "分钟";
        }
        return second + "秒";
    }

    //路径规划方向指示和图片对应
    public static int getDriveActionID(String actionName) {
        if (actionName == null || actionName.equals("")) {
            return R.drawable.dir3;
        }
        if ("左转".equals(actionName)) {
            return R.drawable.dir2;
        }
        if ("右转".equals(actionName)) {
            return R.drawable.dir1;
        }
        if ("向左前方行驶".equals(actionName) || "靠左".equals(actionName)) {
            return R.drawable.dir6;
        }
        if ("向右前方行驶".equals(actionName) || "靠右".equals(actionName)) {
            return R.drawable.dir5;
        }
        if ("向左后方行驶".equals(actionName) || "左转调头".equals(actionName)) {
            return R.drawable.dir7;
        }
        if ("向右后方行驶".equals(actionName)) {
            return R.drawable.dir8;
        }
        if ("直行".equals(actionName)) {
            return R.drawable.dir3;
        }
        if ("减速行驶".equals(actionName)) {
            return R.drawable.dir4;
        }
        return R.drawable.dir3;
    }

    public static int getWalkActionID(String actionName) {
        if (actionName == null || actionName.equals("")) {
            return R.drawable.dir13;
        }
        if ("左转".equals(actionName)) {
            return R.drawable.dir2;
        }
        if ("右转".equals(actionName)) {
            return R.drawable.dir1;
        }
        if ("向左前方".equals(actionName) || "靠左".equals(actionName) || actionName.contains("向左前方")) {
            return R.drawable.dir6;
        }
        if ("向右前方".equals(actionName) || "靠右".equals(actionName) || actionName.contains("向右前方")) {
            return R.drawable.dir5;
        }
        if ("向左后方".equals(actionName) || actionName.contains("向左后方")) {
            return R.drawable.dir7;
        }
        if ("向右后方".equals(actionName) || actionName.contains("向右后方")) {
            return R.drawable.dir8;
        }
        if ("直行".equals(actionName)) {
            return R.drawable.dir3;
        }
        if ("通过人行横道".equals(actionName)) {
            return R.drawable.dir9;
        }
        if ("通过过街天桥".equals(actionName)) {
            return R.drawable.dir11;
        }
        if ("通过地下通道".equals(actionName)) {
            return R.drawable.dir10;
        }

        return R.drawable.dir13;
    }

    public static String getBusPathTitle(BusPath busPath) {
        if (busPath == null) {
            return String.valueOf("");
        }
        List<BusStep> busSetps = busPath.getSteps();
        if (busSetps == null) {
            return String.valueOf("");
        }
        StringBuffer sb = new StringBuffer();
        for (BusStep busStep : busSetps) {
            StringBuffer title = new StringBuffer();
            if (busStep.getBusLines().size() > 0) {
                for (RouteBusLineItem busline : busStep.getBusLines()) {
                    if (busline == null) {
                        continue;
                    }

                    String buslineName = getSimpleBusLineName(busline.getBusLineName());
                    title.append(buslineName);
                    title.append(" / ");
                }
//					RouteBusLineItem busline = busStep.getBusLines().get(0);

                sb.append(title.substring(0, title.length() - 3));
                sb.append(" > ");
            }
            if (busStep.getRailway() != null) {
                RouteRailwayItem railway = busStep.getRailway();
                sb.append(railway.getTrip() + "(" + railway.getDeparturestop().getName()
                        + " - " + railway.getArrivalstop().getName() + ")");
                sb.append(" > ");
            }
        }
        return sb.substring(0, sb.length() - 3);
    }

    public static String getBusPathDes(BusPath busPath) {
        if (busPath == null) {
            return String.valueOf("");
        }
        long second = busPath.getDuration();
        String time = getFriendlyTime((int) second);
        float subDistance = busPath.getDistance();
        String subDis = getFriendlyLength((int) subDistance);
        float walkDistance = busPath.getWalkDistance();
        String walkDis = getFriendlyLength((int) walkDistance);
        return String.valueOf(time + " | " + subDis + " | 步行" + walkDis);
    }

    public static String getSimpleBusLineName(String busLineName) {
        if (busLineName == null) {
            return String.valueOf("");
        }
        return busLineName.replaceAll("\\(.*?\\)", "");
    }

    private static SimpleDateFormat sdf = null;

    public static String formatUTC(long l, String strPattern) {
        if (TextUtils.isEmpty(strPattern)) {
            strPattern = "yyyy-MM-dd HH:mm:ss";
        }
        if (sdf == null) {
            try {
                sdf = new SimpleDateFormat(strPattern, Locale.CHINA);
            } catch (Throwable e) {
            }
        } else {
            sdf.applyPattern(strPattern);
        }
        return sdf == null ? "NULL" : sdf.format(l);
    }

    public static void getMyLocation(Context context) {
        //初始化client
        final AMapLocationClient locationClient = new AMapLocationClient(context);
        AMapLocationClientOption locationOption = new AMapLocationClientOption();
        locationOption.setOnceLocation(true);//可选，设置是否单次定位。默认是false
        locationOption.setOnceLocationLatest(true);//可选，设置是否等待wifi刷新，默认为false.如果设置为true,会自动变为单次定位，持续定位时不要使用
        locationClient.setLocationOption(locationOption);

        // 设置定位监听
        locationClient.setLocationListener(new AMapLocationListener() {
            @Override
            public void onLocationChanged(AMapLocation location) {

                if (location != null) {
                    if (location.getErrorCode() == 0) {

                    } else {
                        String result = "定位失败，错误码：" + location.getErrorCode();
                    }
                } else {
                    String result = "定位失败：location is null";
                }
            }
        });
        locationClient.startLocation();
    }

}
