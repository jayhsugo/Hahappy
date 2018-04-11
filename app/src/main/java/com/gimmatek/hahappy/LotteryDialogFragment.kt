package com.gimmatek.hahappy

import android.app.Dialog
import android.app.DialogFragment
import android.app.FragmentManager
import android.app.FragmentTransaction
import android.os.Bundle
import android.widget.TextView
import android.widget.Button
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.View


/**
 * Created by USER on 2018/4/10.
 */
class LotteryDialogFragment : DialogFragment() {

    private var mName: String = ""
    private var mCardNo: String = ""
    private var mTransNo: String = ""

    fun setValue(name: String, cardNo: String, transNo: String) {
        mName = name
        mCardNo = cardNo
        mTransNo = transNo
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity)
        // Get the layout inflater
        val inflater = activity.layoutInflater
        val view = inflater.inflate(R.layout.dialog_lottery_success, null)
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout

        val uiName = view.findViewById<TextView>(R.id.tv_name)
        val uiCardNo = view.findViewById<TextView>(R.id.tv_cardNo)
        val uiTransNo = view.findViewById<TextView>(R.id.tv_transNo)
        val uiCheck = view.findViewById<Button>(R.id.btn_check)

        uiName.text = mName
        uiCardNo.text = mCardNo
        uiTransNo.text = mTransNo

        builder.setView(view)

        isCancelable = false

        uiCheck.setOnClickListener {
            dialog.dismiss()
        }

        return builder.create()
    }

    override fun show(manager: FragmentManager?, tag: String?) {
        if (!isAdded) {
            val ft: FragmentTransaction? = manager?.beginTransaction()
            ft?.add(this, tag)
            ft?.commitAllowingStateLoss()
        }
    }
}