package com.fxlibs.countdown

import android.content.Context
import android.os.CountDownTimer
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import com.fxlibs.countdown.databinding.DialogCountDownBinding

class CountDownDialog(
    context: Context,
    title: String,
    description: String,
    textAction: String,
    onAction: ((dialog:CountDownDialog) -> Unit)?,
    onTimeout: ((dialog:CountDownDialog) -> Unit)?,
)  {

    private var timer:CountDownTimer
    private var alertDialog:AlertDialog
    private var binding: DialogCountDownBinding = DialogCountDownBinding.inflate(LayoutInflater.from(context))

    init {
        binding.txtTitle.text = title
        binding.txtProgress.text = title
        binding.description.text = description
        binding.submitButton.text = textAction
        binding.submitButton.setOnClickListener {
            binding.submitButton.isEnabled = false
            onAction?.invoke(this)
        }
        alertDialog = AlertDialog.Builder(context).setView(binding.root).create()
        timer = object: CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                binding.txtProgress.text = (millisUntilFinished/1000).toInt().toString()
            }

            override fun onFinish() {
                onTimeout?.invoke(this@CountDownDialog)
                alertDialog.dismiss()
            }
        }
    }

    fun setActionEnable(enable:Boolean) {
        binding.submitButton.isVisible = enable
    }


    fun show() {
        timer.start()
        alertDialog.show()
    }

    fun dismiss() {
        timer.cancel()
        alertDialog.dismiss()
    }

}