package com.example.currentweatherdatabinding

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.example.currentweatherdatabinding.databinding.ActivityMainBinding
import com.google.gson.Gson
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.roundToInt

// Поля соответствуют JSON OpenWeatherMap.
data class WeatherResponse(val name: String, val main: MainWeather,
    val weather: List<WeatherDescription>, val wind: Wind, val clouds: Clouds)
data class MainWeather(val temp: Double, val feels_like: Double, val humidity: Int)
data class WeatherDescription(val description: String)
data class Wind(val speed: Double, val deg: Int)
data class Clouds(val all: Int)

// Эти строки используются в выражениях @{weather...} в XML.
data class WeatherUi(val city: String, val temperature: String, val description: String,
    val feelsLike: String, val humidity: String, val wind: String, val clouds: String,
    val cloudIcon: Int, val windRotation: Float)

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var request: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.loading = false
        binding.hasWeather = false
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        binding.cityInput.setText(savedInstanceState?.getString("city") ?: "Иркутск")
        binding.refreshButton.setOnClickListener { onClick(it) }
        onClick(binding.refreshButton)
    }

    // Обработчик кнопки, как в исходном проекте.
    fun onClick(v: View) {
        request?.cancel()
        request = scope.launch { loadWeather() }
    }

    // Запрос в корутине, как в заготовке; интерфейс обновляется в Main.
    suspend fun loadWeather() {
        val city = binding.cityInput.text.toString().trim()
        if (city.isEmpty()) {
            binding.status = "Введите название города"
            return
        }
        binding.loading = true
        binding.status = "Загрузка…"
        binding.hasWeather = false
        try {
            val result = withContext(Dispatchers.IO) {
                val API_KEY = getString(R.string.openweather_api_key)
                val query = URLEncoder.encode(city, "UTF-8")
                val weatherURL = "https://api.openweathermap.org/data/2.5/weather?q=$query&appid=$API_KEY&units=metric&lang=ru"
                val connection = URL(weatherURL).openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                try {
                    when (connection.responseCode) {
                        200 -> connection.inputStream.bufferedReader().use {
                            Gson().fromJson(it, WeatherResponse::class.java)
                        }
                        404 -> error("Город не найден. Проверьте название")
                        401 -> error("Нужен действующий ключ OpenWeatherMap в strings.xml")
                        else -> error("Сервис погоды недоступен. Попробуйте позже")
                    }
                } catch (e: java.io.IOException) {
                    error("Нет связи с сервисом. Проверьте интернет и повторите")
                } finally {
                    connection.disconnect()
                }
            }
            val directions = listOf("С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ")
            binding.weather = WeatherUi(
                result.name, "${result.main.temp.roundToInt()} °C",
                result.weather.firstOrNull()?.description ?: "",
                "Ощущается как ${result.main.feels_like.roundToInt()} °C",
                "Влажность: ${result.main.humidity}%",
                "Ветер: ${result.wind.speed} м/с, ${directions[((result.wind.deg + 22.5) / 45).toInt() % 8]}",
                "Облачность: ${result.clouds.all}%",
                if (result.clouds.all < 30) R.drawable.ic_sun else R.drawable.ic_cloud,
                result.wind.deg.toFloat()
            )
            binding.hasWeather = true
            binding.status = "Данные OpenWeatherMap"
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            binding.status = e.message ?: "Не удалось получить погоду"
        } finally {
            binding.loading = false
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("city", binding.cityInput.text.toString())
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
