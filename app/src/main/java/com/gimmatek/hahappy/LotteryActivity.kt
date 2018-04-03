package com.gimmatek.hahappy

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import com.gimmatek.hahappy.adapter.CardHolder
import com.gimmatek.hahappy.model.Card
import com.gimmatek.hahappy.utils.ReferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LotteryActivity : AppCompatActivity(), FirebaseAuth.AuthStateListener {

    private val uiGiftList: RecyclerView by lazy { findViewById<RecyclerView>(R.id.list_gift) }
    private val mAuth = FirebaseAuth.getInstance()
    private lateinit var refGift: DatabaseReference
    private var mGifts: List<Pair<String, Card>>? = null
    private var mMode: String = ""

    private val snapGift = object : ValueEventListener {
        override fun onCancelled(error: DatabaseError) {

        }

        override fun onDataChange(snap: DataSnapshot) {
            mGifts = snap.children.asSequence()
                    .filter { it.child("isactive").getValue(Boolean::class.java) == true }
                    .mapNotNull { child -> child.getValue(Card::class.java)?.let { child.key to it } }
                    .toList()

            Log.d("MyLog", "mGifts: ${mGifts.toString()}")
            uiGiftList.adapter = mGifts?.let { CardAdapter(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lottery)
        mAuth.addAuthStateListener(this)
        uiGiftList.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@LotteryActivity)
        }
        mMode = intent.getStringExtra(EXTRA_DB_MODE)
    }

    override fun onAuthStateChanged(p0: FirebaseAuth) {
        val user = mAuth.currentUser
        if (user != null) {
            val database = FirebaseDatabase.getInstance()
            val scheduleId = intent.getStringExtra(EXTRA_SCHEDULE_ID)

            val dbPath = if (mMode == "debug") {
                ReferenceManager.TEST_CARD_REF
            } else {
                ReferenceManager.GAME_CARD_REF
            }

            Log.d("MyLog", "scheduleId: $scheduleId")
            refGift = database.getReference(dbPath)
            refGift.orderByChild("scheduleId").equalTo(scheduleId).addValueEventListener(snapGift)

            mAuth.removeAuthStateListener(this)
        } else {
            Toast.makeText(this@LotteryActivity, "暫時無法登入，請重新開啟程式", Toast.LENGTH_SHORT).show()
        }
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

        fun createIntent(packageContext: Context, scheduleId: String, dbMode: String): Intent
                = Intent(packageContext, LotteryActivity::class.java).also {
            it.putExtra(EXTRA_SCHEDULE_ID, scheduleId)
            it.putExtra(EXTRA_DB_MODE, dbMode)
        }
    }
}
