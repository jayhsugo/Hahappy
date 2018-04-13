package com.gimmatek.hahappy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.gimmatek.hahappy.adapter.ItemsHolder
import com.gimmatek.hahappy.model.Card
import com.gimmatek.hahappy.model.Item
import com.gimmatek.hahappy.utils.ReferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import android.util.Log
import com.gimmatek.hahappy.model.GameLog

class LotteryActivity : AppCompatActivity(), FirebaseAuth.AuthStateListener {

    private val uiGiftList: RecyclerView by lazy { findViewById<RecyclerView>(R.id.list_gift) }
    private val uiStart: ImageView by lazy { findViewById<ImageView>(R.id.iv_start) }
    private val uiRecord: Button by lazy { findViewById<Button>(R.id.btn_record) }
    private val uiGameover: TextView by lazy { findViewById<TextView>(R.id.tv_gameover) }
    private val mAuth = FirebaseAuth.getInstance()
    private lateinit var refGift: Query
    private lateinit var refCardLogCount: Query
    private lateinit var refItem: DatabaseReference
    private lateinit var refGetGift: DatabaseReference
    private var mGifts: List<Pair<String, Card>> = listOf()
    private var mItems: List<Pair<String, Item>> = listOf()
    private var mItemsMap: Map<String, Item> = mapOf()
    private var mMode: String = ""
    private var mScheduleId: String = ""
    private var mLotteryGiftKey: String = ""
    private var mLotteryGift: Pair<String, Card> = Pair("", Card())
    private var mDbpath: MutableMap<String, String> = mutableMapOf()
    private val uiProgress: ProgressBar by lazy { findViewById<ProgressBar>(R.id.progress_bar) }
    private val dialog = LotteryDialogFragment()

    private val snapItems = object : ValueEventListener {
        override fun onCancelled(error: DatabaseError) {
            toastMsg(error.message)
        }

        override fun onDataChange(snap: DataSnapshot) {
            if (snap.exists()) {

                mItemsMap = snap.children.asSequence()
                        .mapNotNull { child ->
                            child.getValue(Item::class.java)?.let { child.key to it }
                        }.toMap()

                mItems = mItemsMap.toList()

                Log.d("MyLog", "mItems: $mItems")
                Log.d("MyLog", "mItemsMap: $mItemsMap")

                uiGiftList.adapter = mItems.let { ItemsAdapter(it) }

                refGift = FirebaseDatabase.getInstance().getReference(mDbpath["CARD"]).orderByChild("scheduleId").equalTo(mScheduleId)
                refGift.addValueEventListener(snapGift)
            }
        }
    }

    private val snapGift = object : ValueEventListener {
        override fun onCancelled(error: DatabaseError) {
            toastMsg(error.message)
        }

        override fun onDataChange(snap: DataSnapshot) {
            if (snap.exists()) {
                mGifts = snap.children.asSequence()
                        .filter { it.child("isactive").getValue(Boolean::class.java) == true }
                        .mapNotNull { child ->
                            child.getValue(Card::class.java)?.let { card ->
                                mItemsMap.let { itemMap ->
                                    card.cardNo = itemMap[card.itemId]?.cardNo ?: 0
                                }.let { child.key to card }
                            }
                        }
                        .toList()

                if (mGifts.isEmpty()) {
                    uiGameover.visibility = View.VISIBLE
                    uiStart.visibility = View.INVISIBLE
                } else {
                    uiGameover.visibility = View.INVISIBLE
                    uiStart.visibility = View.VISIBLE
                }

                uiProgress.visibility = View.GONE
            } else {
                toastMsg("獎品準備中")
            }
        }
    }

    private val snapGetGift = object : ValueEventListener {
        override fun onCancelled(error: DatabaseError) {
            toastMsg(error.message)
        }

        override fun onDataChange(snap: DataSnapshot) {
            if (!snap.exists()) {
                refCardLogCount = FirebaseDatabase.getInstance().getReference(mDbpath["LOG"]).orderByChild("scheduleId").equalTo(mScheduleId)
                refCardLogCount.addListenerForSingleValueEvent(snapCardLog)
            } else {
                // 顯示此獎品已抽過，請重新抽
                toastMsg("此獎品已抽過，請重抽")
            }
        }
    }

    private val snapCardLog = object : ValueEventListener {
        override fun onCancelled(error: DatabaseError) {
            toastMsg(error.message)
        }

        override fun onDataChange(snap: DataSnapshot) {
            val mCardNo = mLotteryGift.second.cardNo

            if (snap.exists()) {
                val cardLogCount = snap.children.asSequence()
                        .mapNotNull { child -> child.getValue(GameLog::class.java)?.let { child.key to it } }
                        .filter { it.second.cardNo == mCardNo }
                        .count()

                writeGameLog(cardLogCount, mCardNo)
            } else {
                writeGameLog(0, mCardNo)
            }
        }
    }

    fun writeGameLog(cardLogCount: Int, cardNo: Int) {
        val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.TAIWAN).apply {
            timeZone = TimeZone.getTimeZone("Asia/Taipei")
        }.format(Date(System.currentTimeMillis()))

        val mName: String? = mLotteryGift.second.cardName
        val mCount = if (cardLogCount + 1 < 10) {
            "0${cardLogCount + 1}"
        } else {
            "${cardLogCount + 1}"
        }
        val mTransNo = "$cardNo$mCount"
        val post = mapOf(
                "datetime" to dateTime,
                "date" to dateTime.substring(0..9),
                "cardName" to mName,
                "scheduleId" to mScheduleId,
                "transNo" to mTransNo,
                "hasSync" to false,
                "cardNo" to cardNo,
                "isgive" to false
        )

