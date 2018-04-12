package com.gimmatek.hahappy

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import com.gimmatek.hahappy.adapter.UserFuidHolder
import com.gimmatek.hahappy.utils.ReferenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class UserFuidListActivity : AppCompatActivity(), FirebaseAuth.AuthStateListener  {

    private val uiUserFuidList: RecyclerView by lazy { findViewById<RecyclerView>(R.id.list_user) }
    private val uiStartQR: Button by lazy { findViewById<Button>(R.id.btn_qr) }
    private val mAuth = FirebaseAuth.getInstance()
    private lateinit var refUser: DatabaseReference
    private var mUserList: List<String> = listOf()
    private var mSchedulesId: String = ""
    private var mMode: String = ""
    private var mDbpath: MutableMap<String, String> = mutableMapOf()
    private val uiProgress: ProgressBar by lazy { findViewById<ProgressBar>(R.id.progress_bar) }

    private val snapUser = object : ValueEventListener {
        override fun onCancelled(error: DatabaseError) {

        }

        override fun onDataChange(snap: DataSnapshot) {
            mUserList = snap.children.mapNotNull { it.key }.toList()

            if (mUserList.isEmpty()) {
                Toast.makeText(this@UserFuidListActivity, "暫無資料", Toast.LENGTH_SHORT).show()
            }

            uiUserFuidList.adapter = mUserList.let { UserAdapter(it) }
            uiProgress.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fuid_list)
        uiProgress.visibility = View.VISIBLE

        mAuth.addAuthStateListener(this)
        uiUserFuidList.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@UserFuidListActivity)
        }

        mMode = intent.getStringExtra(EXTRA_DB_MODE)
        mSchedulesId = intent.getStringExtra(EXTRA_SCHEDULE_ID)

        if (mMode == "release") {
            mDbpath["COIN"] = ReferenceManager.GAME_COIN_REF
        } else {
            mDbpath["COIN"] = ReferenceManager.TEST_COIN_REF
        }

        uiStartQR.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this@UserFuidListActivity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(Intent(this@UserFuidListActivity, ScanQRCodeActivity::class.java), REQUEST_CODE_SCAN_QRCODE)
            } else {
                ActivityCompat.requestPermissions(this@UserFuidListActivity, arrayOf(Manifest.permission.CAMERA), REQUEST_PERMISIION)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_SCAN_QRCODE -> if (data != null) {
                    val mUserFuid = data.getStringExtra(ScanQRCodeActivity.RESULT_DECODE)
                    FirebaseDatabase.getInstance().getReference("${mDbpath["COIN"]}${mSchedulesId}/$mUserFuid").setValue(true).addOnCompleteListener {
                        if (it.isSuccessful) {
                            Toast.makeText(this@UserFuidListActivity, "紀錄成功", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@UserFuidListActivity, "紀錄失敗，請重試", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    override fun onAuthStateChanged(p0: FirebaseAuth) {
        val user = mAuth.currentUser
        if (user != null) {
            val database = FirebaseDatabase.getInstance()

            refUser = database.getReference(mDbpath["COIN"]).child(mSchedulesId).apply { addValueEventListener(snapUser) }

            mAuth.removeAuthStateListener(this)
        } else {
            Toast.makeText(this@UserFuidListActivity, "暫時無法登入，請重新開啟程式", Toast.LENGTH_SHORT).show()
            uiProgress.visibility = View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        refUser.removeEventListener(snapUser)
    }

    private class UserAdapter internal constructor(val items: List<String>) : RecyclerView.Adapter<UserFuidHolder>() {
        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserFuidHolder = UserFuidHolder(LayoutInflater.from(parent.context), parent)

        override fun onBindViewHolder(holder: UserFuidHolder, position: Int) = with(holder) {
            val item = items[position]
            uiName.text = item
        }

    }

    companion object {
        private const val TAG = "UserFuidListActivity"
        private const val EXTRA_DB_MODE = "EXTRA_DB_MODE"
        private const val EXTRA_SCHEDULE_ID = "EXTRA_SCHEDULE_ID"
        private const val REQUEST_CODE_SCAN_QRCODE = 100
        private const val REQUEST_PERMISIION = 300

        fun createIntent(packageContext: Context, dbMode: String, schedule: String): Intent = Intent(packageContext, UserFuidListActivity::class.java).also {
            it.putExtra(EXTRA_DB_MODE, dbMode)
            it.putExtra(EXTRA_SCHEDULE_ID, schedule)
        }
    }
}
