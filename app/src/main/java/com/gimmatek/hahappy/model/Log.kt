package com.gimmatek.hahappy.model

import android.support.annotation.Keep
import com.google.firebase.database.IgnoreExtraProperties

/**
 * Created by USER on 2018/4/3.
 */
@Keep // 保留名稱
@IgnoreExtraProperties // 忽略讀取到的多於參數
data class Log(
        val scheduleId: String = "",
        val dateTime: String = "",
        val date: String = "",
        val cardName: String = "",
        val transNo: String = ""
)