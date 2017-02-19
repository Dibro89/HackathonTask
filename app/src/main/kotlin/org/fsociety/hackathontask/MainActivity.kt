@file:Suppress("DEPRECATION")

package org.fsociety.hackathontask

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.ViewManager
import android.widget.DatePicker
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.httpGet
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet
import org.jetbrains.anko.*
import org.jetbrains.anko.custom.ankoView
import java.text.SimpleDateFormat
import java.util.*

const val privatbankAPI = "https://api.privatbank.ua/p24api/exchange_rates"
const val dayInMillis: Long = 1000 * 60 * 60 * 24

class MainActivity : AppCompatActivity() {
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    private val calendar = Calendar.getInstance()

    private lateinit var pckFrom: DatePicker
    private lateinit var pckTo: DatePicker
    private lateinit var chart: BarChart

    private var from: Long = -1
    private var to: Long = -1
    private var days: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verticalLayout {
            gravity = Gravity.CENTER

            linearLayout {
                gravity = Gravity.CENTER

                verticalLayout {
                    lparams { margin = 10 }
                    textView("From:") { gravity = Gravity.CENTER; textSize = 20f }
                    pckFrom = datePicker { calendarViewShown = false }
                }

                verticalLayout {
                    lparams { margin = 10 }
                    textView("To:") { gravity = Gravity.CENTER; textSize = 20f }
                    pckTo = datePicker { calendarViewShown = false }
                }
            }

            button("Show") {
                lparams { margin = dip(10); width = dip(150) }
                onClick {
                    chart.data = null

                    from = pckFrom.millis
                    to = pckTo.millis

                    if (to < from) to = from

                    days = (to - from) / dayInMillis + 1

                    doAsync {
                        val pb = mutableListOf<BarEntry>()
                        val nb = mutableListOf<BarEntry>()

                        for (i in 0 until days) {
                            val date = Date(from + i * dayInMillis)

                            val (request, response, result) = privatbankAPI
                                    .httpGet(listOf("json" to "", "date" to dateFormat.format(date)))
                                    .responseJson()

                            val arr = result.get().obj().getJSONArray("exchangeRate")
                            for (j in 0 until arr.length()) {
                                val obj = arr.getJSONObject(j)
                                if (obj.getString("currency") == "USD") {
                                    pb.add(BarEntry(i.toFloat(), obj.getDouble("saleRate").toFloat()))
                                    nb.add(BarEntry(i.toFloat(), obj.getDouble("saleRateNB").toFloat()))
                                }
                            }
                        }

                        uiThread {
                            chart.apply {
                                data = BarData(mutableListOf<IBarDataSet>(
                                        BarDataSet(pb, "PrivatBank")
                                                .apply { color = Color.GREEN },
                                        BarDataSet(nb, "National Bank of Ukraine")
                                                .apply { color = Color.BLUE }
                                ))

                                val groupSpace = 0.16f
                                val barSpace = 0.06f
                                val barWidth = 0.36f

                                barData.barWidth = barWidth
                                xAxis.axisMaximum = days.toFloat()

                                groupBars(0f, groupSpace, barSpace)
                                invalidate()
                            }
                        }
                    }
                }
            }

            chart = barChart {
                lparams { margin = dip(30); width = matchParent; height = matchParent }

                description.isEnabled = false

                xAxis.apply {
                    axisMinimum = 0f
                    setCenterAxisLabels(true)
                    setValueFormatter { value, axisBase ->
                        dateFormat.format(Date(from + value.toLong() * dayInMillis))
                    }
                }

                axisLeft.apply {
                    axisMinimum = 25f
                    axisMaximum = 28f
                    setDrawGridLines(false)
                }

                axisRight.isEnabled = false
            }
        }
    }

    val DatePicker.millis: Long get() = calendar.apply { set(year, month, dayOfMonth) }.time.time
}

inline fun ViewManager.barChart(theme: Int = 0, init: BarChart.() -> Unit)
        = ankoView(::BarChart, theme, init)
