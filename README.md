


## SwipeRefresh 支持单个View 的下拉刷新以及上拉加载
### SwipeNest 滚动视图（支持各种 普通View recycleView scrollView listView等 混合排列） 处理滚动冲突  附带下拉刷新以及上拉加载
### MultipleListAdapter<T> 快速实现 ListView 多种类型展示 (自带上拉加载 可设置开启)
### MultipleRecycleAdapter<T>  快速实现  RecycleView多种类型展示 (自带上拉加载 可设置开启)

Add Gradle dependency:
```gradle
dependencies {
      compile 'com.github.powyin:scroll:2.0.0'
      compile 'com.android.support:recyclerview-v7:24.0.0'
}
```

### SwipeRefresh UI

|刷新（可自定义）|下拉加载获取新数据(可自定义)|上拉加载数据全部获得(可自定义)|
|---|---|----
|![github](https://github.com/powyin/nest-scroll/blob/master/app/src/main/res/raw/refresh_pre.gif)|![github](https://github.com/powyin/nest-scroll/blob/master/app/src/main/res/raw/refresh_load_2.gif)|![github](https://github.com/powyin/nest-scroll/blob/master/app/src/main/res/raw/refresh_load_1.gif)|


### SwipeNest UI

|刷新（可自定义）|平滑多个NestScrollChilder之间的滚动|自定义刷新范例|
|---|---|----
|![github](https://github.com/powyin/nest-scroll/blob/master/app/src/main/res/raw/nest_pre.gif)|![github](https://github.com/powyin/nest-scroll/blob/master/app/src/main/res/raw/nest_pre_1.gif)|![github](https://github.com/powyin/nest-scroll/blob/master/app/src/main/res/raw/nest_pre_2.gif)|


### how to use  SwipeRefresh

   
      <com.powyin.scroll.widget.SwipeRefresh
        android:id = "@+id/re"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:fresh_model="BOTH" >


        <!--<ListView-->
            <!--android:id="@+id/my_list"-->
            <!--android:background="#ffffffff"-->
            <!--android:overScrollMode="never"-->
            <!--android:dividerHeight="0dp"-->
            <!--android:divider="#00000000"-->
            <!--android:layout_width="match_parent"-->
            <!--android:layout_height="match_parent"/>-->

        <!--或者是RecyclerView-->
        <android.support.v7.widget.RecyclerView
            android:background="#ffffff"
            android:id="@+id/my_recycle"
            android:overScrollMode="never"
            android:layout_width="match_parent"
            android:layout_height="match_parent"/>

    </com.powyin.scroll.widget.SwipeRefresh>
    
### how to use  SwipeNest 

        <com.powyin.scroll.widget.SwipeNest
        android:id="@+id/nest_combine"
        android:background="#e5e5e5"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        >


        <FrameLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="#b7b7b7"
            android:clickable="true">
            <ImageView
                android:layout_width="match_parent"
                android:layout_height="300dp"
                android:src="@drawable/pic_4"
                android:scaleType="centerCrop"
                />
        </FrameLayout>

        <android.support.v7.widget.RecyclerView
            android:background="#ffffff"
            android:id="@+id/my_recycle"
            android:overScrollMode="never"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"/>

        <!--<FrameLayout-->
            <!--android:layout_width="match_parent"-->
            <!--android:layout_height="wrap_content"-->
            <!--android:background="#b7b7b7"-->
            <!--android:clickable="true">-->
            <!--<ImageView-->
                <!--android:layout_width="match_parent"-->
                <!--android:layout_height="300dp"-->
                <!--android:src="@drawable/pic_4"-->
                <!--android:scaleType="centerCrop"-->
                <!--/>-->
        <!--</FrameLayout>-->

    </com.powyin.scroll.widget.SwipeNest>
    
### 设置刷新监听与刷新结果处理

ISwipe

        ISwipe.setOnRefreshListener(new SwipeRefresh.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // 开始下拉刷新
            }

            @Override
            public void onLoading(boolen isLoadViewShow) {
                // 开始加载更多
            }
        });
        
        ISwipe.setFreshStatue(ISwip.RreshStatus.SUCCESS);             //下拉刷新 完成
        ISwipe.setFreshStatue(ISwipe.RreshStatus.ERROR_AUTO_CANCEL);  //下拉刷新 失败
        ISwipe.setLoadMoreStatus(ISwipe.LoadedStatus.CONTINUE);       //已经获取更多数据   隐藏上拉加载进度条
        
        
        
        
### SwipeRefresh设置刷新模式

```

ISwipe.setSwipeModel(SwipeControl.SwipeModel model)          

app:fresh_model=“ONLY_REFRESH”                                     

    <declare-styleable name="SwipeRefresh">
        <attr name="fresh_model">
            <enum name="BOTH" value="0" />
            <enum name="ONLY_REFRESH" value="1" />
            <enum name="ONLY_LOADINN" value="2" />
            <enum name="SWIPE_NONE" value="3" />
        </attr>
    </declare-styleable>

(BOTH = SwipeModel.SWIPE_BOTH)                    同时支持下拉刷新与上拉加载  
(ONLY_REFRESH == SwipeModel.SWIPE_ONLY_REFRESH))  只支持下拉刷新 
(ONLY_REFRESH == SwipeModel.SWIPE_ONLY_LOADINN)   只支持上拉加载 
(SWIPE_NONE == SwipeModel.SWIPE_NONE）            都不支持
```


### how to use  MultipleRecycleAdapter&MultipleListAdapter

        MultipleRecycleAdapter multipleRecycleAdapter = new MultipleRecycleAdapter<>(this, TypePowViewHolder_Text.class, TypePowViewHolder_Pic_1.class , TypePowViewHolder_Pic_4.class);
        mRecyclerView.setAdapter(multipleRecycleAdapter);
        
        MultipleListAdapter multipleListAdapter = new  MultipleListAdapter<>(this, TypePowViewHolder_Text.class, TypePowViewHolder_Pic_1.class , TypePowViewHolder_Pic_4.class);
        listView.setAdapter(multipleListAdapter);
        
        PowViewHolder<T>    此类抽象出获取ListAdapter.Item 与Recycle.Adapter.Item的必须条件；使用时：必须确定泛型类型
        AdapterDelegate<T>  此接口定义了ListAdapter 与 RecycleView.Adatper 公共数据操作；
    


### 注意
```
  一：SwipeRefresh 只有当包含的子View有足够内容进行独立滑动时 下拉加载才启动有效
  二：SwipeRefresh 调用finishRefresh()后；将自动触发setIsLoadComplete(false)；如果不需要， 可设反；
  三：SwipeNest 不支持上拉加载; 请用其他方式实现  比如：
          (1) Adater的getView(getView(int postion) postion 为数据列表的最后一项时可以进行上拉加载操作）
          (2) 设置ViewTreeObserver.addOnGlobalLayoutListener 监听当ViewcanScrollVertically（-1） 与 Adater.getCount() 判断当前是否可以开始上拉加载；
          (3) 直接使用RecyleView();
  四：SwipeNest 目前只支持NestedScrollingChild继承类作为子View；
  五：MultipleListAdapter<T>&MultipleRecycleAdapter<T> 只是 把各种Adapter的处理逻辑 代理到 ViewHolder 上了；MultipleAdapter会根据 不同的数据类型 分别找到能装载此Data的ViewHolder，再通过ViewHolder.loadData() 做刷新操作； 设计思路类似RecycleView.ViewHolder；

```






