package com.dragon.navigation;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Color;
import android.widget.LinearLayout;

import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.model.LatLng;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.core.SuggestionCity;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.dragon.navigation.util.NewWidget;
import com.dragon.navigation.util.ToastUtil;

import java.util.List;

/**
 * This file created by dragon on 2016/9/20 15:46,
 * belong to com.dragon.navigation .
 * 关于搜索的类
 */
public class ArPoiSearch implements PoiSearch.OnPoiSearchListener{
    private Activity mActivity;
    //输入搜索关键字
    private String mKeyWord;
    //搜索的风格：如酒店、美食、公交等
    private String mStylePoi;
    //城市名和区号：如果为空代表全国
    private String mCityCode;
    private ProgressDialog progDialog = null;// 搜索时进度条
    private PoiResult poiResult; // poi返回的结果
    private PoiSearch.Query query;// Poi查询条件类
    private PoiSearch poiSearch;// POI搜索
    private int currentPage = 0;// 当前页面，从0开始计数
    private List<PoiItem> poiItems;
    private List<SuggestionCity> suggestionCities;
    //  设置每页最多返回多少条poiitem
    private int mPoiItems=10;
    private int poiOrSuggestion=1;
    private PoiResult resultList;
    private LinearLayout lin;
    private LinearLayout.LayoutParams LP_FW;
    private float distance;
    public static LatLng here;

    public void setActivity(Activity mActivity){
        this.mActivity = mActivity;
    }
    public void setKeyWord(String mKeyWord){
        this.mKeyWord = mKeyWord;
    }
    public void setStylePoi(String mStylePoi){
        this.mStylePoi = mStylePoi;
    }
    public void setCityCode(String mCityCode){
        this.mCityCode = mCityCode;
    }
    //    构造函数
    public ArPoiSearch(){

    }
    //    构造函数
    public ArPoiSearch(Activity activity){
        this.mActivity = activity;

    }
    //    构造函数
    public ArPoiSearch(Activity activity,String mKeyWord){
        this.mActivity = activity;
        this.mKeyWord = mKeyWord;
        this.mStylePoi = "";
        this.mCityCode = "";

    }

