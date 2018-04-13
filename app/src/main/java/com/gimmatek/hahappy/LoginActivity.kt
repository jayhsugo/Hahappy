package com.gimmatek.hahappy

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.*
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity(), RadioGroup.OnCheckedChangeListener {

    private val mAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var mAuthListener: FirebaseAuth.AuthStateListener
    private val uiLogo: ImageView by lazy { findViewById<ImageView>(R.id.iv_logo) }
    private val uiTitle: ImageView by lazy { findViewById<ImageView>(R.id.iv_title) }
    private val uiLoginSoon: ImageView by lazy { findViewById<ImageView>(R.id.tv_title) }
    private val uiEmail: EditText by lazy { findViewById<EditText>(R.id.et_email) }
    private val uiPassword: EditText by lazy { findViewById<EditText>(R.id.et_password) }
    private val uiLogin: Button by lazy { findViewById<Button>(R.id.btn_login) }
    private val uiVersion: TextView by lazy { findViewById<TextView>(R.id.tv_version) }
    private lateinit var uiRbBtn: Array<RadioButton>
    private val uiDbSelect: RadioGroup by lazy { findViewById<RadioGroup>(R.id.rg_db_select) }
    private var mEmail: String = ""
    private var mPassword: String = ""
    private var mFuid: String? = null
    private var mDbPath: String = ""
    private val mLogos: Array<Int> = arrayOf(R.drawable.aha_logo_debug, R.drawable.aha_logo)
    private val mTitles: Array<Int> = arrayOf(R.drawable.aha_logo_text_debug, R.drawable.aha_logo_text)
    private val mModes: Array<String> = arrayOf("debug", "release")
    private val uiProgress: ProgressBar by lazy { findViewById<ProgressBar>(R.id.progress_bar) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        uiVersion.text = String.format("%s", BuildConfig.VERSION_NAME)

        uiRbBtn = arrayOf(findViewById(R.id.rb_debug), findViewById(R.id.rb_release))
        uiDbSelect.setOnCheckedChangeListener(this)

        mAuthListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                Log.d("onAuthStateChanged", "登入:" + user.uid)
                mFuid = user.uid
            } else {
                Log.d("onAuthStateChanged", "已登出")
            }
        }

        uiLogin.setOnClickListener {
            if (uiProgress.visibility != View.VISIBLE) {
                uiProgress.visibility = View.VISIBLE
                mEmail = uiEmail.text.toString()
                mPassword = uiPassword.text.toString()
                if (mDbPath != "") {
                    if (mEmail.isNotEmpty() && mPassword.isNotEmpty()) {
                        mAuth.signInWithEmailAndPassword(mEmail, mPassword).addOnCompleteListener {
                            if (it.isSuccessful) {
                                Toast.makeText(this@LoginActivity, "登入成功", Toast.LENGTH_SHORT).show()
                                uiProgress.visibility = View.GONE
                                startActivity(ScheduleActivity.createIntent(this@LoginActivity, mDbPath))
                            } else {
                                Toast.makeText(this@LoginActivity, "登入失敗", Toast.LENGTH_SHORT).show()
                                uiProgress.visibility = View.GONE
                            }
                        }
                    } else {
                        Toast.makeText(this@LoginActivity, "請輸入帳號密碼", Toast.LENGTH_SHORT).show()
                        uiProgress.visibility = View.GONE
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "請選擇環境", Toast.LENGTH_SHORT).show()
                    uiProgress.visibility = View.GONE
                }
            }
        }

        uiLoginSoon.setOnLongClickListener {
            if (uiProgress.visibility != View.VISIBLE) {
                uiProgress.visibility = View.VISIBLE
                if (mDbPath != "") {
                    mAuth.signInWithEmailAndPassword("jayhsugo@gmail.com", "hahago").addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this@LoginActivity, "登入成功", Toast.LENGTH_SHORT).show()
                            uiProgress.visibility = View.GONE
                            startActivity(ScheduleActivity.createIntent(this@LoginActivity, mDbPath))
                        } else {
                            Toast.makeText(this@LoginActivity, "登入失敗", Toast.LENGTH_SHORT).show()
                            uiProgress.visibility = View.GONE
                        }
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "請選擇環境", Toast.LENGTH_SHORT).show()
                    uiProgress.visibility = View.GONE
                }
            }
            return@setOnLongClickListener true
        }
    }

    override fun onStart() {
        super.onStart()
        mAuth.addAuthStateListener(mAuthListener)
    }

    override fun onStop() {
        super.onStop()
        mAuth.removeAuthStateListener(mAuthListener)
    }

    override fun onCheckedChanged(p0: RadioGroup?, p1: Int) {
        val index = uiDbSelect.indexOfChild(uiDbSelect.findViewById(p1))
        uiLogo.setImageResource(mLogos[index])
        uiTitle.setImageResource(mTitles[index])
        mDbPath = mModes[index]
    }
}