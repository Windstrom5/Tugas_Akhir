package com.windstrom5.tugasakhir.feature

import android.content.Context
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog.OnDateSetListener
import com.windstrom5.tugasakhir.R
import org.json.JSONObject
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class CustomDatePickerDialog(
    private val context: Context,
    private val onDateSetListener: OnDateSetListener,
    year: Int,
    monthOfYear: Int,
    dayOfMonth: Int
) : OnDateSetListener {

    private val holidays = loadHolidaysFromJson(context)

    init {
        // Show the DatePickerDialog
        val dpd = DatePickerDialog.newInstance(
            this,
            year,
            monthOfYear,
            dayOfMonth
        )

        // Set the disabled days
        dpd.setDisabledDays(holidays.keys.toTypedArray())

        // Show the dialog
        dpd.show((context as AppCompatActivity).supportFragmentManager, "DatePickerDialog")
    }

    private fun loadHolidaysFromJson(context: Context): Map<Calendar, String> {
        val holidaysMap = mutableMapOf<Calendar, String>()

        try {
            val inputStream: InputStream = context.resources.openRawResource(R.raw.holidays)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)

            // Iterate through the keys (dates) in the JSON object
            for (key in jsonObject.keys()) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = Calendar.getInstance().apply {
                    time = dateFormat.parse(key) ?: Date()
                }
                val dateObject = jsonObject.getJSONObject(key)
                val summary = dateObject.getString("summary")

                // Add the date and summary to the map
                holidaysMap[date] = summary
                Log.d("A", "Holiday: Date = $key, Summary = $summary")
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return holidaysMap
    }

    override fun onDateSet(view: DatePickerDialog?, year: Int, monthOfYear: Int, dayOfMonth: Int) {
        // Check if the selected date is a holiday
        val selectedDate = Calendar.getInstance().apply {
            set(year, monthOfYear, dayOfMonth)
        }

        if (isIndonesianHoliday(selectedDate)) {
            // Show Toast
            Toast.makeText(context, "Selected date is a holiday. Please choose another date.", Toast.LENGTH_LONG).show()

            // You might want to handle this differently, e.g., close the dialog or set to a valid date
        } else {
            // Handle the valid date
            onDateSetListener.onDateSet(view, year, monthOfYear, dayOfMonth)
        }
    }

    private fun isIndonesianHoliday(date: Calendar): Boolean {
        return holidays.containsKey(date)
    }
}