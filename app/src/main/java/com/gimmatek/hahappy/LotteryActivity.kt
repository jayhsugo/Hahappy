package com.gimmatek.hahappy

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
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
import android.content.DialogInterface
import android.support.v4.app.DialogFragment


class LotteryActivity : AppCompatActivity(), FirebaseAuth.AuthStateListener {

    private val uiGiftList: RecyclerView by lazy { findViewById<RecyclerView>(R.id.list_gift) }
    private val uiStart: ImageView by lazy { findViewById<ImageView>(R.id.iv_start) }
    private val mAuth = FirebaseAuth.getInstance()
    private lateinit var refGift: Query
    private lateinit var refItem: DatabaseReference
    private lateinit var refGetGift: DatabaseReference
    private var mGifts: List<Pair<String, Card>>? = null
    private var mItems: List<Pair<String, Item>>? = null
    var mItemsMap: HashMap<String, Item> = HashMap()
    private var mMode: String = ""
    private var mScheduleId: String? = ""
    private var mLotteryGiftKey: String = ""
    private var mLotteryGift: Pair<String, Card>? = null
    private var mDbpath: MutableMap<String, String> = mutableMapOf()
    private val uiProgress: ProgressBar by lazy { findViewById<ProgressBar>(R.id.progress_bar) }

    private val snapItems = object : ValueEventListener {
        override fun onCancelled(error: DatabaseError) {

        }

        override fun onDataChange(snap: DataSnapshot) {
            if (snap.exists()) {
                mItems = snap.children.asSequence()
                        .mapNotNull { child -> child.getValue(Item::class.java)?.let { child.key to it }}
                        .toList()

                if (mItems != null) {
                    try {
                        for (i in (mItems as List<Pair<String, Item>>).iterator()) {
                            mItemsMap[i.first] = i.second
                        }
                    } catch (e: Exception) {
                        Log.d("MyLog", e.toString())
                    }
                }

                Log.d("MyLog", "mItems: ${mItems.toString()}")
                Log.d("MyLog", "mItemsMap: $mItemsMap")

                uiGiftList.adapter = mItems?.let { ItemsAdapter(it) }

                refGift = FirebaseDatabase.getInstance().getReference(mDbpath["CARD"]).orderByChild("scheduleId").equalTo(mScheduleId)
                refGift.addValueEventListener(snapGift)
            }
        }
    }

    private val snapGift = object : ValueEventListener {
        override fun onCancelled(error: DatabaseError) {

        }

        override fun onDataChange(snap: DataSnapshot) {
            if (snap.exists()) {
                mGifts = snap.children.asSequence()
                        .filter { it.child("isactive").getValue(Boolean::class.java) == true }
                        .mapNotNull { child -> child.getValue(Card::class.java)?.let { child.key to it } }
                        .toList()

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

                val mTransNo = randomAlphaNumeric(5)

                val post = mapOf(
                        "dateTime" to dateTime,
                        "date" to dateTime.substring(0..9),
                        "cardName" to mLotteryGift?.second?.cardName,
                        "mScheduleId" to mLotteryGift?.second?.scheduleId,
                        "transNo" to mTransNo,
                        "hasSync" to false
                )

                FirebaseDatabase.getInstance().getReference(mDbpath["LOG"]).child(mLotteryGiftKey).setValue(post).addOnCompleteListener { task1 ->
                    if (task1.isSuccessful) {
//                        toastMsg("Log 成功")
                        FirebaseDatabase.getInstance().getReference(mDbpath["CARD"]).child(mLotteryGiftKey).updateChildren(mapOf("isactive" to false)).addOnCompleteListener { task2 ->
                            if (task2.isSuccessful) {
//                                toastMsg("使獎品失效 成功")
                                showResult(mTransNo)
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

    fun showResult(transNo: String) {
//        val dialog: AlertDialog?
//        val builder: AlertDialog.Builder = AlertDialog.Builder(this@LotteryActivity, R.style.AhaDialog)
//        val view = LayoutInflater.from(this@LotteryActivity).inflate(R.layout.dialog_lottery_success, null)
//
//        //自定義layout中的元件
//        val uiName = view.findViewById(R.id.tv_name) as TextView
//        val uiCardNo = view.findViewById(R.id.tv_cardNo) as TextView
//        val uiTransNo = view.findViewById(R.id.tv_transNo) as TextView
//        val uiCheck = view.findViewById(R.id.btn_check) as Button
//        val key = mLotteryGift?.second?.itemId
//
//        uiName.text = mLotteryGift?.second?.cardName
//        uiCardNo.text = mItemsMap[key]?.cardNo.toString()
//        uiTransNo.text = transNo
//
//        dialog = builder.setView(view).setCancelable(false).create()

//        uiCheck.setOnClickListener {
//            dialog.cancel()
//            uiStart.isClickable = true
//        }
//
//        if (!this.isDestroyed) {
//            try {
//                dialog.show()
//            } catch (e: Exception) {
//                Log.d("MyLog", "dialog.show() NG: $e")
//            }
//
//        }

        val name = mLotteryGift?.second?.cardName.toString()
        val key = mLotteryGift?.second?.itemId
        val cardNo = mItemsMap[key]?.cardNo.toString()

        val dd: MyDialog = MyDialog(name, cardNo, transNo)
        dd.show(fragmentManager, "MyDialog")
        uiStart.isClickable = true

        uiProgress.visibility = View.GONE
        mLotteryGiftKey = ""
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
    }

    fun toastMsg(msg: String) {
        Toast.makeText(this@LotteryActivity, msg, Toast.LENGTH_SHORT).show()
        uiStart.isClickable = true
        uiProgress.visibility = View.GONE
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

    @SuppressLint("ValidFragment")
    class MyDialog internal constructor(name: String, cardNo: String, transNo: String): DialogFragment() {
        val x = name
        val y = cardNo
        val z = transNo

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

            return super.onCreateDialog(savedInstanceState)

            val builder = AlertDialog.Builder(activity)
            // Get the layout inflater
            val inflater = activity.layoutInflater
            val view = inflater.inflate(R.layout.dialog_lottery_success, null)
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout

            //自定義layout中的元件
            val uiName = view.findViewById(R.id.tv_name) as TextView
            val uiCardNo = view.findViewById(R.id.tv_cardNo) as TextView
            val uiTransNo = view.findViewById(R.id.tv_transNo) as TextView
            val uiCheck = view.findViewById(R.id.btn_check) as Button

            uiName.text = x
            uiCardNo.text = y
            uiTransNo.text = z

            builder.setView(view)
                    // Add action buttons

            uiCheck.setOnClickListener {
                builder.show().cancel()
            }

            return builder.create()
        }

    }

    companion object {
        private const val TAG = "LotteryActivity"
        private const val EXTRA_SCHEDULE_ID = "EXTRA_SCHEDULE_ID"
        private const val EXTRA_DB_MODE = "EXTRA_DB_MODE"
        private const val SOURCE_ORDER_NO = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"

        fun createIntent(packageContext: Context, scheduleId: String, dbMode: String): Intent
                = Intent(packageContext, LotteryActivity::class.java).also {
            it.putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            it.putExtra(EXTRA_DB_MODE, dbMode)
        }
    }
}
