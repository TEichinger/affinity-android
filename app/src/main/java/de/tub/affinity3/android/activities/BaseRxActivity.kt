package de.tub.affinity3.android.activities

import androidx.appcompat.app.AppCompatActivity
import io.reactivex.disposables.CompositeDisposable

abstract class BaseRxActivity : AppCompatActivity() {
    val disposeBag = CompositeDisposable()

    override fun onDestroy() {
        super.onDestroy()
        disposeBag.clear()
    }
}
