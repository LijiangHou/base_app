package com.common.app.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.common.app.uikit.PtrHeaderView;
import com.common.listview.AbsListDataAdapter;
import com.common.listview.AbsListView;
import com.common.listview.MySectionIndexer;

/**
 * Created by houlijiang on 15/1/19.
 *
 * 通用ListView基类，把常用功能做了简单封装，避免重复代码
 * 把AbsListView需要设置的通过抽象方法让子类实现同时增加加载第一页接口使逻辑更清晰
 */
public abstract class AbsListFragment extends BaseFragment implements AbsListView.IOnPullToRefresh {

    protected AbsListView mRecyclerListView;
    protected AbsListDataAdapter mAdapter;

    protected View mListHeaderView;
    protected View mListEmptyView;
    protected View mListErrorView;
    protected View mListProgressView;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initData();// 给子类一个机会在初始化list相关类之前初始化数据

        mRecyclerListView = (AbsListView) getView().findViewById(getListViewId());
        mAdapter = getAdapter(getActivity());
        final RecyclerView.LayoutManager manager = getLayoutManager();
        if (manager instanceof GridLayoutManager) {
            ((GridLayoutManager) manager).setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (mAdapter.isFooterView(position)) {
                        return ((GridLayoutManager) manager).getSpanCount();
                    } else {
                        return 1;
                    }
                }
            });
        }
        mRecyclerListView.setLayoutManager(manager);

        if (isRefreshEnabled()) {
            mRecyclerListView.setRefreshListener(this);
            View headerView = createHeaderView();
            AbsListView.IPtrHeaderUI handler = null;
            if (headerView instanceof AbsListView.IPtrHeaderUI) {
                handler = (AbsListView.IPtrHeaderUI) headerView;
            }
            mRecyclerListView.setRefreshHeaderView(headerView, handler);
        }

        MySectionIndexer indexer = getIndexer();
        if (indexer != null) {
            mRecyclerListView.setIndex(indexer);
        }
        mRecyclerListView.setAdapter(mAdapter);
        mAdapter.setIsLoading();
        loadFirstPage();

        mListHeaderView = mRecyclerListView.getHeaderView();
        mListEmptyView = mRecyclerListView.getEmptyView();
        mListErrorView = mRecyclerListView.getErrorView();
        mListProgressView = mRecyclerListView.getProgressView();
    }

    protected View createHeaderView() {
        return new PtrHeaderView(getActivity());
    }

    /**
     * 子类需要在初始化list相关类之前初始其他数据的可以重载这个方法
     */
    protected void initData() {
    }

    protected void reloadSections() {
        mRecyclerListView.reloadSections();
    }

    /**
     * 是否需要支持下拉刷新
     */
    protected boolean isRefreshEnabled() {
        return true;
    }

    /**
     * 下拉刷新
     * 目前什么也没做，将来可能加上一些通用逻辑，子类实现特殊逻辑
     */
    @Override
    public void onRefreshBegin() {
        mAdapter.setIsLoading();
        onListRefresh();
    }

    /**
     * 获取ListView的adapter
     */
    protected abstract AbsListDataAdapter getAdapter(Context context);

    /**
     * 获取ListView的id
     */
    protected abstract int getListViewId();

    /**
     * 是否支持快速检索，如果返回空就是不支持
     */
    protected abstract MySectionIndexer getIndexer();

    /**
     * 获取LayoutManager
     */
    protected abstract RecyclerView.LayoutManager getLayoutManager();

    /**
     * 加载第一页
     */
    protected abstract void loadFirstPage();

    /**
     * 刷新回调
     */
    public abstract void onListRefresh();

}
