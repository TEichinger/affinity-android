package de.tub.affinity3.android.fragments

import androidx.fragment.app.Fragment
import io.reactivex.disposables.CompositeDisposable

open class BaseRxFragment : Fragment() {
    val disposeBag = CompositeDisposable()

    override fun onDestroyView() {
        super.onDestroyView()
        disposeBag.clear()
    }
}
