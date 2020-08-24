package com.shameel.swipeableintrolibrary

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Build
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.*
import com.shameel.swipeableintrolibrary.listeners.*
import com.shameel.swipeableintrolibrary.utils.SwipeableIntroEngineDefaults
import com.shameel.swipeableintrolibrary.utils.SwipeableIntroEngineDefaults.Companion.ANIM_BACKGROUND_TIME
import com.shameel.swipeableintrolibrary.utils.SwipeableIntroEngineDefaults.Companion.ANIM_CONTENT_CENTERING_TIME
import com.shameel.swipeableintrolibrary.utils.SwipeableIntroEngineDefaults.Companion.ANIM_CONTENT_ICON_HIDE_TIME
import com.shameel.swipeableintrolibrary.utils.SwipeableIntroEngineDefaults.Companion.ANIM_CONTENT_ICON_SHOW_TIME
import com.shameel.swipeableintrolibrary.utils.SwipeableIntroEngineDefaults.Companion.ANIM_CONTENT_TEXT_HIDE_TIME
import com.shameel.swipeableintrolibrary.utils.SwipeableIntroEngineDefaults.Companion.ANIM_CONTENT_TEXT_SHOW_TIME
import com.shameel.swipeableintrolibrary.utils.SwipeableIntroEngineDefaults.Companion.ANIM_PAGER_BAR_MOVE_TIME
import com.shameel.swipeableintrolibrary.utils.SwipeableIntroEngineDefaults.Companion.ANIM_PAGER_ICON_TIME
import com.shameel.swipeableintrolibrary.utils.SwipeableIntroEngineDefaults.Companion.CONTENT_ICON_POS_DELTA_Y_DP
import com.shameel.swipeableintrolibrary.utils.SwipeableIntroEngineDefaults.Companion.CONTENT_TEXT_POS_DELTA_Y_DP
import com.shameel.swipeableintrolibrary.utils.SwipeableIntroEngineDefaults.Companion.PAGER_ICON_SHAPE_ALPHA

