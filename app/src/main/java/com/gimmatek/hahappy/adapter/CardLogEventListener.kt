package com.gimmatek.hahappy.adapter

import com.gimmatek.hahappy.model.Schedule

interface CardLogEventListener {
    fun onCardLogClick(isgive: Boolean, cardId: String)
}