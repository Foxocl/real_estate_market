package by.kotlin.salesAppartment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NominatimViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NominatimRepository(
        PropertyDatabase.getInstance(application).geocodeDao(),
        RetrofitInstance.nominatimApi,
        application.applicationContext
    )

    suspend fun fetchCoordinates(address: String): Pair<Double, Double>? {
        // Задержка для соблюдения ограничения Nominatim (1 запрос/сек)
        kotlinx.coroutines.delay(1000)
        return repository.getCoordinates(address)
    }
}