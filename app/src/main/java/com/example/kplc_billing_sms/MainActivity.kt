package com.example.kplc_billing_sms

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.SmsManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.provider.Telephony
import android.widget.*
import kotlin.system.exitProcess
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.json.JSONObject
import org.json.JSONArray


open class MainActivity : AppCompatActivity() {

    // Initialise  variables useful although the class
    private val sendSmsCode: Int = 1
    private val retrieveSmsCode: Int = 2

    //
    //Entry point to the app
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Test the post
        GlobalScope.launch(Dispatchers.Main) {
            postToServer("http://206.189.207.206/test123.php" ,"initial test")
        }

        // Check the permissions
        checkPermission(Manifest.permission.SEND_SMS,sendSmsCode)
        checkPermission(Manifest.permission.READ_SMS,retrieveSmsCode)
        checkPermission(Manifest.permission.RECEIVE_SMS,3)

        //Initialise variables and assigning ui buttons by linking them to their ids
        val send = findViewById<Button>(R.id.send)
        val retrieve = findViewById<Button>(R.id.retrieve)
        val sendMultiple = findViewById<Button>(R.id.btnSendMultiple)
        val clear = findViewById<Button>(R.id.btnClear)
        val retrieveAccountNumbers = findViewById<Button>(R.id.btnRetreiveAccountNos)

        //Set onclick listener for various functionality
        //Send sms to kplc listener
        send.setOnClickListener {

            //Get the accountInputField by its id and retrieving its text attribute
            val accountInputField: EditText = findViewById(R.id.accountInputField)

            // Value returned is a char sequence convert to string
            val accountNumber = accountInputField.text.toString()
            sendSms(accountNumber)

            //Clear the accountInputField
            accountInputField.setText("")
        }
        //Read the response from inbox listener
        retrieve.setOnClickListener {
            retrieveSms()
        }
        //sendMultiple sms listener
        sendMultiple.setOnClickListener {
            // call the function that sends multiple sms
            sendMultipleSms()
        }
        //clearInbox functionality listener
        clear.setOnClickListener {
            //calling the clearInbox function
            clearInbox()
        }
        //Get account numbers from serer
        retrieveAccountNumbers.setOnClickListener{

            // Scope is an object used for launching coroutines
            // Launch is useful when the coroutine returns nothing
            GlobalScope.launch(Dispatchers.Main) {

                // Async is used when a coroutine is to return something
                val accountNumbers = async {

                    // Call the function that fetches the data from url
                    getServerContent("http://206.189.207.206/tracker/v/andriod.php")
                }
                // Print results to the console
                val txt: String = accountNumbers.await()
                //
                //decode the json string to an array
                val obj = Json.decodeFromString<Array<String>>(txt)

                obj.forEach { account ->
                    sendSms(account)
                }
            }
        }
    }

    //Request for the given permission ?????
    protected  fun checkPermission(permission: String,requestCode: Int){
        //Checking for permission and requesting if not granted
        //
        if (
            ContextCompat.checkSelfPermission(
                this@MainActivity,
                permission
            )!= PackageManager.PERMISSION_GRANTED
            ){
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(permission),
                    requestCode
                )
        }
    }

    //Response retrieval 
    protected fun retrieveSms() {

        //Creat the message array to store the messages
        val message = ArrayList<String>()

        //Define the columns to select
        val projection = arrayOf("address","body")
        //
        // Query the content provider through the content resolver
        val cursor = contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            "address = 97771",
            null,
            Telephony.Sms.Inbox.DEFAULT_SORT_ORDER
        )

        // Iterate over the cursor and adding results to an array
        while (cursor?.moveToNext() == true){
            val messageBody = cursor.getString(cursor.getColumnIndexOrThrow("body"))
            message.add(messageBody.toString())
        }

        //Create an ArrayAdapter to display the message in the list view
        val messageArrayAdapter = ArrayAdapter<String>(
            this@MainActivity,
            android.R.layout.simple_list_item_1,
            message
            )

        //CLose the cursor
        cursor?.close()
        //Initialise the contentBox and displaying the messages using the adopter
        val contentBox = findViewById<ListView>(R.id.contentBox)
        contentBox.adapter = messageArrayAdapter
    }

    //Sms sending 
    protected fun sendSms(accountNumber: String) {

        //Initialising the SmsManager to access the sendTextMessage method
        val manager = SmsManager.getDefault()

        //Send message if no exception is raised
        try {
            // Send the message and display an alert to inform that the message is sent
            manager.sendTextMessage(
                "97771",
                null,
                accountNumber,
                null,
                null
            )
            Toast.makeText(
                this@MainActivity,
                "Message sent",
                Toast.LENGTH_SHORT
            ).show()
        }catch (e:Exception){
            
            //Investigate on exception type???????

            //Display exception message in a toast
            Toast.makeText(
                this@MainActivity,
                "$e :Please fill the account number",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    //Sends multiple sms by iteration over an array containing the message body
    protected fun sendMultipleSms(){

        //declare the array of accountNumbers <String>
        val accountNumbers = arrayOf<String>(
            "44573293",
            "44573319",
            "44573327",
            "44573343",
            "44573368"
        )

        //Iterate over the array and with each iteration call the sendSms function
        //Use either for or forEach to iterate over array
        //
        for(accountNumber in accountNumbers){
            //call the sendSms function with each iteration
            sendSms(accountNumber)
        }
    }

    //Delete historical records from the inbox ???????
    protected fun clearInbox(){
        contentResolver.delete(Telephony.Sms.Inbox.CONTENT_URI,null,null)
    }

    //Use the ktor library to get data from the server using the given url
    private suspend fun getServerContent(url :String): String{
        //
        //Create an instance of the client
        val client = HttpClient(CIO)

        //Use the client to get a http response
        val result : HttpResponse = client.get(url)

        //Access the body of the http response
        val txt: String =result.bodyAsText()

        //Close the client
        client.close()
        //
        //Return the body of the response as text
        return txt
//        println(responseBody)

    }

    // Post large amounts of data to a specified url
    protected suspend fun postToServer(url: String, messageBody: String){

        // Create an instance of the client
        val client = HttpClient(CIO)

        // Use the instance to post to the server
        val response: HttpResponse = client.submitForm (
            //
            //The url to post to
            url ="http://206.189.207.206/test123.php",
            //
            //The data to post
            formParameters = Parameters.build {
                append(
                    "username",
                    "james"
                )
            }
        )
        //
        //Console lo the respomse body as text
        println(response.bodyAsText())
        //
        // Confirmation toast
        Toast.makeText(this, "Post complete", Toast.LENGTH_SHORT).show()
        //
        // Terminate the client and release holdup resources
        client.close()

    }

    // Check the result of the requestPermission operation
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,//????
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        //
        if (grantResults.isNotEmpty() && (grantResults[0] == PackageManager.PERMISSION_GRANTED)){
            //
            //Do nothing if the permission is granted

            //Store the results in a global variable(grantResults)?????

        }else{
            //If permission is not granted show a toast and close the application
            Toast.makeText(
                this@MainActivity,
                "Permission denied",
                Toast.LENGTH_SHORT // Duration of toast
            ).show()

            //Terminate the current activity
            this.finish()
            //close the application
            exitProcess(0)
        }

    }
}