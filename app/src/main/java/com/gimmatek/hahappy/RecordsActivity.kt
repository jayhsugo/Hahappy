package com.gimmatek.hahappy

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.gimmatek.hahappy.adapter.CardHolder
import com.gimmatek.hahappy.adapter.ItemEventListener
import com.gimmatek.hahappy.adapter.RecordHolder

import com.gimmatek.hahappy.model.GameLog
import com.gimmatek.hahappy.model.Item
import com.gimmatek.hahappy.utils.ReferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class RecordsActivity : AppCompatActivity(), FirebaseAuth.AuthStateListener, ItemEventListener {

    private val uiGiftList: RecyclerView by lazy { findViewById<RecyclerView>(R.id.list_gift) }
    private val mAuth = FirebaseAuth.getInstance()
    private lateinit var refRecord: Query
    private lateinit var refItem: DatabaseReference
    private var mRecords: List<Pair<Int, List<GameLog>>>? = null
    private var mItems: List<Pair<Int?, Item>>? = null
    private var mMode: String = ""
    private var mScheduleId: String = ""
    private var mDbpath: MutableMap<String, String> = mutableMapOf()
    private val uiProgress: ProgressBar by lazy { findViewById<ProgressBar>(R.id.progress_bar) }

    private val snapItems = object : ValueEventListener {

        override fun onCancelled(error: DatabaseError) {

        }

        override fun onDataChange(snap: DataSnapshot) {
            if (snap.exists()) {
                mItems = snap.children.asSequence()
                        .mapNotNull { child -> child.getValue(Item::class.java)?.let { child.child("cardNo").getValue(Int::class.java) to it }}
                        .toList()

                mItems?.forEach {
                    it.second.rest = it.second.total
                }

                Log.d("MyLog", "mItems: $mItems")

                refRecord = FirebaseDatabase.getInstance().getReference(mDbpath["LOG"]).orderByChild("scheduleId").equalTo(mScheduleId)
                refRecord.addValueEventListener(snapRecord)
            }
        }
    }

    private val snapRecord = object : ValueEventListener {
        override fun onCancelled(error: DatabaseError) {

        }

        override fun onDataChange(snap: DataSnapshot) {
            if (snap.exists()) {
                mRecords = snap.children.asSequence()
                        .mapNotNull { child -> child.getValue(GameLog::class.java) }
                        .groupBy { it.cardNo }
                        .toList()

                mRecords?.forEach {
                    mItems?.get((it.first-1))?.second?.rest = mItems?.get((it.first-1))?.second?.total!! - it.second.size
                }

                Log.d("MyLog", "mRecords: $mRecords")

                uiGiftList.adapter = mItems?.let { RecordAdapter(mScheduleId, it, this@RecordsActivity) }
                uiProgress.visibility = View.GONE
            } else {
                uiGiftList.adapter = mItems?.let { RecordAdapter(mScheduleId, it, this@RecordsActivity) }
                uiProgress.visibility = View.GONE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_records)
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
        uiGiftList.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@RecordsActivity)
        }
    }

    override fun onAuthStateChanged(p0: FirebaseAuth) {
        val user = mAuth.currentUser
        if (user != null) {

            refItem = FirebaseDatabase.getInstance().getReference(mDbpath["ITEMS"]).child(mScheduleId)
            refItem.addValueEventListener(snapItems)

            mAuth.removeAuthStateListener(this)
        } else {
            uiProgress.visibility = View.GONE
            Toast.makeText(this@RecordsActivity, "暫時無法登入，請重新開啟程式", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        refRecord.removeEventListener(snapRecord)
    }

    override fun onItemSelect(cardNo: Int, scheduleId: String) {
        startActivity(RecordsDetailActivity.createIntent(this, cardNo, scheduleId, mMode))
    }

    private class RecordAdapter internal constructor(val scheduleId: String, val items: List<Pair<Int?, Item>>, private val eventListener: ItemEventListener) : RecyclerView.Adapter<RecordHolder>() {
        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordHolder = RecordHolder(LayoutInflater.from(parent.context), parent)

        override fun onBindViewHolder(holder: RecordHolder, position: Int) = with(holder) {
            val item = items[position].second
            val cardNo = item.cardNo

            uiCardNo.text = cardNo.toString()
            uiName.text = item.cardName
            uiTotal.text = item.total.toString()
            uiRest.text = item.rest.toString()

            itemView.setOnClickListener { eventListener.onItemSelect(cardNo, scheduleId) }
        }
    }

    companion object {
        private const val TAG = "RecordsActivity"
        private const val EXTRA_SCHEDULE_ID = "EXTRA_SCHEDULE_ID"
        private const val EXTRA_DB_MODE = "EXTRA_DB_MODE"

        fun createIntent(packageContext: Context, scheduleId: String, dbMode: String): Intent
                = Intent(packageContext, RecordsActivity::class.java).also {
            it.putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            it.putExtra(EXTRA_DB_MODE, dbMode)
        }
    }
}
