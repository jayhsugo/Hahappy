package com.gimmatek.hahappy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.gimmatek.hahappy.adapter.CardHolder
import com.gimmatek.hahappy.model.Card
import com.gimmatek.hahappy.utils.ReferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*


class LotteryActivity : AppCompatActivity(), FirebaseAuth.AuthStateListener {

    private val uiGiftList: RecyclerView by lazy { findViewById<RecyclerView>(R.id.list_gift) }
    private val uiStart: ImageView by lazy { findViewById<ImageView>(R.id.iv_start) }
    private val mAuth = FirebaseAuth.getInstance()
    private lateinit var refGift: Query
    private lateinit var refGetGift: DatabaseReference
    private var mGifts: List<Pair<String, Card>>? = null
    private var mMode: String = ""
    private var mLotteryGiftKey: String = ""
    private var mLotteryGift: Pair<String, Card>? = null
    private var mDbpath: MutableMap<String, String> = mutableMapOf()
    private val uiProgress: ProgressBar by lazy { findViewById<ProgressBar>(R.id.progress_bar) }

    private val snapGift = object : ValueEventListener {
        override fun onCancelled(error: DatabaseError) {

        }

        override fun onDataChange(snap: DataSnapshot) {
            if (snap.exists()) {
                mGifts = snap.children.asSequence()
                        .filter { it.child("isactive").getValue(Boolean::class.java) == true }
                        .mapNotNull { child -> child.getValue(Card::class.java)?.let { child.key to it } }
                        .toList()

                Log.d("MyLog", "mGifts: ${mGifts.toString()}")

                uiGiftList.adapter = mGifts?.let { CardAdapter(it) }
                uiProgress.visibility = View.GONE
            }
        }
    }

    private val snapGetGift = object : ValueEventListener {
        override fun onCancelled(error: DatabaseError) {

        }

        override fun onDataChange(snap: DataSnapshot) {
            if (!snap.exists()) {
                // 將card寫入log

                val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.TAIWAN).apply {
                    timeZone = TimeZone.getTimeZone("Asia/Taipei")
                }.format(Date(System.currentTimeMillis()))

                val post = mapOf(
                        "dateTime" to dateTime,
                        "date" to dateTime.substring(0..9),
                        "cardName" to mLotteryGift?.second?.cardName,
                        "scheduleId" to mLotteryGift?.second?.scheduleId,
                        "transNo" to randomAlphaNumeric(5),
                        "hasSync" to false
                )

                FirebaseDatabase.getInstance().getReference(mDbpath["LOG"]).child(mLotteryGiftKey).setValue(post).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
//                        toastMsg("Log 成功")
                        FirebaseDatabase.getInstance().getReference(mDbpath["CARD"]).child(mLotteryGiftKey).updateChildren(mapOf("isactive" to false)).addOnCompleteListener { task ->
                            if (task.isSuccessful) {
//                                toastMsg("使獎品失效 成功")
                                showResult()
                            } else {
//                                toastMsg("使獎品失效 失敗")
                            }
                        }
                    } else {
//                        toastMsg("Log 失敗")
                    }
                }
            } else {
                // 顯示此獎品已抽過，請重新抽
                toastMsg("此獎品已抽過，請重抽")
            }
        }
    }

    fun showResult() {
        var dialog: AlertDialog? = null
        var builder: AlertDialog.Builder? = null
        val view = LayoutInflater.from(this@LotteryActivity).inflate(R.layout.dialog_lottery_success, null)

        //自定義layout中的元件
        val uiName = view.findViewById(R.id.tv_name) as TextView
        uiName.text = mLotteryGift?.second?.cardName

        builder = AlertDialog.Builder(this@LotteryActivity, R.style.AhaDialog)
        builder.setView(view)
        dialog = builder.create()
        dialog?.show()

        uiProgress.visibility = View.GONE
        uiStart.isClickable = true
    }

    fun randomAlphaNumeric(length: Int): String = StringBuilder().run {
        val chars = SOURCE_ORDER_NO.toCharArray()
        val random = Random()
        for (i in 0 until length) {
            append(chars[random.nextInt(chars.size)])
        }
        toString()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lottery)
        uiProgress.visibility = View.VISIBLE
        mMode = intent.getStringExtra(EXTRA_DB_MODE)
        if (mMode == "release") {
            mDbpath["CARD"] = ReferenceManager.GAME_CARD_REF
            mDbpath["LOG"] = ReferenceManager.GAME_LOG_REF
        } else {
            mDbpath["CARD"] = ReferenceManager.TEST_CARD_REF
            mDbpath["LOG"] = ReferenceManager.TEST_LOG_REF
        }
        mAuth.addAuthStateListener(this)
        uiStart.isClickable = false
        uiGiftList.apply {
            setHasFixedSize(true)
            layoutManager = GridLayoutManager(this@LotteryActivity, 4)
        }

        uiStart.setOnClickListener {
            if (uiProgress.visibility != View.VISIBLE) {
                uiProgress.visibility = View.VISIBLE
                uiStart.isClickable = false
                if (mGifts!!.isNotEmpty()) {
                    mLotteryGift = mGifts?.shuffled()?.take(1)?.get(0)
                    mLotteryGiftKey = mLotteryGift?.first ?: ""
                    Log.d("MyLog", mLotteryGiftKey)

                    if (mLotteryGiftKey != "") {
                        refGetGift = FirebaseDatabase.getInstance().getReference(mDbpath["LOG"]).child(mLotteryGiftKey)
                        refGetGift.addListenerForSingleValueEvent(snapGetGift)
                    } else {
                        toastMsg("暫無資料，請稍後再試！")
                    }
                } else {
                    toastMsg("暫無資料，請稍後再試！")
                }
            }
        }
    }

    override fun onAuthStateChanged(p0: FirebaseAuth) {
        val user = mAuth.currentUser
        if (user != null) {
            val scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID)

            Log.d("MyLog", "scheduleId: $scheduleId")
            refGift = FirebaseDatabase.getInstance().getReference(mDbpath["CARD"]).orderByChild("scheduleId").equalTo(scheduleId)
            refGift.addValueEventListener(snapGift)

            mAuth.removeAuthStateListener(this)
        } else {
            toastMsg("暫時無法登入，請重新開啟程式")
        }
    }

    fun toastMsg(msg: String) {
        Toast.makeText(this@LotteryActivity, msg, Toast.LENGTH_SHORT).show()
        uiStart.isClickable = true
        uiProgress.visibility = View.GONE
    }

    private class CardAdapter internal constructor(val items: List<Pair<String, Card>>) : RecyclerView.Adapter<CardHolder>() {
        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardHolder = CardHolder(LayoutInflater.from(parent.context), parent)

        override fun onBindViewHolder(holder: CardHolder, position: Int) = with(holder) {
            val item = items[position].second
            val key = items[position].first

            uiName.text = item.cardName
        }
    }

    companion object {
        private const val TAG = "LotteryActivity"
        private const val EXTRA_SCHEDULE_ID = "EXTRA_SCHEDULE_ID"
        private const val EXTRA_DB_MODE = "EXTRA_DB_MODE"
        private const val SOURCE_ORDER_NO = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

        fun createIntent(packageContext: Context, scheduleId: String, dbMode: String): Intent
                = Intent(packageContext, LotteryActivity::class.java).also {
            it.putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            it.putExtra(EXTRA_DB_MODE, dbMode)
        }
    }
}
