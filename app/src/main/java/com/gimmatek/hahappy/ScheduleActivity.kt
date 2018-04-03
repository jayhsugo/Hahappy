package com.gimmatek.hahappy

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityOptionsCompat
import android.support.v4.view.ViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import com.gimmatek.hahappy.adapter.ScheduleEventListener
import com.gimmatek.hahappy.adapter.ScheduleHolder
import com.gimmatek.hahappy.model.Schedule
import com.gimmatek.hahappy.utils.ReferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ScheduleActivity : AppCompatActivity(), FirebaseAuth.AuthStateListener, ScheduleEventListener  {



    private val uiScheduleList: RecyclerView by lazy { findViewById<RecyclerView>(R.id.list_schedule) }
    private val mAuth = FirebaseAuth.getInstance()
    private lateinit var refSchedule: DatabaseReference
    private var mSchedules: List<Pair<String, Schedule>>? = null
    private var mMode: String = ""

    private val snapSchedule = object : ValueEventListener {
        override fun onCancelled(error: DatabaseError) {

        }

        override fun onDataChange(snap: DataSnapshot) {
            mSchedules = snap.children.asSequence()
                    .filter { it.child("isactive").getValue(Boolean::class.java) == true }
                    .mapNotNull { child -> child.getValue(Schedule::class.java)?.let { child.key to it } }
                    .toList()

            uiScheduleList.adapter = mSchedules?.let { ScheduleAdapter(it, this@ScheduleActivity) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_schedule)
        mAuth.addAuthStateListener(this)
        uiScheduleList.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@ScheduleActivity)
        }
        mMode = intent.getStringExtra(EXTRA_DB_MODE)
    }

    override fun onAuthStateChanged(p0: FirebaseAuth) {
        val user = mAuth.currentUser
        if (user != null) {
            val database = FirebaseDatabase.getInstance()

            val dbPath = if (mMode == "debug") {
                ReferenceManager.TEST_SCHEDULE_REF
            } else {
                ReferenceManager.GAME_SCHEDULE_REF
            }

            refSchedule = database.getReference(dbPath).apply { addValueEventListener(snapSchedule) }

            mAuth.removeAuthStateListener(this)
        } else {
            Toast.makeText(this@ScheduleActivity, "暫時無法登入，請重新開啟程式", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mAuth.removeAuthStateListener(this)
        refSchedule.removeEventListener(snapSchedule)
    }

    override fun onScheduleSelect(scheduleId: String) {
        startActivity(LotteryActivity.createIntent(this, scheduleId, mMode))
    }

    private class ScheduleAdapter internal constructor(val items: List<Pair<String, Schedule>>, private val eventListener: ScheduleEventListener) : RecyclerView.Adapter<ScheduleHolder>() {
        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleHolder = ScheduleHolder(LayoutInflater.from(parent.context), parent)

        override fun onBindViewHolder(holder: ScheduleHolder, position: Int) = with(holder) {
            val item = items[position].second
            val key = items[position].first

            uiDate.text = item.date
            uiName.text = item.name
            itemView.setOnClickListener { eventListener.onScheduleSelect(key) }
        }

    }

    companion object {
        private const val TAG = "ScheduleActivity"
        private const val EXTRA_DB_MODE = "EXTRA_DB_MODE"

        fun createIntent(packageContext: Context, dbMode: String): Intent = Intent(packageContext, ScheduleActivity::class.java).also {
            it.putExtra(EXTRA_DB_MODE, dbMode)
        }

    }
}
