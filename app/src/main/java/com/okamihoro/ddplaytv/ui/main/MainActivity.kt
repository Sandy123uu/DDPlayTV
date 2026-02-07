package com.okamihoro.ddplaytv.ui.main

import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.launcher.ARouter
import com.okamihoro.ddplaytv.R
import com.okamihoro.ddplaytv.databinding.ActivityMainBinding
import com.okamihoro.ddplaytv.ui.shell.BaseShellActivity
import com.okamihoro.ddplaytv.ui.shell.ShellFragmentSwitcher
import com.xyoye.common_component.config.RouteTable
import com.xyoye.common_component.extension.findAndRemoveFragment
import com.xyoye.common_component.log.LogFacade
import com.xyoye.common_component.log.model.LogModule
import com.xyoye.common_component.services.DeveloperMenuService

class MainActivity : BaseShellActivity<ActivityMainBinding>() {
    companion object {
        private const val TAG_FRAGMENT_HOME = "tag_fragment_home"
        private const val TAG_FRAGMENT_MEDIA = "tag_fragment_media"
        private const val TAG_FRAGMENT_PERSONAL = "tag_fragment_personal"
        private const val LOG_TAG = "MainNav"
    }

    @Autowired
    lateinit var developerMenuService: DeveloperMenuService

    private val fragmentSwitcher by lazy {
        ShellFragmentSwitcher(
            fragmentManager = supportFragmentManager,
            containerId = R.id.fragment_container,
            fragmentProvider = ::getFragment,
        )
    }

    // 标题栏菜单管理器
    private var developerMenus: DeveloperMenuService.Delegate? = null

    override fun getLayoutId() = R.layout.activity_main

    override fun initView() {
        // 隐藏返回按钮
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            setDisplayShowTitleEnabled(true)
        }

        // 默认显示媒体库页面
        // 标题
        title = "媒体库"
        LogFacade.d(LogModule.CORE, LOG_TAG, "init default tab=media")
        // 移除所有已添加的fragment，防止如旋转屏幕后导致的屏幕错乱
        supportFragmentManager.findAndRemoveFragment(
            TAG_FRAGMENT_HOME,
            TAG_FRAGMENT_MEDIA,
            TAG_FRAGMENT_PERSONAL,
        )
        // 切换到媒体库页面
        switchFragment(TAG_FRAGMENT_MEDIA)
        // 底部导航栏设置选中
        dataBinding.navigationView.post {
            dataBinding.navigationView.selectedItemId = R.id.navigation_media
        }

        // 设置底部导航栏事件
        dataBinding.navigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_home -> {
                    title = "弹弹play"
                    LogFacade.d(LogModule.CORE, LOG_TAG, "switch tab=home")
                    switchFragment(TAG_FRAGMENT_HOME)
                }

                R.id.navigation_media -> {
                    title = "媒体库"
                    LogFacade.d(LogModule.CORE, LOG_TAG, "switch tab=media")
                    switchFragment(TAG_FRAGMENT_MEDIA)
                }

                R.id.navigation_personal -> {
                    title = "个人中心"
                    LogFacade.d(LogModule.CORE, LOG_TAG, "switch tab=personal")
                    switchFragment(TAG_FRAGMENT_PERSONAL)
                }
            }
            return@setOnItemSelectedListener true
        }

        initShell()
    }

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?
    ): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && handleBackExit()) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        developerMenus = developerMenuService.create(this, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val handled = developerMenus?.onOptionsItemSelected(item) == true
        return handled || super.onOptionsItemSelected(item)
    }

    private fun switchFragment(tag: String) {
        val fragmentPath =
            when (tag) {
                TAG_FRAGMENT_HOME -> RouteTable.Anime.HomeFragment
                TAG_FRAGMENT_MEDIA -> RouteTable.Local.MediaFragment
                TAG_FRAGMENT_PERSONAL -> RouteTable.User.PersonalFragment
                else -> throw RuntimeException("no match fragment")
            }
        fragmentSwitcher.switchFragment(tag, fragmentPath)
    }

    private fun getFragment(path: String) =
        ARouter
            .getInstance()
            .build(path)
            .navigation() as Fragment?
}
