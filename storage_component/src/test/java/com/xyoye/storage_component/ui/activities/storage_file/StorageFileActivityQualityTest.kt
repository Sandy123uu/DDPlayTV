package com.xyoye.storage_component.ui.activities.storage_file

import android.app.Activity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageFileActivityQualityTest {
    private companion object {
        private const val REQUEST_CODE_BILIBILI_RISK_VERIFY = 3301
        private const val REQUEST_CODE_OPEN115_REAUTH = 3302
        private const val REQUEST_CODE_CLOUD115_REAUTH = 3303
    }

    @Test
    fun shouldResumeRiskPlaybackOnlyWhenRequestResultAndPendingFileAreValid() {
        assertTrue(
            StorageFileActivity.shouldResumeRiskPlayback(
                requestCode = REQUEST_CODE_BILIBILI_RISK_VERIFY,
                resultCode = Activity.RESULT_OK,
                hasPendingFile = true,
            ),
        )
        assertFalse(
            StorageFileActivity.shouldResumeRiskPlayback(
                requestCode = REQUEST_CODE_BILIBILI_RISK_VERIFY,
                resultCode = Activity.RESULT_CANCELED,
                hasPendingFile = true,
            ),
        )
        assertFalse(
            StorageFileActivity.shouldResumeRiskPlayback(
                requestCode = REQUEST_CODE_BILIBILI_RISK_VERIFY,
                resultCode = Activity.RESULT_OK,
                hasPendingFile = false,
            ),
        )
        assertFalse(
            StorageFileActivity.shouldResumeRiskPlayback(
                requestCode = REQUEST_CODE_OPEN115_REAUTH,
                resultCode = Activity.RESULT_OK,
                hasPendingFile = true,
            ),
        )
    }

    @Test
    fun shouldRefreshAfterReauthAcceptsBothCloud115AndOpen115SuccessOnly() {
        assertTrue(StorageFileActivity.shouldRefreshAfterReauth(REQUEST_CODE_OPEN115_REAUTH, Activity.RESULT_OK))
        assertTrue(StorageFileActivity.shouldRefreshAfterReauth(REQUEST_CODE_CLOUD115_REAUTH, Activity.RESULT_OK))

        assertFalse(StorageFileActivity.shouldRefreshAfterReauth(REQUEST_CODE_OPEN115_REAUTH, Activity.RESULT_CANCELED))
        assertFalse(StorageFileActivity.shouldRefreshAfterReauth(REQUEST_CODE_CLOUD115_REAUTH, Activity.RESULT_CANCELED))
        assertFalse(StorageFileActivity.shouldRefreshAfterReauth(9999, Activity.RESULT_OK))
    }
}