    public ArPoiSearch(Activity activity,String mKeyWord, String mStylePoi, String mCityCode,LinearLayout lin){
        this.mActivity= activity;
        this.mKeyWord = mKeyWord;
        this.mStylePoi = mStylePoi;
        this.mCityCode = mCityCode;
        this.lin = lin;
        this.LP_FW = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);


    }

    public void doSearch(){
        showProgressDialog();// 耗时操作前，显示进度框
        currentPage = 0;
        // 第一个参数表示搜索字符串，第二个参数表示poi搜索类型，第三个参数表示poi搜索区域（空字符串代表全国）
        query = new PoiSearch.Query(this.mKeyWord, "", this.mCityCode);
        query.setPageSize(this.mPoiItems);// 设置每页最多返回多少条poiitem
        query.setPageNum(currentPage);// 设置查第一页);//设置查询页码
//        poiSearch.setOnPoiSearchListener(this);
//        poiSearch.setBound(new PoiSearch.SearchBound(lp, 5000, true));

        poiSearch = new PoiSearch(this.mActivity, query);//兴趣点搜索
        poiSearch.setOnPoiSearchListener(this);
        poiSearch.searchPOIAsyn();

    }


    private LinearLayout  generateNewWidget(String str,float distance)
    {
        LinearLayout layout_sub_Lin=new LinearLayout(this.mActivity);
//        layout_sub_Lin.setBackgroundColor(Color.argb(0xff, 0x00, 0xff, 0x00));
        layout_sub_Lin.setOrientation(LinearLayout.VERTICAL);
        layout_sub_Lin.setPadding(5, 5, 5, 5);

        NewWidget mNewWidget = new NewWidget(this.mActivity);
        LinearLayout.LayoutParams LP_WW = new LinearLayout.LayoutParams(600,200);
        mNewWidget.setTitle(str);
        mNewWidget.setContent(distance+"m");
        mNewWidget.setTitleBackgroundColor(Color.RED);
        mNewWidget.setContentBackgroundColor(Color.GRAY);
        mNewWidget.setTextSize(40);
        mNewWidget.setTextColor(Color.GREEN);
        mNewWidget.setLayoutParams(LP_WW);
        layout_sub_Lin.addView(mNewWidget);
        return  layout_sub_Lin ;
    }


    /**
     * POI信息查询回调方法
     */
    @Override
    public void onPoiSearched(PoiResult result, int rCode) {
        dissmissProgressDialog();// 隐藏对话框
        if (rCode == 1000) {
            if (result != null && result.getQuery() != null) {// 搜索poi的结果
                if (result.getQuery().equals(query)) {// 是否是同一条
                    poiResult = result;
                    // 取得搜索到的poiitems有多少页
                    poiItems = poiResult.getPois();// 取得第一页的poiitem数据，页数从数字0开始

//                    ToastUtil.show(this.mActivity,poiItems.get(0)+","+poiItems.get(1));
                //    LatLng var0= convertToLatLng(poiItems.get(1).getLatLonPoint());
                //    LatLng var1= convertToLatLng(poiItems.get(2).getLatLonPoint());

                    lin.removeAllViews();
                    for(int i=0;i<poiItems.size();i++) {
                        LatLng var0= convertToLatLng(poiItems.get(i).getLatLonPoint());
                        distance=AMapUtils.calculateLineDistance(var0, here);
                        lin.addView(generateNewWidget((poiItems.get(i) + ""),distance), LP_FW);
                    }
//                    poiItems.get(1).getTitle()+","+poiItems.get(1).getSnippet()
                   // ToastUtil.show(this.mActivity,","+ AMapUtils.calculateLineDistance(var0,var1));
                    suggestionCities = poiResult.getSearchSuggestionCitys();// 当搜索不到poiitem数据时，会返回含有搜索关键字的城市信息
                    if (poiItems != null && poiItems.size() > 0) {
                        poiOrSuggestion=1;
                    } else if (suggestionCities != null
                            && suggestionCities.size() > 0) {
                        showSuggestCity(suggestionCities);
                        poiOrSuggestion=2;
                    } else {
                        ToastUtil.show(this.mActivity,
                                R.string.no_result);
                        poiOrSuggestion=0;
                    }
                }
            } else {
                ToastUtil.show(this.mActivity,
                        R.string.no_result);
            }
        } else {
            ToastUtil.showerror(this.mActivity, rCode);
        }

    }


    /**
     * 把LatLonPoint对象转化为LatLon对象
     */
    public static LatLng convertToLatLng(LatLonPoint latLonPoint) {
        return new LatLng(latLonPoint.getLatitude(), latLonPoint.getLongitude());
    }
    /**
     * 显示进度框
     */
    private void showProgressDialog() {
        if (progDialog == null)
            progDialog = new ProgressDialog(this.mActivity);
        progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDialog.setIndeterminate(false);
        progDialog.setCancelable(false);
        progDialog.setMessage("正在搜索:\n" + this.mKeyWord);
        progDialog.show();
    }
    /**
     * 隐藏进度框
     */
    private void dissmissProgressDialog() {
        if (progDialog != null) {
            progDialog.dismiss();
        }
    }

    @Override
    public void onPoiItemSearched(PoiItem item, int rCode) {
        // TODO Auto-generated method stub

    }

    /**
     * poi没有搜索到数据，返回一些推荐城市的信息
     */
    private String showSuggestCity(List<SuggestionCity> cities) {
        String infomation = "推荐城市\n";
        for (int i = 0; i < cities.size(); i++) {
            infomation += "城市名称:" + cities.get(i).getCityName() + "城市区号:"
                    + cities.get(i).getCityCode() + "城市编码:"
                    + cities.get(i).getAdCode() + "\n";
        }
        ToastUtil.show(this.mActivity, infomation);
        return infomation;
    }

//    下面这个方法还有问题，待解决
//    给出解决思路：需要用到android的代理开发模式，本人能力有限，留给后面的开发者去完善（dragon）
    public PoiResult getPoiResult(){
//        返回poi搜索结果
        if(1==poiOrSuggestion){
            resultList=this.poiResult;
        }

        return resultList;
    }
}

////        如果没有合适的POI搜索结果返回建议结果
//if(2==poiOrSuggestion){
//        resultList=this.poiResult.getSearchSuggestionCitys();
//        }