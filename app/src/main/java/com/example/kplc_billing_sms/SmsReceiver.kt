package com.example.kplc_billing_sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony



class SmsReceiver:BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {

        //Check if the broadcast from the android system is about an sms received
        //If not exit the function
        if (!intent?.action.equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) return

        //Extract the message from the intent passed by the android system
        val extractMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        //Iterate over the messages and print he originating address and message body
        extractMessages.forEach { smsMessage ->

            //Filter the intents based on the originating address
            //Check the origin of the message
            if( smsMessage.displayOriginatingAddress == "97771" ) {
                println("${smsMessage.displayOriginatingAddress} -> ${smsMessage.displayMessageBody}")
            } else return
        }
    }
}
