1.ReaderView（继承自adapterView,属于整个框架）
     -->onMeasure :先计算屏幕的宽高与pdf宽高的比例。再计算目前的缩放比例，用来设置子view的宽高
     -->onLayout：
            -->a.判断是否移动，也就是说是否需要绘制下一页。如果需要下一页则，清除当前页，创建下一页，并且绘制高亮框等控件
            -->b.将无用的page页面，清空并且复用
            -->c.给当前页面布局
            -->d.如果存在上一页或者，下一页则布局

     创建一个pdf页面的方法getOrCreateChild()



   MuPDFReaderView继承ReaderView,-->内部主要实现AdapterView的点击，滑动，触摸等事件


   MuPDFPageView（对应具体的一页PDF）
      -->blank ,清空页面，显示loading
      -->setpage,
             设置pdf的页面的实际大小
             加载Annotations, 加载Widget:这两项与pdf具体属性有关,暂时页面上没体现出来是加载什么
      --> Point  mPatchViewSize;//表示图片的实际大小
          Rect   mPatchArea;　//表示显示在屏幕区域的pdf的rect,不能大于屏幕的区域





2.ScaleGestureDetector图片缩放
3.Scroller
    --->scrollTo()还是scrollBy()方法，滚动的都是该View内部的内容
    --->scrollTo/scrollBy,以为X轴，为例子。正值向左移动，负值向右移



4.ndk 两中编译方式:ndk-build来构建，一种是CMake构建原生库。这个是Google新提出的，比较方便强大。

5.view.post方法，通过发消息的方式，保证操作在onlayout之后执行

6.搜索功能
　　　　－－>SearchTaskResult.java  //搜索结果，包括当前页码，搜索内容，搜索结果
　　　　－－>SearcTask.java //搜索，AsyncTask
　　　　－－>搜索到内容
　　　　　　　　　　－－>setDisplayedViewIndex
　　　　　　　　　　    -->清空当前页注释
　　　　　　　　　　　　 -->重设当前页面，并且重绘制
　　　　　　　　　　－－>绘制当前前后３页的,搜索框

7.初始化时,高亮，下划线，删除线，墨迹等的加载
　     MuPDFPageView.java
              -->setPage()
                 -->loadAnnotations　//加载批注
                 -->mLoadWidgetAreas //加载wdget区域
                 -->mGetLinkInfo　　　//高亮并且启用墨迹
                 -->渲染pdf
                 -->绘制mSearchView表示:高亮/搜索框/触摸屏幕选择高亮，下划线，删除线时/删除框/墨迹
                 　　-->确定下划线，删除线．这两个是在so文件中绘制的．没有在java中处理



8.提取功能点
    -->浏览模式
    -->切换屏幕
    -->目录：代码在　OutlineActivity中
    -->搜索:
    　　　-->searchText,//按键监听
    　　　-->内容变化监听
    　　　-->搜索内容有向前向后之分
    　　　-->SearchTask搜索/SearchTaskResult
    　　　-->搜索到内容后，重绘，并且将搜索内容绘制出来
    -->选择文本
    　　　-->调用ReaderView的onScroll方法
    　　　-->计算对应当前缩放比时的坐标，计算真实文档中的坐标
         -->取出当前页面的所有text内容(以及其真实的坐标)
         -->将符合当前选中区域的文本，拼接成最终需要绘制的区域．
         -->因为结算的一直是，真是文本中的坐标，此时再根据缩放比例，计算当前页面真正的绘制区域

    -->复制文本:选择文本结束后，将选择区域清空，再重绘制即可
    -->高亮，下划线，删除线，都是基于选择文本．
    　　-->在确定的内容框中做处理．
       -->doselect 取消掉之前"选择文本"的颜色框子
    -->墨迹:onTouchEvent中,记录每个点的坐标
       -->down：从down-move-up,这些点放入到一个ArrayList中，页面中的所有线，再放入ArrayList<ArrayList<Point>>
       -->move:
       -->保存确认绘制:将坐标点以二维数组形式，存储起来