        FirebaseDatabase.getInstance().getReference(mDbpath["LOG"]).child(mLotteryGiftKey).setValue(post).addOnCompleteListener { task1 ->
            if (task1.isSuccessful) {
                FirebaseDatabase.getInstance().getReference(mDbpath["CARD"]).child(mLotteryGiftKey).updateChildren(mapOf("isactive" to false)).addOnCompleteListener { task2 ->
                    if (task2.isSuccessful) {
                        if (mName != null) {
                            showResult(mName, cardNo.toString(), mTransNo)
                        }
                    } else {
                        toastMsg("使獎品失效 失敗")
                    }
                }
            } else {
                toastMsg("GameLog 失敗")
            }
        }
    }

    private fun showResult(name: String, cardNo: String, transNo: String) {

        dialog.setValue(name, cardNo, transNo)

        if (!isDestroyed) {
            dialog.show(fragmentManager, "LotteryDialogFragment")
        }

        uiStart.isClickable = true
        uiProgress.visibility = View.GONE
        mLotteryGiftKey = ""
    }

//    fun randomAlphaNumeric(length: Int): String = StringBuilder().run {
//        val chars = SOURCE_ORDER_NO.toCharArray()
//        val random = Random()
//        for (i in 0 until length) {
//            append(chars[random.nextInt(chars.size)])
//        }
//        toString()
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lottery)
        uiProgress.visibility = View.VISIBLE
        mMode = intent.getStringExtra(EXTRA_DB_MODE)
        mScheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID)

        if (mMode == "release") {
            mDbpath["ITEMS"] = ReferenceManager.GAME_ITEMS_REF
            mDbpath["CARD"] = ReferenceManager.GAME_CARD_REF
            mDbpath["LOG"] = ReferenceManager.GAME_LOG_REF
        } else {
            mDbpath["ITEMS"] = ReferenceManager.TEST_ITEMS_REF
            mDbpath["CARD"] = ReferenceManager.TEST_CARD_REF
            mDbpath["LOG"] = ReferenceManager.TEST_LOG_REF
        }

        mAuth.addAuthStateListener(this)
        uiStart.isClickable = false
        uiGiftList.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@LotteryActivity)
        }

        uiStart.setOnClickListener {
            if (dialog.isAdded) {
                Toast.makeText(this@LotteryActivity, "請稍後", Toast.LENGTH_SHORT).show()
            } else {
                if (uiProgress.visibility != View.VISIBLE) {
                    uiProgress.visibility = View.VISIBLE
                    uiStart.isClickable = false
                    if (mGifts.isNotEmpty()) {
                        mLotteryGift = mGifts.shuffled().take(1)[0]
                        mLotteryGiftKey = mLotteryGift.first
                        Log.d("MyLog", mLotteryGiftKey)

                        if (mLotteryGiftKey != "") {
                            refGetGift = FirebaseDatabase.getInstance().getReference(mDbpath["LOG"]).child(mLotteryGiftKey)
                            refGetGift.addListenerForSingleValueEvent(snapGetGift)
                        } else {
                            toastMsg("暫無資料，請稍後再試！")
                        }
                    } else {
                        toastMsg("獎品已抽完")
                    }

                } else {
                    toastMsg("獎品準備中")
                }
            }
        }

        uiRecord.setOnClickListener {
            if (uiProgress.visibility == View.VISIBLE) {
                Toast.makeText(this@LotteryActivity, "抽獎中請稍後", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(RecordsActivity.createIntent(this, mScheduleId, mMode))
            }
        }
    }

    override fun onAuthStateChanged(p0: FirebaseAuth) {
        val user = mAuth.currentUser
        if (user != null) {

            Log.d("MyLog", "scheduleId: $mScheduleId")
            refItem = FirebaseDatabase.getInstance().getReference(mDbpath["ITEMS"]).child(mScheduleId)
            refItem.addValueEventListener(snapItems)

            mAuth.removeAuthStateListener(this)
        } else {
            toastMsg("暫時無法登入，請重新開啟程式")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        refItem.removeEventListener(snapItems)
        refGift.removeEventListener(snapGift)
    }

    override fun onBackPressed() {
        if (uiProgress.visibility == View.VISIBLE) {
            Toast.makeText(this@LotteryActivity, "抽獎中請稍後", Toast.LENGTH_SHORT).show()
        } else {
            super.onBackPressed()
        }
    }

    fun toastMsg(msg: String) {
        Toast.makeText(this@LotteryActivity, msg, Toast.LENGTH_SHORT).show()
        uiProgress.visibility = View.GONE
        uiStart.isClickable = true
    }

    private class ItemsAdapter internal constructor(val items: List<Pair<String, Item>>) : RecyclerView.Adapter<ItemsHolder>() {
        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemsHolder = ItemsHolder(LayoutInflater.from(parent.context), parent)

        override fun onBindViewHolder(holder: ItemsHolder, position: Int) = with(holder) {
            val item = items[position].second
//            val key = items[position].first

            uiCardNo.text = item.cardNo.toString()
            uiName.text = item.cardName
            uiTotal.text = item.total.toString()
        }
    }

    companion object {
        private const val TAG = "LotteryActivity"
        private const val EXTRA_SCHEDULE_ID = "EXTRA_SCHEDULE_ID"
        private const val EXTRA_DB_MODE = "EXTRA_DB_MODE"
//        private const val SOURCE_ORDER_NO = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"

        fun createIntent(packageContext: Context, scheduleId: String, dbMode: String): Intent
                = Intent(packageContext, LotteryActivity::class.java).also {
            it.putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            it.putExtra(EXTRA_DB_MODE, dbMode)
        }
    }
}
