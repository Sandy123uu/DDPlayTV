package com.xyoye.common_component.database.migration

import android.content.Context
import androidx.startup.Initializer
import com.xyoye.common_component.base.app.BaseInitializer
import com.xyoye.common_component.base.app.BuglyInitializer
import com.xyoye.common_component.utils.RuntimeErrorReporter
import com.xyoye.common_component.utils.SupervisorScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class ManualMigrationInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        SupervisorScope.IO.launch {
            runCatching { ManualMigration.migrate() }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable

                    RuntimeErrorReporter.report(
                        throwable,
                        tag = LOG_TAG,
                        message = "manual migration failed",
                        context =
                            mapOf(
                                "task" to "G-T0025",
                                "module" to ":core_database_component",
                                "migration" to "6->7",
                                "step" to "migrate_6_7",
                            ),
                        extraInfo = "module=:core_database_component migration=6->7 step=migrate_6_7 task=G-T0025",
                    )
                }
        }
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> =
        mutableListOf(
            BaseInitializer::class.java,
            BuglyInitializer::class.java,
        )

    private companion object {
        private const val LOG_TAG = "ManualMigration"
    }
}
