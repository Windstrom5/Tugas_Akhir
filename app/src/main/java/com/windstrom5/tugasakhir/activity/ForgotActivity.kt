package com.windstrom5.tugasakhir.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import com.chaos.view.PinView
import com.google.android.material.textfield.TextInputLayout
import com.windstrom5.tugasakhir.R
import com.windstrom5.tugasakhir.connection.ApiResponse
import com.windstrom5.tugasakhir.connection.ApiService
import com.windstrom5.tugasakhir.databinding.ActivityForgotBinding
import com.windstrom5.tugasakhir.feature.EmailSender
import com.windstrom5.tugasakhir.model.Admin
import com.windstrom5.tugasakhir.model.Pekerja
import com.windstrom5.tugasakhir.model.response
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import www.sanju.motiontoast.MotionToast
import www.sanju.motiontoast.MotionToastStyle
import java.util.concurrent.TimeUnit

class ForgotActivity : AppCompatActivity() {
    private lateinit var binding : ActivityForgotBinding
    private lateinit var linear1 : LinearLayout
    private lateinit var linear2 : LinearLayout
    private lateinit var linear3 : LinearLayout
    private lateinit var email : TextInputLayout
    private lateinit var pin : PinView
    private lateinit var password : TextInputLayout
    private lateinit var retype : TextInputLayout
    private lateinit var sendCode : Button
    private lateinit var save: Button
    private var timer: CountDownTimer? = null
    private var admin: Admin?= null
    private var pekerja: Pekerja?= null
    private var code : Int? = null
    private lateinit var loading : LinearLayout
    private val passwordTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            validatePassword()
        }
    }
    private val retypeTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            validatePassword()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotBinding.inflate(layoutInflater)
        setContentView(binding.root)
        linear1 = binding.li1
        loading = findViewById(R.id.layout_loading)
        linear2 = binding.li2
        linear3 = binding.li3
        email = binding.textInputEmail
        pin = binding.firstPinView
        password = binding.textInputPassword
        retype = binding.textInputPassword2
        sendCode = binding.cirSendButton
        save = binding.cirSaveButton
        email.editText?.addTextChangedListener(emailTextWatcher)
        sendCode.setOnClickListener {
            code = null
            val url = "http://192.168.1.6:8000/api/"
            val retrofit = Retrofit.Builder()
                .baseUrl(url)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(ApiService::class.java)
            val call = apiService.checkEmail(email.editText?.text.toString())
            call.enqueue(object : Callback<Map<String, Any>> {
                override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse == null) {
                            Log.e("ErrorMassage", email.editText?.text.toString())
                            runOnUiThread {
                                MotionToast.createToast(
                                    this@ForgotActivity, "Error",
                                    "Email Tidak Ada",
                                    MotionToastStyle.ERROR,
                                    MotionToast.GRAVITY_BOTTOM,
                                    MotionToast.LONG_DURATION,
                                    ResourcesCompat.getFont(
                                        this@ForgotActivity,
                                        R.font.ralewaybold
                                    )
                                )
                            }
                        } else {
                            startTimer()
                            sendCode.isEnabled = false
                            code = generateRandomCode()
                            EmailSender.sendEmail(
                                email.editText?.text.toString(),
                                "Reset Password - Workhubs",
                                "Hello,\n\nYou have requested to reset your password for Workhubs account. " +
                                        "Your verification code is: $code\n\nIf you did not request this, please ignore this email.\n\n" +
                                        "Regards,\nWorkhubs Team"
                            )
                            runOnUiThread {
                                MotionToast.createToast(
                                    this@ForgotActivity, "Successfully Send Email",
                                    "Check Email at Inbox or In Spam Folder",
                                    MotionToastStyle.SUCCESS,
                                    MotionToast.GRAVITY_BOTTOM,
                                    MotionToast.LONG_DURATION,
                                    ResourcesCompat.getFont(
                                        this@ForgotActivity,
                                        R.font.ralewaybold
                                    )
                                )
                                linear2.visibility = View.VISIBLE
                            }
                        }
                    } else {
                        Log.e("ErrorMassage", "WHYYY")
                    }
                    setLoading(false)
                }

                override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                    Log.e("ErrorMassage", "Retrofit failure: ${t.message}", t)
                    // Handle failure
                    setLoading(false)
                }
            })
        }
        pin.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val enteredCode = s.toString()
                if (enteredCode.length == 4) {
                    if (enteredCode == code.toString()) {
                        // Code matches, show li3 and hide li2 and li1
                        linear2.visibility = View.GONE
                        linear1.visibility = View.GONE
                        linear3.visibility = View.VISIBLE
                    } else {
                        // Code does not match, reset PinView and show an error message
                        pin.setText("")
                        pin.error = "Invalid code. Please enter the correct code."
                    }
                }
            }
        })
        password.editText?.addTextChangedListener(passwordTextWatcher)
        retype.editText?.addTextChangedListener(retypeTextWatcher)
        save.setOnClickListener{
            val retrofit = Retrofit.Builder()
                .baseUrl("http://192.168.1.6:8000/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(ApiService::class.java)

            val call = apiService.resetPassword(email.editText?.text.toString())
            call.enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        // Handle successful response
                        val apiResponse = response.body()
                        // Process the response data
                    } else {
                        // Handle unsuccessful response
                        // You can extract error information from the response if needed
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // Handle failure
                    // This method is called if the request fails to execute due to network failure, timeout, etc.
                }
            })
        }
    }
    private fun validatePassword() {
        val passwordText = password.editText?.text.toString()
        val retypeText = retype.editText?.text.toString()

        // Enable or disable the save button based on password and retype fields
        save.isEnabled = passwordText.isNotEmpty() && retypeText.isNotEmpty() && passwordText == retypeText
    }




    private fun setLoading(isLoading:Boolean){
        if(isLoading){
            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
            )
            loading!!.visibility = View.VISIBLE
        }else{
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
            loading!!.visibility = View.INVISIBLE
        }
    }

    private fun generateRandomCode(): Int {
        return (1000..9999).random()
    }

    private val emailTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(s: Editable?) {
            // Check if the email is empty or does not match the email pattern
            val isValidEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(s.toString()).matches()
            val isNotEmpty = s.toString().isNotEmpty()
            // Enable or disable the button based on the email validity
            sendCode.isEnabled = isValidEmail && isNotEmpty
        }
    }


    private fun startTimer() {
        // Initialize the timer for 1 minute
        timer = object : CountDownTimer(TimeUnit.MINUTES.toMillis(1), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Calculate the remaining time in seconds
                val secondsRemaining = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished)

                // Update the text of the button to show the remaining time
                sendCode.text = "Available again in $secondsRemaining seconds"
            }

            override fun onFinish() {
                // Timer finished, enable the button
                sendCode.isEnabled = true

                // Reset the text of the button
                sendCode.text = "Send Code"
            }
        }
        // Start the timer
        timer?.start()
    }
}