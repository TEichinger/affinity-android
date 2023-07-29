package de.tub.affinity3.android.view_models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.reactivex.disposables.CompositeDisposable

abstract class BaseRxViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * Abstract class from which view-model classes inherit. It adds reactive components
     * from the ReactiveX project to the <AndroidViewModel> class.
     */
    protected val disposeBag = CompositeDisposable()

    override fun onCleared() {
        super.onCleared()
        disposeBag.clear()
    }
}
