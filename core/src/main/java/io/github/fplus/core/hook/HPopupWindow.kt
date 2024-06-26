package io.github.fplus.core.hook

import android.content.Context
import android.widget.PopupWindow
import com.freegang.extension.fieldGet
import de.robv.android.xposed.XC_MethodHook
import io.github.fplus.core.base.BaseHook
import io.github.xpler.core.entity.OnBefore
import io.github.xpler.core.hookBlockRunning
import io.github.xpler.core.log.XplerLog

@Deprecated("暂存区")
class HPopupWindow : BaseHook<PopupWindow>() {
    companion object {
        const val TAG = "HPopupWindow"
    }

    @OnBefore("showAtLocation")
    fun showAtLocationBefore(params: XC_MethodHook.MethodHookParam) {
        hookBlockRunning(params) {
            val context = thisObject.fieldGet("mContext") as Context
        }.onFailure {
            XplerLog.e(it)
        }
    }
}