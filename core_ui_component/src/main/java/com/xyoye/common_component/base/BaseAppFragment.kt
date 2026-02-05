package com.xyoye.common_component.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment

/**
 * Created by xyoye on 2020/7/27.
 */

abstract class BaseAppFragment<V : ViewDataBinding> : Fragment() {
    private var _binding: V? = null

    protected val dataBinding get() = _binding!!
    protected val bindingOrNull get() = _binding

    protected lateinit var mAttachActivity: AppCompatActivity

    private lateinit var loadingHost: LoadingHost

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mAttachActivity =
            context as? AppCompatActivity
                ?: throw IllegalStateException(
                    "${this::class.java.name} must be attached to an AppCompatActivity, but was: ${context::class.java.name}"
                )
        loadingHost =
            context as? LoadingHost
                ?: throw IllegalStateException(
                    "${this::class.java.name} host activity must implement LoadingHost, but was: ${context::class.java.name}"
                )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DataBindingUtil.inflate(inflater, getLayoutId(), container, false)
        return dataBinding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
    }

    protected fun showLoading(msg: String = "") {
        loadingHost.showLoading(msg)
    }

    protected fun hideLoading() {
        loadingHost.hideLoading()
    }

    protected fun isDestroyed(): Boolean = _binding == null

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    abstract fun getLayoutId(): Int

    abstract fun initView()
}
