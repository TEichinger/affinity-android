package de.tub.affinity3.android.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar
import com.mindorks.placeholderview.SwipeDecor
import com.mindorks.placeholderview.Utils
import de.tub.affinity3.android.R
import de.tub.affinity3.android.views.RecommendationCard
import de.tub.affinity3.android.view_models.RecommendationsViewModel
import de.tub.affinity3.android.util.getDisplaySize
import de.tub.affinity3.android.util.toast
import de.tub.affinity3.android.util.visible
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import java.util.concurrent.TimeUnit
import kotlinx.android.synthetic.main.fragment_recommendations.acceptButton
import kotlinx.android.synthetic.main.fragment_recommendations.buttonContainer
import kotlinx.android.synthetic.main.fragment_recommendations.emptyLayout
import kotlinx.android.synthetic.main.fragment_recommendations.frameLayout
import kotlinx.android.synthetic.main.fragment_recommendations.rejectButton
import kotlinx.android.synthetic.main.fragment_recommendations.swipeView
import kotlinx.android.synthetic.main.fragment_recommendations.undoButton
import timber.log.Timber

import de.tub.affinity3.android.services.DummyRecommendationService


class RecommendationsFragment : BaseRxFragment() {

    private lateinit var viewModel: RecommendationsViewModel
    private lateinit var refreshSnackbar: Snackbar
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_recommendations, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        activity?.setTitle(R.string.title_recommended)
        setupSwipeView()
        viewModel = ViewModelProvider(this).get(RecommendationsViewModel::class.java)
        bindViewModel()

        // The snackbar in the bottom that shows a "Refresh" button, whenever new recommendations are available
        refreshSnackbar = Snackbar.make(frameLayout,
                R.string.no_recommendations_available, Snackbar.LENGTH_INDEFINITE
        )
        refreshSnackbar.setAction(R.string.refresh) {
            startFetch()
        }
    }

    private fun startFetch() {
        Timber.d("Entering 'startFetch' to fetch recommendations.")
        viewModel.fetchAllMovieRecommendations()
                .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { recommendations ->
                    showEmptyView(recommendations.isEmpty())
                    Timber.d("After: $recommendations")
                    for (movieRecommendation in recommendations) {
                        swipeView.addView(RecommendationCard(requireContext(), movieRecommendation, viewModel))
                        viewModel.cardsCount.onNext(viewModel.cardsCount.value!! + 1)
                    }
                },
                onError = {
                    Timber.d(it)
                }
            ).addTo(disposeBag)
    }

    private fun bindViewModel() {
        startFetch()

        viewModel.toastSignal
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    requireActivity().toast(it, centered = true)
                }
                .addTo(disposeBag)

        viewModel.cardsCount
            .map { it == 0 }
            .subscribe { isEmpty ->
                showEmptyView(isEmpty)
            }.addTo(disposeBag)

        Observables.combineLatest(viewModel.cardsCount, viewModel.newRecommendationsCount()) { cardsCount, newRecommendationsCount ->
            // if there are no cards shown, yet there are some new recommendations
            (cardsCount == 0) && (newRecommendationsCount > 0) 
            Timber.d("Cards: $cardsCount, Recs: $newRecommendationsCount")
            (cardsCount == 0) && (newRecommendationsCount > 0)
        }.throttleWithTimeout(1000, TimeUnit.MILLISECONDS)
                .subscribe { newRecommendationsAvailable ->
                    // show the snackbar that shows a refresh Button to load new recommendations 
                    if (newRecommendationsAvailable) {
                        if (!refreshSnackbar.isShown) {
                            refreshSnackbar.show()
                        }
                    }
                }.addTo(disposeBag)

    }

    private fun showEmptyView(show: Boolean) {
        swipeView.visible = !show
        buttonContainer.visible = !show
        emptyLayout.visible = show
    }

    private fun setupSwipeView() {
        val windowSize = getDisplaySize(requireActivity().windowManager)
        val bottomMargin = Utils.dpToPx(220f)

        showEmptyView(true)

        swipeView.addItemRemoveListener { count ->
            viewModel.cardRemoved(count)
        }

        swipeView.builder
                .setDisplayViewCount(3)
                .setIsUndoEnabled(true)
                .setHeightSwipeDistFactor(6f)
                .setWidthSwipeDistFactor(4f)
                .setSwipeVerticalThreshold(Utils.dpToPx(50f))
                .setSwipeHorizontalThreshold(Utils.dpToPx(50f))
                .setSwipeDecor(
                        SwipeDecor()
                                .setViewWidth(windowSize.x)
                                .setViewHeight(windowSize.y - bottomMargin)
                                .setViewGravity(Gravity.TOP)
                                .setMarginTop(30)
                                .setPaddingTop(-20)
                                .setPaddingLeft(20)
                                .setRelativeScale(0.01f)
                                .setSwipeInMsgLayoutId(R.layout.recommendation_swipe_in_message)
                                .setSwipeOutMsgLayoutId(R.layout.recommendation_swipe_out_msg)
                )

        acceptButton.setOnClickListener {
            swipeView.doSwipe(true)
        }

        rejectButton.setOnClickListener {
            swipeView.doSwipe(false)
        }

        undoButton.setOnClickListener {
            swipeView.undoLastSwipe()
            viewModel.undoClicked()
        }
    }
}