open class PaperOnboardingEngine/**
 * Main constructor for create a Paper Onboarding Engine
 *
 * @param rootLayout root paper onboarding layout element
 * @param contentElements ordered list of prepared content elements for onboarding
 * @param appContext application context
 */
    (rootLayout:View, contentElements:ArrayList<SwipeableIntroPage>, appContext:Context):SwipeableIntroEngineDefaults {
    // scale factor for converting dp to px
    private var dpToPixelsScaleFactor:Float = 0.toFloat()
    // main layout parts
    private val mRootLayout:RelativeLayout
    private val mContentTextContainer:FrameLayout
    private val mContentIconContainer:FrameLayout
    private val mBackgroundContainer:FrameLayout
    private val mPagerIconsContainer:LinearLayout
    private val mContentRootLayout:RelativeLayout
    private val mContentCenteredContainer:LinearLayout
    // application context
    private val mAppContext: Context
    // state variables
    private val mElements:ArrayList<SwipeableIntroPage> = arrayListOf()
    /**
     * @return index of currently active element
     */
    var activeElementIndex = 0
    // params for Pager position calculations, virtually final, but initializes in onGlobalLayoutListener
    private var mPagerElementActiveSize:Int = 0
    private var mPagerElementNormalSize:Int = 0
    private var mPagerElementLeftMargin:Int = 0
    private var mPagerElementRightMargin:Int = 0
    // Listeners
    private lateinit var mOnChangeListener:SwipeableIntroOnChangedListener
    private lateinit var mOnRightOutListener:SwipeableIntroOnRightOutListener
    private lateinit var mOnLeftOutListener:SwipeableIntroOnLeftOutListener
    /**
     * Returns content for currently active element
     *
     * @return content for currently active element
     */
    private val activeElement: SwipeableIntroPage?
        get() {
            return if (mElements.size > activeElementIndex) mElements.get(activeElementIndex) else null
        }
    init{
        if (contentElements == null || contentElements.isEmpty())
            throw IllegalArgumentException("No content elements provided")
        this.mElements.addAll(contentElements)
        this.mAppContext = appContext.getApplicationContext()
        mRootLayout = rootLayout as RelativeLayout
        mContentTextContainer = rootLayout.findViewById(R.id.onboardingContentTextContainer) as FrameLayout
        mContentIconContainer = rootLayout.findViewById(R.id.onboardingContentIconContainer) as FrameLayout
        mBackgroundContainer = rootLayout.findViewById(R.id.onboardingBackgroundContainer) as FrameLayout
        mPagerIconsContainer = rootLayout.findViewById(R.id.onboardingPagerIconsContainer) as LinearLayout
        mContentRootLayout = mRootLayout.getChildAt(1) as RelativeLayout
        mContentCenteredContainer = mContentRootLayout.getChildAt(0) as LinearLayout
        this.dpToPixelsScaleFactor = this.mAppContext.getResources().getDisplayMetrics().density
        initializeStartingState()


        mRootLayout.setOnTouchListener(object:OnSwipeListener(mAppContext) {
            override fun onSwipeLeft() {
                toggleContent(false)
            }
            override fun onSwipeRight() {
                toggleContent(true)
            }
        })
        mRootLayout.viewTreeObserver.addOnGlobalLayoutListener(object:ViewTreeObserver.OnGlobalLayoutListener() {
            override fun onGlobalLayout() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                {
                    mRootLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this)
                }
                else
                {
                    mRootLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this)
                }
                mPagerElementActiveSize = mPagerIconsContainer.getHeight()
                mPagerElementNormalSize = Math.min(mPagerIconsContainer.getChildAt(0).getHeight(),
                    mPagerIconsContainer.getChildAt(mPagerIconsContainer.getChildCount() - 1).getHeight())
                val layoutParams = mPagerIconsContainer.getChildAt(0).layoutParams as ViewGroup.MarginLayoutParams
                mPagerElementLeftMargin = layoutParams.leftMargin
                mPagerElementRightMargin = layoutParams.rightMargin
                mPagerIconsContainer.setX(calculateNewPagerPosition(0).toFloat())
                mContentCenteredContainer.setY(((mContentRootLayout.getHeight() - mContentCenteredContainer.getHeight()) / 2).toFloat())
            }
        })
    }
    /**
     * Calculate new position for pager without using pager's current position(like .getX())
     * this method allows to avoid incorrect position values while animation of pager in progress
     *
     * @param newActiveElement index of newly active element (from 0)
     * @return new X position for pager bar
     */
    protected fun calculateNewPagerPosition(newActiveElement: Int): Int {
        var a = newActiveElement
        a++
        if (a <= 0)
            a = 1
        val pagerActiveElemCenterPosX = (mPagerElementActiveSize / 2
                + a * mPagerElementLeftMargin
                + (a - 1) * (mPagerElementNormalSize + mPagerElementRightMargin))
        return (mRootLayout.getWidth() / 2 - pagerActiveElemCenterPosX)
    }
    /**
     * Calculate current center coordinates of pager element with provided index
     *
     * @param activeElementIndex index of element (from 0)
     * @return array with 2 coordinate values [x,y]
     */
    private fun calculateCurrentCenterCoordinatesOfPagerElement(activeElementIndex:Int):IntArray {
        val y = (mPagerIconsContainer.getY() + mPagerIconsContainer.getHeight() / 2) as Int
        if (activeElementIndex >= mPagerIconsContainer.getChildCount())
            return intArrayOf(mRootLayout.getWidth() / 2, y)
        val pagerElem = mPagerIconsContainer.getChildAt(activeElementIndex)
        val x = (mPagerIconsContainer.getX() + pagerElem.getX() + pagerElem.getWidth() / 2) as Int
        return intArrayOf(x, y)
    }
    /**
     * Initializes starting state
     */
    private fun initializeStartingState() {
        // Create bottom bar icons for all elements with big first icon
        for (i in 0 until mElements.size)
        {
            val PaperOnboardingPage = mElements.get(i)
            val bottomBarIconElement = createPagerIconElement(PaperOnboardingPage.bottomBarIconRes, i == 0)
            mPagerIconsContainer.addView(bottomBarIconElement)
        }
        // Initialize first element on screen
        val activeElement = activeElement
        // initial content texts
        val initialContentText = createContentTextView(activeElement)
        mContentTextContainer.addView(initialContentText)
        // initial content icons
        val initContentIcon = createContentIconView(activeElement)
        mContentIconContainer.addView(initContentIcon)
        // initial bg color
        if (activeElement != null) {
            mRootLayout.setBackgroundColor(activeElement.bgColor)
        }
    }
    /**
     * @param prev set true to animate onto previous content page (default is false - animating to next content page)
     */
    protected fun toggleContent(prev:Boolean) {
        val oldElementIndex = activeElementIndex
        val newElement = if (prev) toggleToPreviousElement() else toggleToNextElement()
        if (newElement == null)
        {
            if (prev && mOnLeftOutListener != null)
                mOnLeftOutListener.onLeftOut()
            if (!prev && mOnRightOutListener != null)
                mOnRightOutListener.onRightOut()
            return
        }
        val newPagerPosX = calculateNewPagerPosition(activeElementIndex)
        // 1 - animate BG
        val bgAnimation = createBGAnimatorSet(newElement.bgColor)
        // 2 - animate pager position
        val pagerMoveAnimation = ObjectAnimator.ofFloat(mPagerIconsContainer, "x", mPagerIconsContainer.x.toFloat(), newPagerPosX.toFloat())
        pagerMoveAnimation.setDuration(ANIM_PAGER_BAR_MOVE_TIME.toLong())
        // 3 - animate pager icons
        val pagerIconAnimation = createPagerIconAnimation(oldElementIndex, activeElementIndex)
        // 4 animate content text
        val newContentText = createContentTextView(newElement)
        mContentTextContainer.addView(newContentText)
        val contentTextShowAnimation = createContentTextShowAnimation(
            mContentTextContainer.getChildAt(mContentTextContainer.getChildCount() - 2), newContentText)
        // 5 animate content icon
        val newContentIcon = createContentIconView(newElement)
        mContentIconContainer.addView(newContentIcon)
        val contentIconShowAnimation = createContentIconShowAnimation(
            mContentIconContainer.getChildAt(mContentIconContainer.getChildCount() - 2), newContentIcon)
        // 6 animate centering of all content
        val centerContentAnimation = createContentCenteringVerticalAnimation(newContentText, newContentIcon)
        centerContentAnimation.start()
        bgAnimation.start()
        pagerMoveAnimation.start()
        pagerIconAnimation.start()
        contentIconShowAnimation.start()
        contentTextShowAnimation.start()
        if (mOnChangeListener != null)
            mOnChangeListener.onPageChanged(oldElementIndex, activeElementIndex)
    }
    fun setOnChangeListener(onChangeListener:SwipeableIntroOnChangedListener) {
        this.mOnChangeListener = onChangeListener
    }
    fun setOnRightOutListener(onRightOutListener: SwipeableIntroOnRightOutListener) {
        this.mOnRightOutListener = onRightOutListener
    }
    fun setOnLeftOutListener(onLeftOutListener: SwipeableIntroOnLeftOutListener) {
        this.mOnLeftOutListener = onLeftOutListener
    }
    /**
     * @param color new background color for new
     * @return animator set with background color circular reveal animation
     */
    protected fun createBGAnimatorSet(color:Int):AnimatorSet {
        val bgColorView = ImageView(mAppContext)
        bgColorView.layoutParams = RelativeLayout.LayoutParams(mRootLayout.getWidth(), mRootLayout.getHeight())
        bgColorView.setBackgroundColor(color)
        mBackgroundContainer.addView(bgColorView)
        val pos = calculateCurrentCenterCoordinatesOfPagerElement(activeElementIndex)
        val finalRadius = if (mRootLayout.getWidth() > mRootLayout.getHeight()) mRootLayout.getWidth() else mRootLayout.height
        val bgAnimSet = AnimatorSet()
        val fadeIn = ObjectAnimator.ofFloat(bgColorView, "alpha", 0f, 1f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            val circularReveal = ViewAnimationUtils.createCircularReveal(bgColorView, pos[0], pos[1],
                0F,
                finalRadius.toFloat()
            )
            circularReveal.setInterpolator(AccelerateInterpolator())
            bgAnimSet.playTogether(circularReveal, fadeIn)
        }
        else
        {
            bgAnimSet.playTogether(fadeIn)
        }
        bgAnimSet.setDuration(ANIM_BACKGROUND_TIME.toLong())
        bgAnimSet.addListener(object:AnimatorEndListener() {
            override fun onAnimationEnd(animation:Animator) {
                mRootLayout.setBackgroundColor(color)
                bgColorView.setVisibility(View.GONE)
                mBackgroundContainer.removeView(bgColorView)
            }
        })
        return bgAnimSet
    }
    /**
     * @param currentContentText currently displayed view with text
     * @param newContentText newly created and prepared view to display
     * @return animator set with this animation
     */
    private fun createContentTextShowAnimation(currentContentText:View, newContentText:View):AnimatorSet {
        val positionDeltaPx = dpToPixels(CONTENT_TEXT_POS_DELTA_Y_DP)
        val animations = AnimatorSet()
        val currentContentMoveUp = ObjectAnimator.ofFloat(currentContentText, "y", 0f, -positionDeltaPx.toFloat())
        currentContentMoveUp.setDuration(ANIM_CONTENT_TEXT_HIDE_TIME.toLong())
        currentContentMoveUp.addListener(object:AnimatorEndListener() {
            override fun onAnimationEnd(animation:Animator) {
                mContentTextContainer.removeView(currentContentText)
            }
        })
        val currentContentFadeOut = ObjectAnimator.ofFloat(currentContentText, "alpha", 1f, 0f)
        currentContentFadeOut.setDuration(ANIM_CONTENT_TEXT_HIDE_TIME.toLong())
        animations.playTogether(currentContentMoveUp, currentContentFadeOut)
        val newContentMoveUp = ObjectAnimator.ofFloat(newContentText, "y", positionDeltaPx.toFloat(), 0f)
        newContentMoveUp.setDuration(ANIM_CONTENT_TEXT_SHOW_TIME.toLong())
        val newContentFadeIn = ObjectAnimator.ofFloat(newContentText, "alpha", 0f, 1f)
        newContentFadeIn.setDuration(ANIM_CONTENT_TEXT_SHOW_TIME.toLong())
        animations.playTogether(newContentMoveUp, newContentFadeIn)
        animations.setInterpolator(DecelerateInterpolator())
        return animations
    }
    /**
     * @param currentContentIcon currently displayed view with icon
     * @param newContentIcon newly created and prepared view to display
     * @return animator set with this animation
     */
    protected fun createContentIconShowAnimation(currentContentIcon:View, newContentIcon:View):AnimatorSet {
        val positionDeltaPx = dpToPixels(CONTENT_ICON_POS_DELTA_Y_DP)
        val animations = AnimatorSet()
        val currentContentMoveUp = ObjectAnimator.ofFloat(currentContentIcon, "y", 0f, -positionDeltaPx.toFloat())
        currentContentMoveUp.setDuration(ANIM_CONTENT_ICON_HIDE_TIME.toLong())
        currentContentMoveUp.addListener(object: AnimatorEndListener() {
            override fun onAnimationEnd(animation:Animator) {
                mContentIconContainer.removeView(currentContentIcon)
            }
        })
        val currentContentFadeOut = ObjectAnimator.ofFloat(currentContentIcon, "alpha", 1f, 0f)
        currentContentFadeOut.setDuration(ANIM_CONTENT_ICON_HIDE_TIME.toLong())
        animations.playTogether(currentContentMoveUp, currentContentFadeOut)
        val newContentMoveUp = ObjectAnimator.ofFloat(newContentIcon, "y", positionDeltaPx.toFloat(), 0f)
        newContentMoveUp.setDuration(ANIM_CONTENT_ICON_SHOW_TIME.toLong())
        val newContentFadeIn = ObjectAnimator.ofFloat(newContentIcon, "alpha", 0f, 1f)
        newContentFadeIn.setDuration(ANIM_CONTENT_ICON_SHOW_TIME.toLong())
        animations.playTogether(newContentMoveUp, newContentFadeIn)
        animations.setInterpolator(DecelerateInterpolator())
        return animations
    }
    protected fun createContentCenteringVerticalAnimation(newContentText:View, newContentIcon:View): Animator {
        newContentText.measure(View.MeasureSpec.makeMeasureSpec(mContentCenteredContainer.getWidth(), View.MeasureSpec.AT_MOST), -2)
        val measuredContentTextHeight = newContentText.getMeasuredHeight()
        newContentIcon.measure(-2, -2)
        val measuredContentIconHeight = newContentIcon.getMeasuredHeight()
        val newHeightOfContent = measuredContentIconHeight + measuredContentTextHeight + (mContentTextContainer.getLayoutParams() as ViewGroup.MarginLayoutParams).topMargin
        val centerContentAnimation = ObjectAnimator.ofFloat(mContentCenteredContainer, "y", mContentCenteredContainer.y, (mContentRootLayout.height - newHeightOfContent).toFloat() / 2)
        centerContentAnimation.setDuration(ANIM_CONTENT_CENTERING_TIME.toLong())
        centerContentAnimation.setInterpolator(DecelerateInterpolator())
        return centerContentAnimation
    }
    /**
     * Create animator for pager icon
     *
     * @param oldIndex index currently active icon
     * @param newIndex index of new active icon
     * @return animator set with this animation
     */
    protected fun createPagerIconAnimation(oldIndex:Int, newIndex:Int):AnimatorSet {
        val animations = AnimatorSet()
        animations.setDuration(ANIM_PAGER_ICON_TIME.toLong())
        // scale down whole old element
        val oldActiveItem = mPagerIconsContainer.getChildAt(oldIndex) as ViewGroup
        val oldActiveItemParams = oldActiveItem.layoutParams as LinearLayout.LayoutParams
        val oldItemScaleDown = ValueAnimator.ofInt(mPagerElementActiveSize, mPagerElementNormalSize)
        oldItemScaleDown.addUpdateListener { valueAnimator ->
            oldActiveItemParams.height = valueAnimator.getAnimatedValue() as Int
            oldActiveItemParams.width = valueAnimator.getAnimatedValue() as Int
            oldActiveItem.requestLayout()
        }
        // fade out old new element icon
        val oldActiveIcon = oldActiveItem.getChildAt(1)
        val oldActiveIconFadeOut = ObjectAnimator.ofFloat(oldActiveIcon, "alpha", 1f, 0f)
        // fade in old element shape
        val oldActiveShape = oldActiveItem.getChildAt(0) as ImageView
        oldActiveShape.setImageResource(if (oldIndex - newIndex > 0) R.drawable.swipeable_intro_pager_circle_icon else R.drawable.swwipeable_intro_pager_round_icon)
        val oldActiveShapeFadeIn = ObjectAnimator.ofFloat(oldActiveShape, "alpha", 0f, PAGER_ICON_SHAPE_ALPHA)
        // add animations
        animations.playTogether(oldItemScaleDown, oldActiveIconFadeOut, oldActiveShapeFadeIn)
        // scale up whole new element
        val newActiveItem = mPagerIconsContainer.getChildAt(newIndex) as ViewGroup
        val newActiveItemParams = newActiveItem.getLayoutParams() as LinearLayout.LayoutParams
        val newItemScaleUp = ValueAnimator.ofInt(mPagerElementNormalSize, mPagerElementActiveSize)
        newItemScaleUp.addUpdateListener { valueAnimator ->
            newActiveItemParams.height = valueAnimator.getAnimatedValue() as Int
            newActiveItemParams.width = valueAnimator.getAnimatedValue() as Int
            newActiveItem.requestLayout()
        }
        // fade in new element icon
        val newActiveIcon = newActiveItem.getChildAt(1)
        val newActiveIconFadeIn = ObjectAnimator.ofFloat(newActiveIcon, "alpha", 0f, 1f)
        // fade out new element shape
        val newActiveShape = newActiveItem.getChildAt(0) as ImageView
        val newActiveShapeFadeOut = ObjectAnimator.ofFloat(newActiveShape, "alpha", PAGER_ICON_SHAPE_ALPHA, 0f)
        // add animations
        animations.playTogether(newItemScaleUp, newActiveShapeFadeOut, newActiveIconFadeIn)
        animations.setInterpolator(DecelerateInterpolator())
        return animations
    }
    /**
     * @param iconDrawableRes drawable resource for icon
     * @param isActive is active element
     * @return configured pager icon with selected drawable and selected state (active or inactive)
     */
    protected fun createPagerIconElement(iconDrawableRes:Int, isActive:Boolean):ViewGroup {
        val vi = LayoutInflater.from(mAppContext)
        val bottomBarElement = vi.inflate(R.layout.swipeable_intro_pager_layout, mPagerIconsContainer, false) as FrameLayout
        val elementShape = bottomBarElement.getChildAt(0) as ImageView
        val elementIcon = bottomBarElement.getChildAt(1) as ImageView
        elementIcon.setImageResource(iconDrawableRes)
        if (isActive)
        {
            val layoutParams = bottomBarElement.getLayoutParams() as LinearLayout.LayoutParams
            layoutParams.width = mPagerIconsContainer.getLayoutParams().height
            layoutParams.height = mPagerIconsContainer.getLayoutParams().height
            elementShape.setAlpha(0f)
            elementIcon.setAlpha(1f)
        }
        else
        {
            elementShape.setAlpha(PAGER_ICON_SHAPE_ALPHA)
            elementIcon.setAlpha(0f)
        }
        return bottomBarElement
    }
    /**
     * @param PaperOnboardingPage new content page to show
     * @return configured view with new content texts
     */
    protected fun createContentTextView(PaperOnboardingPage: SwipeableIntroPage?):ViewGroup {
        val vi = LayoutInflater.from(mAppContext)
        val contentTextView = vi.inflate(R.layout.swipeable_intro_text_content_layout, mContentTextContainer, false) as ViewGroup
        val contentTitle = contentTextView.getChildAt(0) as TextView
        contentTitle.setText(SwipeableIntroPage.titleText)
        val contentText = contentTextView.getChildAt(1) as TextView
        contentText.setText(SwipeableIntroPage.descriptionText)
        return contentTextView
    }
    /**
     * @param PaperOnboardingPage new content page to show
     * @return configured view with new content image
     */
    protected fun createContentIconView(PaperOnboardingPage: SwipeableIntroPage?):ImageView {
        val contentIcon = ImageView(mAppContext)
        contentIcon.setImageResource(SwipeableIntroPage.contentIconRes)
        val iconLP = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        iconLP.gravity = Gravity.CENTER
        contentIcon.setLayoutParams(iconLP)
        return contentIcon
    }
    /**
     * Changes active element to the previous one and returns a new content
     *
     * @return content for previous element
     */
    protected fun toggleToPreviousElement(): SwipeableIntroPage? {
        if (activeElementIndex - 1 >= 0)
        {
            activeElementIndex--
            return if (mElements.size > activeElementIndex) mElements.get(activeElementIndex) else null
        }
        else
            return null
    }
    /**
     * Changes active element to the next one and returns a new content
     *
     * @return content for next element
     */
    protected fun toggleToNextElement(): SwipeableIntroPage? {
        if (activeElementIndex + 1 < mElements.size)
        {
            activeElementIndex++
            return if (mElements.size > activeElementIndex) mElements.get(activeElementIndex) else null
        }
        else
            return null
    }
    /**
     * Converts DP values to PX
     *
     * @param dpValue value to convert in dp
     * @return converted value in px
     */
    protected fun dpToPixels(dpValue:Int):Int {
        return (dpValue * dpToPixelsScaleFactor + 0.5f).toInt()
    }
}