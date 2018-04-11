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
import com.gimmatek.hahappy.adapter.RecordHolder
import com.gimmatek.hahappy.model.GameLog
import com.gimmatek.hahappy.utils.ReferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class RecordsDetailActivity : AppCompatActivity(), FirebaseAuth.AuthStateListener {

    private val uiGiftList: RecyclerView by lazy { findViewById<RecyclerView>(R.id.list_gift) }
    private val mAuth = FirebaseAuth.getInstance()
    private lateinit var refRecord: Query
    private var mRecords: List<Pair<String, GameLog>>? = null
    private var mMode: String = ""
    private var mCardNo: Int = 0
    private var mScheduleId: String? = ""
    private var mDbpath: MutableMap<String, String> = mutableMapOf()
    private val uiProgress: ProgressBar by lazy { findViewById<ProgressBar>(R.id.progress_bar) }

    private val snapRecord = object : ValueEventListener {
        override fun onCancelled(error: DatabaseError) {

        }

        override fun onDataChange(snap: DataSnapshot) {
            if (snap.exists()) {
                mRecords = snap.children.asSequence()
                        .mapNotNull { child -> child.getValue(GameLog::class.java)?.let { child.key to it } }
                        .filter { it.second.cardNo == mCardNo }
                        .sortedBy { it.second.cardNo }
                        .toList()

                Log.d("MyLog", "mRecords: $mRecords")

                if (mRecords!!.isEmpty()) {
                    uiProgress.visibility = View.GONE
                    Toast.makeText(this@RecordsDetailActivity, "目前暫無紀錄！", Toast.LENGTH_SHORT).show()
                } else {
                    uiGiftList.adapter = mRecords?.let { RecordAdapter(it) }
                    uiProgress.visibility = View.GONE
                }
            } else {
                uiProgress.visibility = View.GONE
                Toast.makeText(this@RecordsDetailActivity, "目前暫無紀錄！", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_records_detail)
        uiProgress.visibility = View.VISIBLE
        mCardNo = intent.getIntExtra(EXTRA_CARD_NO, 0)
        mMode = intent.getStringExtra(EXTRA_DB_MODE)
        mScheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID)

        if (mMode == "release") {
            mDbpath["LOG"] = ReferenceManager.GAME_LOG_REF
        } else {
            mDbpath["LOG"] = ReferenceManager.TEST_LOG_REF
        }

        mAuth.addAuthStateListener(this)
        uiGiftList.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@RecordsDetailActivity)
        }
    }

    override fun onAuthStateChanged(p0: FirebaseAuth) {
        val user = mAuth.currentUser
        if (user != null) {

            refRecord = FirebaseDatabase.getInstance().getReference(mDbpath["LOG"]).orderByChild("scheduleId").equalTo(mScheduleId)
            refRecord.addValueEventListener(snapRecord)

            mAuth.removeAuthStateListener(this)
        } else {
            uiProgress.visibility = View.GONE
            Toast.makeText(this@RecordsDetailActivity, "暫時無法登入，請重新開啟程式", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        refRecord.removeEventListener(snapRecord)
    }



    private class RecordAdapter internal constructor(val items: List<Pair<String, GameLog>>) : RecyclerView.Adapter<CardHolder>() {
        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardHolder = CardHolder(LayoutInflater.from(parent.context), parent)

        override fun onBindViewHolder(holder: CardHolder, position: Int) = with(holder) {
            val item = items[position].second
//            val key = items[position].first

            uiCardNo.text = item.cardNo.toString()
            uiName.text = item.cardName
            uiTransNo.text = item.transNo
        }
    }

    companion object {
        private const val TAG = "RecordsDetailActivity"
        private const val EXTRA_CARD_NO = "EXTRA_CARD_NO"
        private const val EXTRA_SCHEDULE_ID = "EXTRA_SCHEDULE_ID"
        private const val EXTRA_DB_MODE = "EXTRA_DB_MODE"

        fun createIntent(packageContext: Context, cardNo: Int, scheduleId: String, dbMode: String): Intent
                = Intent(packageContext, RecordsDetailActivity::class.java).also {
            it.putExtra(EXTRA_CARD_NO, cardNo)
            it.putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            it.putExtra(EXTRA_DB_MODE, dbMode)
        }
    }
}
