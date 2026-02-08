package com.okamihoro.ddplaytv.app.service

import android.content.Context
import android.content.Intent
import com.alibaba.android.arouter.facade.annotation.Route
import com.xyoye.common_component.config.RouteTable
import com.xyoye.common_component.services.Media3SessionServiceProvider

@Route(
    path = RouteTable.App.Media3SessionServiceProvider,
    name = "Media3SessionService Intent Provider",
)
class Media3SessionServiceProviderImpl : Media3SessionServiceProvider {
    override fun init(context: Context?) {
        // ARouter provider lifecycle hook; this provider only creates intents on demand.
    }

    override fun createBindIntent(context: Context): Intent = Intent(context, Media3SessionService::class.java)
}
