package dika.hackathontask

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.widget.DatePicker
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.httpGet
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.LegendRenderer
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import org.jetbrains.anko.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MainActivityUI().setContentView(this)
    }
}

@Suppress("DEPRECATION")
class MainActivityUI : AnkoComponent<MainActivity> {
    override fun createView(ui: AnkoContext<MainActivity>) =
            with(ui) {
                verticalLayout {
                    gravity = Gravity.CENTER

                    val graph = GraphView(ui.ctx)

                    var fromPicker: DatePicker? = null
                    var toPicker: DatePicker? = null

                    linearLayout {
                        gravity = Gravity.CENTER

                        verticalLayout {
                            textView("From:") { gravity = Gravity.CENTER; textSize = 20f }
                            fromPicker = datePicker { calendarViewShown = false }
                        }.lparams { margin = 10 }

                        verticalLayout {
                            textView("To:") { gravity = Gravity.CENTER; textSize = 20f }
                            toPicker = datePicker { calendarViewShown = false }
                        }.lparams { margin = 10 }
                    }

                    button("Show") {
                        onClick {
                            graph.removeAllSeries()

                            val from = Calendar.getInstance().apply {
                                fromPicker!!.let { set(it.year, it.month, it.dayOfMonth) }
                            }.time.time

                            val to = Calendar.getInstance().apply {
                                toPicker!!.let { set(it.year, it.month, it.dayOfMonth) }
                            }.time.time

                            val step: Long

                            val delta = from - to
                            if (delta > 1000 * 60 * 60 * 24 * cols) {
                                step = delta / cols
                            } else step = 1000 * 60 * 60 * 24

                            val s = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

                            val pb = arrayOfNulls<DataPoint>(cols)
                            val nb = arrayOfNulls<DataPoint>(cols)

                            ui.doAsync {
                                for (i in 0..cols - 1) {
                                    val d = Date(from + step * i)

                                    val (request, response, result) = p24.httpGet(
                                            listOf(
                                                    "json" to "",
                                                    "date" to s.format(d)
                                            )
                                    ).responseJson()

                                    println(result.get().content)

                                    val arr = result.get().obj().getJSONArray("exchangeRate")
                                    for (j in 0..arr.length() - 1) {
                                        val obj = arr.getJSONObject(j)
                                        if (obj.getString("currency") == "USD") {
                                            pb[i] = DataPoint(d, obj.getDouble("saleRate"))
                                            nb[i] = DataPoint(d, obj.getDouble("saleRateNB"))
                                        }
                                    }
                                }

                                activityUiThread {
                                    graph.addSeries(LineGraphSeries(pb).apply {
                                        title = "PrivatBank"
                                        color = Color.GREEN
                                    })

                                    graph.addSeries(LineGraphSeries(nb).apply {
                                        title = "National Bank of Ukraine"
                                        color = Color.BLUE
                                    })

                                    graph.gridLabelRenderer.apply {
                                        labelFormatter = DateAsXAxisLabelFormatter(graph.context)
                                        numVerticalLabels = 5
                                        numHorizontalLabels = cols
//                                        isHumanRounding = false
                                    }

                                    graph.viewport.apply {
                                        setMinX(pb.first()!!.x)
                                        setMaxX(pb.last()!!.x)
                                        isXAxisBoundsManual = true
                                    }

                                    graph.legendRenderer.apply {
                                        isVisible = true
                                        align = LegendRenderer.LegendAlign.TOP
                                        backgroundColor = Color.GRAY
                                    }
                                }
                            }
                        }
                    }.lparams { topMargin = -20; width = 250 }

                    addView(graph)
                }
            }
}

const val p24 = "https://api.privatbank.ua/p24api/exchange_rates"
const val cols = 3

/*
    public class MainActivity extends AppCompatActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }
    }
*/
